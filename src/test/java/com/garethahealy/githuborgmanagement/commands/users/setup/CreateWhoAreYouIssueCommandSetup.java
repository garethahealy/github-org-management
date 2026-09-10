package com.garethahealy.githuborgmanagement.commands.users.setup;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.testutils.CsvParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

public class CreateWhoAreYouIssueCommandSetup {

    private static class runReadWithoutMe {
        public static File LDAP = new File("target/CreateWhoAreYouIssue/ldap-members-runReadWithoutMe.csv");
        public static File SUPPLEMENTARY = new File("target/CreateWhoAreYouIssue/supplementary-runReadWithoutMe.csv");
    }

    public static class runUserMarkedForDeletion {
        public static File LDAP = new File("target/CreateWhoAreYouIssue/ldap-members-runUserMarkedForDeletion.csv");
        public static File SUPPLEMENTARY = new File("target/CreateWhoAreYouIssue/supplementary-runUserMarkedForDeletion.csv");
    }

    public void setup() throws IOException {
        CsvParser csvParser = new CsvParser();

        preRunReadWithoutMe(csvParser);
        preRunUserMarkedForDeletion(csvParser);
    }

    void preRunReadWithoutMe(CsvParser csvParser) throws IOException {
        FileUtils.copyFile(new File("ldap-members.csv"), runReadWithoutMe.LDAP);
        FileUtils.copyFile(new File("supplementary.csv"), runReadWithoutMe.SUPPLEMENTARY);

        Map<String, OrgMember> members = csvParser.parse(runReadWithoutMe.LDAP);
        members.remove("garethahealy");

        csvParser.write(members.values(), runReadWithoutMe.LDAP.toPath());
    }

    void preRunUserMarkedForDeletion(CsvParser csvParser) throws IOException {
        FileUtils.copyFile(new File("ldap-members.csv"), runUserMarkedForDeletion.LDAP);
        FileUtils.copyFile(new File("supplementary.csv"), runUserMarkedForDeletion.SUPPLEMENTARY);

        Map<String, OrgMember> members = csvParser.parse(runUserMarkedForDeletion.LDAP);
        OrgMember me = members.values().stream().filter(member -> member.gitHubUsername().equalsIgnoreCase("garethahealy")).toList().getFirst();
        members.replace(me.gitHubUsername(), me, me.withDeleteAfter(LocalDate.now().minusDays(1)));

        OrgMember andy = members.values().stream().filter(member -> member.gitHubUsername().equalsIgnoreCase("sabre1041")).toList().getFirst();
        members.replace(andy.gitHubUsername(), andy, andy.withDeleteAfter(LocalDate.now().plusDays(1)));

        csvParser.write(members.values(), runUserMarkedForDeletion.LDAP.toPath());
    }
}
