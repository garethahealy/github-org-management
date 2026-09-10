package com.garethahealy.githuborgmanagement.commands.users.setup;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.testutils.CsvParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CollectMembersFromRedHatLdapCommandSetup {

    public static class run {
        public static File LDAP = new File("target/CollectMembersFromRedHatLdap/ldap-members-run.csv");
        public static File SUPPLEMENTARY = new File("target/CollectMembersFromRedHatLdap/supplementary-run.csv");
    }

    public static class runWithLimit {
        public static File LDAP = new File("target/CollectMembersFromRedHatLdap/ldap-members-runWithLimit.csv");
        public static File SUPPLEMENTARY = new File("target/CollectMembersFromRedHatLdap/supplementary-runWithLimit.csv");
    }

    public static class runValidateMovingMembers {
        public static File LDAP = new File("target/CollectMembersFromRedHatLdap/ldap-members-runValidateMovingMembers.csv");
        public static File SUPPLEMENTARY = new File("target/CollectMembersFromRedHatLdap/supplementary-runValidateMovingMembers.csv");
    }

    public void setup() throws IOException {
        CsvParser csvParser = new CsvParser();

        preRun();
        preRunWithLimit();
        preRunValidateMovingMembers(csvParser);
    }

    private void preRun() throws IOException {
        FileUtils.copyFile(new File("ldap-members.csv"), run.LDAP);
        FileUtils.copyFile(new File("supplementary.csv"), run.SUPPLEMENTARY);
    }

    private void preRunWithLimit() throws IOException {
        FileUtils.copyFile(new File("supplementary.csv"), runWithLimit.SUPPLEMENTARY);
    }

    private void preRunValidateMovingMembers(CsvParser csvParser) throws IOException {
        FileUtils.copyFile(new File("ldap-members.csv"), runValidateMovingMembers.LDAP);
        FileUtils.copyFile(new File("supplementary.csv"), runValidateMovingMembers.SUPPLEMENTARY);

        // Pre-Test Data
        Map<String, OrgMember> ldapInput = csvParser.parse(runValidateMovingMembers.LDAP);
        Map<String, OrgMember> supplementaryInput = csvParser.parse(runValidateMovingMembers.SUPPLEMENTARY);

        OrgMember me = ldapInput.values().stream().filter(member -> member.gitHubUsername().equalsIgnoreCase("garethahealy")).toList().getFirst();
        ldapInput.remove(me.gitHubUsername());
        supplementaryInput.put(me.gitHubUsername(), me);

        csvParser.write(ldapInput.values(), runValidateMovingMembers.LDAP.toPath());
        csvParser.write(supplementaryInput.values(), runValidateMovingMembers.SUPPLEMENTARY.toPath());
    }
}
