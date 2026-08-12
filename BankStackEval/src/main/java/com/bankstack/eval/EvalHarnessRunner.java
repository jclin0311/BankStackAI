package com.bankstack.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EvalHarnessRunner implements CommandLineRunner {

    private final ChatClient chatClient;
    private final PromptLibrary promptLibrary;
    private final PromptRenderer renderer;
    private final RuleScorer scorer;
    private final ObjectMapper mapper = new ObjectMapper();

    public EvalHarnessRunner(ChatClient.Builder chatClientBuilder,
                             PromptLibrary promptLibrary,
                             PromptRenderer renderer,
                             RuleScorer scorer) {
        this.chatClient = chatClientBuilder.build();
        this.promptLibrary = promptLibrary;
        this.renderer = renderer;
        this.scorer = scorer;
    }

    @Override
    public void run(String... args) throws Exception {

        String suitePath = argOrDefault(args, "--suite", "eval/eval-suite-v1.json");
        int runs = intArgOrDefault(args, "--runs", 5);

        String suiteJson = ResourceLoaderUtil.loadText(suitePath);
        EvalSuite suite = mapper.readValue(suiteJson, EvalSuite.class);

        String policyChunks = ResourceLoaderUtil.loadText("data/policy_chunks.json");

        Path outDir = Path.of("reports");
        Files.createDirectories(outDir);

        List<Map<String, Object>> results = new ArrayList<>();

        for (EvalCase evalCase : suite.cases) {

            String systemTemplate = promptLibrary.system(evalCase.promptKey);

            Map<String, String> vars = new HashMap<>();
            vars.put("chunks", "");

            switch (evalCase.promptKey) {
                case POLICY  -> vars.put("chunks", policyChunks);
                case REFUSAL -> {
                    // fixed guardrail prompt, no extra data required
                }
            }

            String system = renderer.render(systemTemplate, vars);

            List<Map<String, Object>> perRun = new ArrayList<>();
            double sum = 0.0;
            int passCount = 0;
            boolean flaky = false;

            String lastOutput = null;
            Double lastPercent = null;

            for (int i = 1; i <= runs; i++) {

                String output = chatClient
                        .prompt()
                        .system(system)
                        .user(evalCase.question)
                        .call()
                        .content();

                ScoreResult score = scorer.score(evalCase, output);
                double percent = score.scorePercent;

                if (percent >= 100.0) {
                    passCount++;
                }

                sum += percent;

                if (lastOutput != null && !lastOutput.equals(output)) {
                    flaky = true;
                }

                if (lastPercent != null && Double.compare(lastPercent, percent) != 0) {
                    flaky = true;
                }

                lastOutput = output;
                lastPercent = percent;

                Map<String, Object> runRow = new LinkedHashMap<>();
                runRow.put("runIndex", i);
                runRow.put("output", output);
                runRow.put("percent", percent);
                runRow.put("failureCodes", score.failureCodes);
                runRow.put("failureReasons", score.failureReasons);
                runRow.put("exactRefusalMatch", score.exactRefusalMatch);
                runRow.put("trimRefusalMatch", score.trimRefusalMatch);

                perRun.add(runRow);
            }

            double avgPercent = runs <= 0 ? 100.0 : (sum / runs);

            System.out.printf(
                    "%s (%s) -> %.2f%% pass=%d/%d%s%n",
                    evalCase.id,
                    evalCase.promptKey,
                    avgPercent,
                    passCount,
                    runs,
                    flaky ? " ⚠️FLAKY" : ""
            );

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caseId", evalCase.id);
            row.put("category", evalCase.category);
            row.put("promptKey", evalCase.promptKey);
            row.put("question", evalCase.question);
            row.put("runs", runs);
            row.put("avgPercent", avgPercent);
            row.put("passCount", passCount);
            row.put("flaky", flaky);
            row.put("runResults", perRun);

            results.add(row);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("suiteName", suite.suiteName);
        report.put("suitePath", suitePath);
        report.put("runs", runs);
        report.put("results", results);

        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(outDir.resolve("report.json").toFile(), report);

        try (FileWriter fw = new FileWriter(outDir.resolve("report.csv").toFile())) {

            fw.write("caseId,category,promptKey,avgPercent,passCount,runs,flaky\n");

            for (Map<String, Object> result : results) {
                fw.write("%s,%s,%s,%.2f,%s,%s,%s\n".formatted(
                        result.get("caseId"),
                        result.get("category"),
                        result.get("promptKey"),
                        (Double) result.get("avgPercent"),
                        result.get("passCount"),
                        result.get("runs"),
                        result.get("flaky")
                ));
            }
        }

        System.out.println("✅ reports/report.json + reports/report.csv generated");
    }

    private static String argOrDefault(String[] args, String key, String defaultValue) {
        String prefix = key + "=";

        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }

        return defaultValue;
    }

    private static int intArgOrDefault(String[] args, String key, int defaultValue) {
        try {
            return Integer.parseInt(
                    argOrDefault(args, key, String.valueOf(defaultValue))
            );
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}