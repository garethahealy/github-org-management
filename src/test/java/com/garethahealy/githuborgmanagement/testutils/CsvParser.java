package com.garethahealy.githuborgmanagement.testutils;

import com.garethahealy.githuborgmanagement.model.users.OrgMember;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class CsvParser {

    public Map<String, OrgMember> parse(File input) throws IOException {
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

        return answer;
    }

    public void write(Collection<OrgMember> members, Path output) throws IOException {
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
    }
}
