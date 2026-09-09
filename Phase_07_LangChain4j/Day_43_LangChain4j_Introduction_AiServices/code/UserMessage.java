package com.genai.langchain4j.aiservices;

import java.lang.annotation.*;

/**
 * Declares the user prompt template for an AI Service method or parameter.
 * Matches dev.langchain4j.service.UserMessage.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface UserMessage {
    String value() default "";
}
