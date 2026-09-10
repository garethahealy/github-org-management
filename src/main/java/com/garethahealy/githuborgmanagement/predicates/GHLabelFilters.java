package com.garethahealy.githuborgmanagement.predicates;

import org.kohsuke.github.GHLabel;

import java.util.function.Predicate;

public final class GHLabelFilters {

    private GHLabelFilters() {
    }

    /**
     * Filter by the name
     *
     * @param name
     * @return
     */
    public static Predicate<GHLabel> equals(String name) {
        return label -> name.equalsIgnoreCase(label.getName());
    }
}
