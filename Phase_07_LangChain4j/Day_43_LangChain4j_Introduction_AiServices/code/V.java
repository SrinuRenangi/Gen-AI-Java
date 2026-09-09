package com.genai.langchain4j.aiservices;

import java.lang.annotation.*;

/**
 * Binds a method parameter to a template placeholder variable {{name}}.
 * Matches dev.langchain4j.service.V.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface V {
    String value();
}
