package com.genai.security.rbac;

/**
 * Thrown when an authenticated user lacks the required role or authority to invoke a method.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
