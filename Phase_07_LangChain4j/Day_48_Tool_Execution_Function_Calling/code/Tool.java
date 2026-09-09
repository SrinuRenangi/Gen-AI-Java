package com.genai.langchain4j.tools;

import java.lang.annotation.*;

/**
 * Marks a method as an executable tool accessible by an LLM agent.
 * Matches dev.langchain4j.agent.tool.Tool.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {
    String value() default "";
    String name() default "";
}
