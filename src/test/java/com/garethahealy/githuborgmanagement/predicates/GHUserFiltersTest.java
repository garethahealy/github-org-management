package com.garethahealy.githuborgmanagement.predicates;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.model.users.OrgMemberRepository;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHUser;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GHUserFiltersTest {

    @Test
    void equalsMatchesByLogin() {
        GHUser user = Mockito.mock(GHUser.class);
        when(user.getLogin()).thenReturn("garethahealy");

        OrgMember member = new OrgMember("me@redhat.com", "garethahealy", java.util.List.of(), java.util.List.of(), OrgMember.Source.Manual, null, null);

        assertTrue(GHUserFilters.equals(member).test(user));
    }

    @Test
    void notContainsReturnsTrueWhenUnknown() {
        GHUser user = Mockito.mock(GHUser.class);
        when(user.getLogin()).thenReturn("unknown");

        OrgMemberRepository ldapMembers = new OrgMemberRepository(new File("ldap.csv"), new HashMap<>());
        OrgMemberRepository supplementaryMembers = new OrgMemberRepository(new File("supplementary.csv"), new HashMap<>());

        assertTrue(GHUserFilters.notContains(ldapMembers, supplementaryMembers).test(user));
    }

    @Test
    void notContainsReturnsFalseWhenInLdapMembers() {
        GHUser user = Mockito.mock(GHUser.class);
        when(user.getLogin()).thenReturn("known");

        Map<String, OrgMember> ldap = new HashMap<>();
        ldap.put("known", OrgMember.from("known"));

        OrgMemberRepository ldapMembers = new OrgMemberRepository(new File("ldap.csv"), ldap);
        OrgMemberRepository supplementaryMembers = new OrgMemberRepository(new File("supplementary.csv"), new HashMap<>());

        assertFalse(GHUserFilters.notContains(ldapMembers, supplementaryMembers).test(user));
    }

    @Test
    void notContainsReturnsFalseWhenInSupplementaryMembers() {
        GHUser user = Mockito.mock(GHUser.class);
        when(user.getLogin()).thenReturn("known");

        Map<String, OrgMember> supplementary = new HashMap<>();
        supplementary.put("known", OrgMember.from("known"));

        OrgMemberRepository ldapMembers = new OrgMemberRepository(new File("ldap.csv"), new HashMap<>());
        OrgMemberRepository supplementaryMembers = new OrgMemberRepository(new File("supplementary.csv"), supplementary);

        assertFalse(GHUserFilters.notContains(ldapMembers, supplementaryMembers).test(user));
    }
}
