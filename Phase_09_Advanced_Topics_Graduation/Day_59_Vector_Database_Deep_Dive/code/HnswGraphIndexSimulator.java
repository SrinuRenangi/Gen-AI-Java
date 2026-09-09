package com.genai.enterprise.vectordb;

import java.util.*;

/**
 * Hierarchical Navigable Small World (HNSW) Index Simulator.
 * Models multi-layer skip-graph traversal with logarithmic O(log N) search complexity.
 */
public class HnswGraphIndexSimulator {

    public record HnswConfig(int m, int efConstruction, int efSearch, int maxLayers) {}

    public record HnswNode(VectorItem item, int maxLevel, List<List<HnswNode>> neighborsByLevel) {
        public HnswNode(VectorItem item, int maxLevel) {
            this(item, maxLevel, new ArrayList<>());
            for (int i = 0; i <= maxLevel; i++) {
                neighborsByLevel.add(new ArrayList<>());
            }
        }
    }

    private final HnswConfig config;
    private final List<HnswNode> allNodes = new ArrayList<>();
    private HnswNode entryPoint = null;
    private int currentMaxLayer = 0;

    public HnswGraphIndexSimulator(HnswConfig config) {
        this.config = config;
    }

    public synchronized void add(VectorItem item) {
        // Probabilistic level assignment
        int level = assignRandomLevel();
        HnswNode newNode = new HnswNode(item, level);

        if (entryPoint == null) {
            entryPoint = newNode;
            currentMaxLayer = level;
            allNodes.add(newNode);
            return;
        }

        // Connect new node to nearest neighbors in each layer up to its level
        for (int l = 0; l <= level; l++) {
            List<HnswNode> layerNodes = getNodesAtLevel(l);
            layerNodes.sort(Comparator.comparingDouble(n -> 
                    DistanceMetric.cosineDistance(item.embedding(), n.item().embedding())));

            int connections = Math.min(config.m(), layerNodes.size());
            for (int i = 0; i < connections; i++) {
                HnswNode neighbor = layerNodes.get(i);
                newNode.neighborsByLevel().get(l).add(neighbor);
                if (neighbor.neighborsByLevel().get(l).size() < config.m() * 2) {
                    neighbor.neighborsByLevel().get(l).add(newNode);
                }
            }
        }

        allNodes.add(newNode);
        if (level > currentMaxLayer) {
            currentMaxLayer = level;
            entryPoint = newNode;
        }
    }

    public List<FlatVectorIndex.SearchResult> search(float[] queryVector, int topK) {
        if (entryPoint == null) return Collections.emptyList();

        HnswNode curr = entryPoint;
        // 1. Traverse top highway layers down to layer 1 using greedy nearest neighbor
        for (int l = currentMaxLayer; l > 0; l--) {
            curr = greedySearchLayer(curr, queryVector, l);
        }

        // 2. Base layer 0: Explore candidates up to efSearch
        PriorityQueue<FlatVectorIndex.SearchResult> candidates = new PriorityQueue<>(
                Comparator.comparingDouble(FlatVectorIndex.SearchResult::distance)
        );
        Set<String> visited = new HashSet<>();

        candidates.offer(new FlatVectorIndex.SearchResult(curr.item(), 
                DistanceMetric.cosineDistance(queryVector, curr.item().embedding())));
        visited.add(curr.item().id());

        PriorityQueue<FlatVectorIndex.SearchResult> resultPq = new PriorityQueue<>(
                Comparator.comparingDouble(FlatVectorIndex.SearchResult::distance).reversed()
        );

        while (!candidates.isEmpty()) {
            var nearest = candidates.poll();
            resultPq.offer(nearest);
            if (resultPq.size() > topK) {
                resultPq.poll();
            }

            // Inspect neighbors of nearest node at layer 0
            HnswNode node = findNode(nearest.item().id());
            if (node != null && node.neighborsByLevel().size() > 0) {
                for (HnswNode neighbor : node.neighborsByLevel().get(0)) {
                    if (visited.add(neighbor.item().id())) {
                        double d = DistanceMetric.cosineDistance(queryVector, neighbor.item().embedding());
                        candidates.offer(new FlatVectorIndex.SearchResult(neighbor.item(), d));
                    }
                }
            }
            if (visited.size() >= config.efSearch()) break;
        }

        List<FlatVectorIndex.SearchResult> results = new ArrayList<>();
        while (!resultPq.isEmpty()) results.add(resultPq.poll());
        Collections.reverse(results);
        return results;
    }

    private HnswNode greedySearchLayer(HnswNode curr, float[] query, int layer) {
        boolean changed = true;
        while (changed) {
            changed = false;
            double currentDist = DistanceMetric.cosineDistance(query, curr.item().embedding());
            if (layer < curr.neighborsByLevel().size()) {
                for (HnswNode n : curr.neighborsByLevel().get(layer)) {
                    double d = DistanceMetric.cosineDistance(query, n.item().embedding());
                    if (d < currentDist) {
                        curr = n;
                        changed = true;
                        break;
                    }
                }
            }
        }
        return curr;
    }

    private int assignRandomLevel() {
        double r = Math.random();
        int lvl = 0;
        while (r < 0.5 && lvl < config.maxLayers()) {
            lvl++;
            r = Math.random();
        }
        return lvl;
    }

    private List<HnswNode> getNodesAtLevel(int level) {
        List<HnswNode> res = new ArrayList<>();
        for (HnswNode n : allNodes) {
            if (n.maxLevel() >= level) res.add(n);
        }
        return res;
    }

    private HnswNode findNode(String id) {
        for (HnswNode n : allNodes) {
            if (n.item().id().equals(id)) return n;
        }
        return null;
    }

    public int size() { return allNodes.size(); }
}
