package com.bankstack.eval;

import java.util.*;

public class ScoreResult {
    public double scorePercent;
    public List<String> failureCodes = new ArrayList<>();
    public List<String> failureReasons = new ArrayList<>();

    // diagnostics
    public int sentenceCount;
    public int wordCount;
    public boolean hasCitationC1;
    public boolean exactRefusalMatch;
    public boolean trimRefusalMatch;
    public boolean hasTrailingWhitespace;
}