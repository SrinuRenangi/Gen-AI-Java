package com.genai.langchain4j.tools;

import java.lang.annotation.*;

/**
 * Documents a specific parameter of a @Tool method.
 * Matches dev.langchain4j.agent.tool.P.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface P {
    String value();
}
