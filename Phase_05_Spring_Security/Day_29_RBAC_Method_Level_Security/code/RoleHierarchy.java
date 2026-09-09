package com.genai.security.rbac;

import java.util.*;

/**
 * Simulates Spring Security's RoleHierarchyImpl.
 * Example: ROLE_ADMIN > ROLE_LEAD_AI_ENGINEER > ROLE_PRO_USER > ROLE_FREE_USER
 * If a user holds ROLE_ADMIN, they automatically inherit all downstream roles and authorities.
 */
public class RoleHierarchy {

    private final Map<String, Set<String>> hierarchyMap = new HashMap<>();

    public void addHierarchy(String parentRole, String... childRoles) {
        hierarchyMap.computeIfAbsent(parentRole, k -> new HashSet<>())
                    .addAll(Arrays.asList(childRoles));
    }

    /**
     * Traverses the hierarchy map to find all reachable roles and authorities.
     */
    public Set<String> getReachableAuthorities(Collection<String> directAuthorities) {
        Set<String> reachable = new HashSet<>(directAuthorities);
        Queue<String> queue = new LinkedList<>(directAuthorities);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> children = hierarchyMap.get(current);
            if (children != null) {
                for (String child : children) {
                    if (reachable.add(child)) {
                        queue.add(child);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(reachable);
    }
}
