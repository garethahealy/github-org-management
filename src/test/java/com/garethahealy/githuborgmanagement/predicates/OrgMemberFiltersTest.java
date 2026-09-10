package com.garethahealy.githuborgmanagement.predicates;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrgMemberFiltersTest {

    @Test
    void deleteAfterMatchesWhenDateIsAfter() {
        LocalDate deleteAfter = LocalDate.of(2026, 2, 1);
        OrgMember member = new OrgMember("me@redhat.com", "member", List.of(), List.of(), OrgMember.Source.Manual, deleteAfter, null);

        assertTrue(OrgMemberFilters.deleteAfter(LocalDate.of(2026, 2, 2)).test(member));
    }

    @Test
    void deleteAfterDoesNotMatchOnSameDate() {
        LocalDate deleteAfter = LocalDate.of(2026, 2, 1);
        OrgMember member = new OrgMember("me@redhat.com", "member", List.of(), List.of(), OrgMember.Source.Manual, deleteAfter, null);

        assertFalse(OrgMemberFilters.deleteAfter(LocalDate.of(2026, 2, 1)).test(member));
    }

    @Test
    void deleteAfterIsNullMatchesOnlyNull() {
        OrgMember member = new OrgMember("me@redhat.com", "member", List.of(), List.of(), OrgMember.Source.Manual, null, null);

        assertTrue(OrgMemberFilters.deleteAfterIsNull().test(member));
    }

    @Test
    void deleteAfterIsNotNullMatchesOnlyNonNull() {
        OrgMember member = new OrgMember("me@redhat.com", "member", List.of(), List.of(), OrgMember.Source.Manual, LocalDate.now(), null);

        assertTrue(OrgMemberFilters.deleteAfterIsNotNull().test(member));
    }

    @Test
    void deleteAfterIsNullAndMemberNotBotMatchesRegularUser() {
        OrgMember member = new OrgMember("me@redhat.com", "member", List.of(), List.of(), OrgMember.Source.Manual, null, null);

        assertTrue(OrgMemberFilters.deleteAfterIsNullAndMemberNotBot().test(member));
    }

    @Test
    void deleteAfterIsNullAndMemberNotBotExcludesBot() {
        OrgMember member = OrgMember.bot();

        assertFalse(OrgMemberFilters.deleteAfterIsNullAndMemberNotBot().test(member));
    }
}
