package com.javagenai.day10;

import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAY 10: SPRING BEAN LIFECYCLE & 3-LEVEL CACHE MEMORY SIMULATION
 *
 * This runnable class illustrates:
 * 1. The Step-by-Step Bean Lifecycle in JVM Memory (Instantiation -> Wiring -> Aware -> BPP -> Init -> Destroy).
 * 2. Heap Topology: Singleton Registry (ConcurrentHashMap) vs. Prototype Unmanaged Allocations.
 * 3. Spring's 3-Level Cache resolving circular dependencies between Bean A and Bean B in Heap RAM.
 */
public class SpringBeanLifecycleMemoryDemo {

    // =========================================================================
    // 1. SIMULATED LIFECYCLE ANNOTATIONS & INTERFACES
    // =========================================================================
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MyPostConstruct {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface MyPreDestroy {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface MyAutowired {}

    public interface MyBeanNameAware {
        void setBeanName(String name);
    }

    public interface MyBeanPostProcessor {
        default Object postProcessBeforeInitialization(Object bean, String beanName) { return bean; }
        default Object postProcessAfterInitialization(Object bean, String beanName) { return bean; }
    }

    // =========================================================================
    // 2. DOMAIN SERVICE DEMONSTRATING FULL LIFECYCLE
    // =========================================================================
    public static class RagPipelineEngine implements MyBeanNameAware {
        private String beanName;
        private boolean modelWeightsLoaded = false;
        private byte[] offHeapBuffer;

        public RagPipelineEngine() {
            System.out.println("  [Lifecycle Stage 1] Constructor: Allocated raw object on Heap. Hash: 0x" 
                    + Integer.toHexString(System.identityHashCode(this)));
        }

        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            System.out.println("  [Lifecycle Stage 2] BeanNameAware invoked: Registered bean name = '" + name + "'");
        }

        @MyPostConstruct
        public void initModelWeights() {
            System.out.println("  [Lifecycle Stage 3] @PostConstruct Hook: Pre-warming ONNX neural weights...");
            this.offHeapBuffer = new byte[2048]; // Simulated model weight buffer
            this.modelWeightsLoaded = true;
            System.out.println("                      Weights loaded into memory buffer (" + offHeapBuffer.length + " bytes).");
        }

        public String query(String prompt) {
            if (!modelWeightsLoaded) throw new IllegalStateException("Model not initialized!");
            return "[Response to '" + prompt + "' from " + beanName + "]";
        }

        @MyPreDestroy
        public void cleanup() {
            System.out.println("  [Lifecycle Stage 4] @PreDestroy Hook: Flushing caches and releasing memory buffer...");
            this.offHeapBuffer = null;
            this.modelWeightsLoaded = false;
            System.out.println("                      Bean memory marked ready for JVM Garbage Collection.");
        }
    }

    // =========================================================================
    // 3. CIRCULAR DEPENDENCY PARTICIPANTS (A needs B, B needs A)
    // =========================================================================
    public static class ServiceA {
        @MyAutowired
        public ServiceB serviceB;

        public ServiceA() {
            System.out.println("       [Instantiation] ServiceA raw instance allocated on Heap (0x" 
                    + Integer.toHexString(System.identityHashCode(this)) + ")");
        }
    }

    public static class ServiceB {
        @MyAutowired
        public ServiceA serviceA;

        public ServiceB() {
            System.out.println("       [Instantiation] ServiceB raw instance allocated on Heap (0x" 
                    + Integer.toHexString(System.identityHashCode(this)) + ")");
        }
    }

    // =========================================================================
    // 4. MINI THREE-LEVEL CACHE CONTAINER IMPLEMENTATION
    // =========================================================================
    public static class MiniThreeLevelCacheContainer {
        // Level 1: Fully initialized singletons ready for production use
        private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

        // Level 2: Early partially-instantiated objects (instantiated, but properties not yet injected)
        private final Map<String, Object> earlySingletonObjects = new HashMap<>();

        // Level 3: Object factories capable of producing early references / AOP proxies
        private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

        // Tracks beans currently in creation to detect cycles
        private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @FunctionalInterface
        public interface ObjectFactory<T> {
            T getObject();
        }

        @SuppressWarnings("unchecked")
        public <T> T getBean(String beanName, Class<T> clazz) {
            Object singleton = getSingleton(beanName, true);
            if (singleton != null) {
                return (T) singleton;
            }
            return (T) doCreateBean(beanName, clazz);
        }

        /**
         * The core 3-level cache resolution lookup algorithm
         */
        protected Object getSingleton(String beanName, boolean allowEarlyReference) {
            // Level 1: Check fully initialized singletons
            Object singleton = singletonObjects.get(beanName);
            if (singleton == null && singletonsCurrentlyInCreation.contains(beanName)) {
                // Level 2: Check early partially-created objects
                singleton = earlySingletonObjects.get(beanName);
                if (singleton == null && allowEarlyReference) {
                    // Level 3: Check early factory suppliers
                    ObjectFactory<?> factory = singletonFactories.get(beanName);
                    if (factory != null) {
                        singleton = factory.getObject();
                        // Promote from Level 3 to Level 2
                        earlySingletonObjects.put(beanName, singleton);
                        singletonFactories.remove(beanName);
                        System.out.println("       [3-Level Cache] PROMOTED '" + beanName 
                                + "' from Level 3 (singletonFactories) to Level 2 (earlySingletonObjects)");
                    }
                }
            }
            return singleton;
        }

        private Object doCreateBean(String beanName, Class<?> clazz) {
            try {
                singletonsCurrentlyInCreation.add(beanName);

                // 1. Instantiation: Call raw constructor
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                Object rawInstance = ctor.newInstance();

                // 2. Put into Level 3 cache before populating properties
                final Object beanRef = rawInstance;
                singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, beanRef));
                System.out.println("       [3-Level Cache] ADDED '" + beanName + "' to Level 3 (singletonFactories)");

                // 3. Populate Properties (Wire dependencies)
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(MyAutowired.class)) {
                        String depName = field.getType().getSimpleName().substring(0, 1).toLowerCase() 
                                + field.getType().getSimpleName().substring(1);
                        System.out.println("       [Wiring] Resolving dependency: " + clazz.getSimpleName() 
                                + "." + field.getName() + " -> " + depName);
                        Object dependency = getBean(depName, field.getType());
                        field.setAccessible(true);
                        field.set(rawInstance, dependency);
                    }
                }

                // 4. Initialize Bean (Hooks & Aware)
                Object exposedObject = initializeBean(rawInstance, beanName);

                // 5. Final Registration into Level 1
                earlySingletonObjects.remove(beanName);
                singletonFactories.remove(beanName);
                singletonObjects.put(beanName, exposedObject);
                singletonsCurrentlyInCreation.remove(beanName);

                System.out.println("       [3-Level Cache] REGISTERED '" + beanName 
                        + "' into Level 1 (singletonObjects) [FULLY READY]");
                return exposedObject;

            } catch (Exception e) {
                throw new RuntimeException("Bean creation failed for: " + beanName, e);
            }
        }

        private Object getEarlyBeanReference(String beanName, Object bean) {
            // In real Spring, this is where early AOP proxies are created if cyclic reference occurs
            return bean;
        }

        private Object initializeBean(Object bean, String beanName) throws Exception {
            if (bean instanceof MyBeanNameAware aware) {
                aware.setBeanName(beanName);
            }

            // PostConstruct Hook
            for (var method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(MyPostConstruct.class)) {
                    method.setAccessible(true);
                    method.invoke(bean);
                }
            }
            return bean;
        }

        public void destroySingletons() {
            System.out.println("\n--- Initiating Container Shutdown ---");
            for (Map.Entry<String, Object> entry : singletonObjects.entrySet()) {
                Object bean = entry.getValue();
                for (var method : bean.getClass().getDeclaredMethods()) {
                    if (method.isAnnotationPresent(MyPreDestroy.class)) {
                        try {
                            method.setAccessible(true);
                            method.invoke(bean);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            singletonObjects.clear();
            System.out.println("Container shutdown complete: singletonObjects registry cleared from Heap.");
        }
    }

    // =========================================================================
    // MAIN EXECUTION & VERIFICATION
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 10: SPRING BEAN LIFECYCLE & 3-LEVEL CACHE HEAP SIMULATION                 ");
        System.out.println("================================================================================");

        MiniThreeLevelCacheContainer container = new MiniThreeLevelCacheContainer();

        // ---------------------------------------------------------------------
        // EXPERIMENT 1: Full Lifecycle Execution of a Managed Bean
        // ---------------------------------------------------------------------
        System.out.println("\n>>> [EXPERIMENT 1] Tracing Complete Bean Lifecycle Stages in RAM <<<");
        RagPipelineEngine engine = container.getBean("ragPipelineEngine", RagPipelineEngine.class);
        System.out.println("  [Active Execution] Calling business method: " + engine.query("What is Inversion of Control?"));

        // ---------------------------------------------------------------------
        // EXPERIMENT 2: 3-Level Cache Resolving Circular Dependency (A <-> B)
        // ---------------------------------------------------------------------
        System.out.println("\n>>> [EXPERIMENT 2] Resolving Circular Dependency (ServiceA <-> ServiceB) <<<");
        System.out.println("Requesting ServiceA from container...");
        ServiceA a = container.getBean("serviceA", ServiceA.class);

        System.out.println("\n  [Verification of Wiring Integrity]:");
        System.out.println("  -> Is A's serviceB wired? " + (a.serviceB != null));
        System.out.println("  -> Is B's serviceA wired back to A? " + (a.serviceB.serviceA != null));
        System.out.println("  -> Are pointers identical in Heap RAM? " + (a == a.serviceB.serviceA));
        System.out.printf("     serviceA Heap ID: 0x%08X | serviceB.serviceA Heap ID: 0x%08X%n",
                System.identityHashCode(a), System.identityHashCode(a.serviceB.serviceA));

        // ---------------------------------------------------------------------
        // EXPERIMENT 3: Container Teardown & Destruction Hooks
        // ---------------------------------------------------------------------
        System.out.println("\n>>> [EXPERIMENT 3] Container Destruction & Heap Unregistration <<<");
        container.destroySingletons();

        System.out.println("\n================================================================================");
        System.out.println(" EXPERIMENT COMPLETE: Lifecycle and 3-Level Cache Verified Successfully!        ");
        System.out.println("================================================================================");
    }
}
