package com.bankstack.eval;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class PromptLibrary {
  private final Map<PromptKey, String> prompts = new EnumMap<>(PromptKey.class);

  public PromptLibrary() {
    prompts.put(PromptKey.POLICY, ResourceLoaderUtil.loadText("prompts/policy.system.txt"));
    prompts.put(PromptKey.REFUSAL, ResourceLoaderUtil.loadText("prompts/refusal.system.txt"));
  }

  public String system(PromptKey key) {
    return prompts.get(key);
  }
}
