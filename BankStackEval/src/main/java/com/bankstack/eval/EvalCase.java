package com.bankstack.eval;
public class EvalCase {
  public String id;
  public String category;
  public PromptKey promptKey;
  public String question;
  public Expected expected;

  public static class Expected {
    public boolean mustCite;
    public boolean mustRefuse;
    public boolean mustSayNoPolicyFound;
  }
}