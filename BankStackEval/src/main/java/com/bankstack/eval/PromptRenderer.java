package com.bankstack.eval;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptRenderer {
  public String render(String template, Map<String, String> vars) {
    String out = template;
    for (var e : vars.entrySet()) {
      out = out.replace("{{" + e.getKey() + "}}", e.getValue());
    }
    return out;
  }
}