package com.javagenai.day09.mini_ioc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MiniApplicationContext {
    private final Map<Class<?>, Object> beanRegistry = new HashMap<>();

    public MiniApplicationContext(Class<?>... componentClasses) {
        try {
            // 1. Instantiation Phase
            for (Class<?> clazz : componentClasses) {
                if (clazz.isAnnotationPresent(MyComponent.class)) {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    beanRegistry.put(clazz, instance);
                    System.out.println("[Mini-IoC] Registered Bean: " + clazz.getSimpleName());
                }
            }

            // 2. Injection Phase
            for (Object bean : beanRegistry.values()) {
                for (Field field : bean.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(MyInject.class)) {
                        Class<?> fieldType = field.getType();
                        Object dependencyToInject = beanRegistry.get(fieldType);

                        if (dependencyToInject != null) {
                            field.setAccessible(true);
                            field.set(bean, dependencyToInject);
                            System.out.println("[Mini-IoC] Injected " + fieldType.getSimpleName() 
                                               + " into " + bean.getClass().getSimpleName() + "." + field.getName());
                        } else {
                            throw new RuntimeException("No bean found of type: " + fieldType.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("IoC Container Initialization Failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        T bean = (T) beanRegistry.get(requiredType);
        if (bean == null) {
            throw new IllegalArgumentException("No bean found for: " + requiredType.getName());
        }
        return bean;
    }
}
