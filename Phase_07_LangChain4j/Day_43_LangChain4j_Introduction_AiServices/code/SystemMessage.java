package com.genai.langchain4j.aiservices;

import java.lang.annotation.*;

/**
 * Declares the system prompt instruction for an AI Service method or interface.
 * Matches dev.langchain4j.service.SystemMessage.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SystemMessage {
    String value();
}
