package com.genai.enterprise.security;

/**
 * Categorization of Generative AI security threats matching OWASP Top 10 for LLMs.
 */
public enum SecurityThreatType {
    DIRECT_PROMPT_INJECTION,
    INDIRECT_PROMPT_INJECTION,
    PII_EXPOSURE,
    SYSTEM_PROMPT_EXTRACTION,
    SECRET_KEY_LEAKAGE
}
