package com.garethahealy.githuborgmanagement.predicates;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHLabel;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GHLabelFiltersTest {

    @Test
    void equalsMatchesByName() {
        GHLabel label = Mockito.mock(GHLabel.class);
        when(label.getName()).thenReturn("admin");

        assertTrue(GHLabelFilters.equals("admin").test(label));
    }
}
