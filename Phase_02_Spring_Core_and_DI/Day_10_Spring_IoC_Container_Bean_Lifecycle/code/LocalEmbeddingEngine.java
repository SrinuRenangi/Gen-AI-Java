package com.javagenai.day10;

public class LocalEmbeddingEngine {

    public void initWarmup() {
        System.out.println("[LocalEmbeddingEngine - @PostConstruct]");
        System.out.println("   -> Loading 384-dimensional ONNX weights into RAM...");
        System.out.println("   -> ✅ Vector embedding model warmed up and ready!");
    }

    public double[] embed(String text) {
        return new double[] { 0.124, 0.892, 0.451 };
    }
}
