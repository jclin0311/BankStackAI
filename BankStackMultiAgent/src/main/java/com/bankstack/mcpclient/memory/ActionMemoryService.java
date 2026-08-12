package com.bankstack.mcpclient.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionMemoryService {

    private static final String SUCCESS = "SUCCESS";

    private final ActionMemoryRepository repository;
    private final ObjectMapper objectMapper;

    public ActionMemoryService(ActionMemoryRepository repository,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void rememberSuccessfulAction(String actorId,
                                         String toolName,
                                         String intentLabel,
                                         Map<String, Object> arguments) {
        try {
            ActionMemoryEntity entity = new ActionMemoryEntity();
            entity.setId(UUID.randomUUID());
            entity.setActorId(actorId);
            entity.setToolName(toolName);
            entity.setIntentLabel(intentLabel);
            entity.setArgumentsJson(objectMapper.writeValueAsString(arguments));
            entity.setOutcome(SUCCESS);
            entity.setCreatedAt(Instant.now());

            repository.save(entity);

        } catch (Exception ex) {
            // Memory should never break banking flow.
            // Later we can audit/log this.
        }
    }

    public Optional<Map<String, Object>> findLatestSuccessfulArguments(String actorId,
                                                                       String toolName) {
        try {
            List<ActionMemoryEntity> results =
                    repository.findTop10ByActorIdAndToolNameAndOutcomeOrderByCreatedAtDesc(
                            actorId,
                            toolName,
                            SUCCESS
                    );

            if (results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> arguments =
                    objectMapper.readValue(
                            results.get(0).getArgumentsJson(),
                            new TypeReference<Map<String, Object>>() {
                            }
                    );

            return Optional.of(arguments);

        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}