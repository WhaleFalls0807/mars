package com.whaleal.mars.core.aggregation.stages;

/**
 * Returns plan cache information for a collection. The stage returns a document for each plan cache entry.
 *
 * @aggregation.expression $planCacheStats
 */
public class PlanCacheStats extends Stage {
    protected PlanCacheStats() {
        super("$planCacheStats");
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     * @deprecated use {@link #planCacheStats()}
     */
    @Deprecated()
    public static PlanCacheStats of() {
        return new PlanCacheStats();
    }

    /**
     * Creates the new stage
     *
     * @return the new stage
     */
    public static PlanCacheStats planCacheStats() {
        return new PlanCacheStats();
    }
}
