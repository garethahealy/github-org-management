package com.garethahealy.githuborgmanagement.services.users;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import com.garethahealy.githuborgmanagement.model.users.OrgMemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles reading and writing to a CSV file for OrgMembers
 */
@ApplicationScoped
public class OrgMemberCsvService {

    private final Logger logger;

    public OrgMemberCsvService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Parse the input into a OrgMember
     *
     * @param input
     * @return
     * @throws IOException
     */
    public OrgMemberRepository parse(File input) throws IOException {
        Map<String, OrgMember> answer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader(OrgMember.Headers.class)
            .setSkipHeaderRecord(true)
            .get();

        try (Reader reader = new BufferedReader(new FileReader(input, StandardCharsets.UTF_8))) {
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            for (CSVRecord record : records) {
                OrgMember orgMember = OrgMember.from(record);

                answer.put(orgMember.gitHubUsername(), orgMember);
            }
        }

        logger.infof("Parsed %s from %s", answer.size(), input);

        // If the input is the supplementary.csv, we need make sure the bot is in the file
        if (input.getName().contains("supplementary")) {
            if (!answer.containsKey(OrgMember.botGithubUsername())) {
                answer.put(OrgMember.botGithubUsername(), OrgMember.bot());
            }
        }

        return new OrgMemberRepository(input, answer);
    }

    /**
     * Write OrgMembers to output
     *
     * @param orgMemberRepository
     * @throws IOException
     */
    public void write(OrgMemberRepository orgMemberRepository) throws IOException {
        List<OrgMember> members = orgMemberRepository.items();
        Path output = orgMemberRepository.path();

        if (members.isEmpty()) {
            logger.infof("OrgMembers is empty, ignoring for %s", output);
        } else {
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }

            if (!Files.exists(output)) {
                Files.createFile(output);
            }

            CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(OrgMember.Headers.class)
                .get();

            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                    for (OrgMember current : members) {
                        csvPrinter.printRecord(current.toArray());
                    }
                }
            }

            logger.infof("Written %s members to %s", members.size(), output);
        }
    }
}
