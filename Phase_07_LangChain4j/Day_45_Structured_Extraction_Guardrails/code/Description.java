package com.genai.langchain4j.extraction;

import java.lang.annotation.*;

/**
 * Custom annotation documenting schema requirements and semantics for fields.
 * Matches dev.langchain4j.model.output.structured.Description.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD})
public @interface Description {
    String value();
}
