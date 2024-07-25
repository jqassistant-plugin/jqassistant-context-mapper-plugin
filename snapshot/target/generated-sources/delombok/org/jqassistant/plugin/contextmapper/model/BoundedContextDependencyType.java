// Generated by delombok at Thu Jul 25 10:40:11 UTC 2024
package org.jqassistant.plugin.contextmapper.model;

/**
 * DDD dependency type between {@link BoundedContextDescriptor}s.
 *
 * @author Stephan Pirnbaum
 */
public enum BoundedContextDependencyType {
    CUSTOMER_SUPPLIER("C/S"), UPSTREAM_DOWNSTREAM("U/D"), SHARED_KERNEL("SK"), PARTNERSHIP("P");
    private final String type;

    BoundedContextDependencyType(String type) {
        this.type = type;
    }

    public static BoundedContextDependencyType getByType(String type) {
        if (type == null || type.isEmpty()) {
            return UPSTREAM_DOWNSTREAM;
        } else {
            switch (type) {
            case "C/S": 
                return CUSTOMER_SUPPLIER;
            case "U/D": 
                return UPSTREAM_DOWNSTREAM;
            case "SK": 
                return SHARED_KERNEL;
            case "P": 
                return PARTNERSHIP;
            default: 
                return null;
            }
        }
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }
}
