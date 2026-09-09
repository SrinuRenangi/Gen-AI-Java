package com.javagenai.day13;

import java.util.function.Function;

public class SimulatedAOPProxy {

    public static <T, R> Function<T, R> wrapWithAspect(String operationName, Function<T, R> target) {
        return input -> {
            System.out.printf("[PROXY @Before]: Intercepted call to '%s' with prompt: \"%s\"%n", operationName, input);
            long start = System.currentTimeMillis();
            try {
                R result = target.apply(input);
                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("[PROXY @AfterReturning]: '%s' completed successfully in %d ms.%n", operationName, elapsed);
                return result;
            } catch (Exception ex) {
                long elapsed = System.currentTimeMillis() - start;
                System.err.printf("[PROXY @AfterThrowing]: '%s' threw %s after %d ms!%n", 
                                  operationName, ex.getClass().getSimpleName(), elapsed);
                throw ex;
            }
        };
    }
}
