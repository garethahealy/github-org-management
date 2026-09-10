package com.garethahealy.githuborgmanagement.services.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parses GitHub GHContent data into usable formats
 */
@ApplicationScoped
public class GitHubFileRetrievalService {

    private final YAMLMapper mapper;
    private final Logger logger;

    public GitHubFileRetrievalService(Logger logger, YAMLMapper mapper) {
        this.logger = logger;
        this.mapper = mapper;
    }

    public Set<String> getRepos(GHContent configYaml, boolean validateOrgConfig) throws IOException {
        Set<String> answer = new TreeSet<>();

        if (validateOrgConfig && configYaml != null) {
            String configContent = getFileYaml(configYaml);
            JsonNode configMap = mapper.readValue(configContent, JsonNode.class);

            recursiveGetRepos(answer, configMap.get("orgs"));
        }

        return answer;
    }

    private void recursiveGetRepos(Set<String> allRepos, JsonNode parent) {
        for (JsonNode child : parent) {
            JsonNode repos = child.get("repos");

            if (repos != null) {
                allRepos.addAll(IteratorUtils.toList(repos.fieldNames()));
            }

            recursiveGetRepos(allRepos, child);
        }
    }

    public Set<String> getArchivedRepos(GHContent configYaml, boolean validateOrgConfig) throws IOException {
        Set<String> answer = new TreeSet<>();

        if (validateOrgConfig) {
            String configContent = getFileYaml(configYaml);
            JsonNode configMap = mapper.readValue(configContent, JsonNode.class);

            JsonNode archived = configMap.get("orgs").get("redhat-cop").get("teams").get("aarchived").get("repos");
            answer.addAll(IteratorUtils.toList(archived.fieldNames()));
        }

        return answer;
    }

    public Set<String> getConfigMembers(GHContent configYaml) throws IOException {
        Set<String> answer = new TreeSet<>();
        if (configYaml != null) {
            String configContent = getFileYaml(configYaml);
            JsonNode configMap = mapper.readValue(configContent, JsonNode.class);

            JsonNode admins = configMap.get("orgs").get("redhat-cop").get("admins");
            for (JsonNode current : admins) {
                answer.add(current.asText());
            }

            recursiveGetConfigMembers(answer, configMap.get("orgs"));
        }

        return answer;
    }

    private void recursiveGetConfigMembers(Set<String> allMembers, JsonNode parent) {
        for (JsonNode child : parent) {
            JsonNode maintainers = child.get("maintainers");
            JsonNode members = child.get("members");

            if (maintainers != null) {
                for (JsonNode current : maintainers) {
                    allMembers.add(current.asText());
                }
            }

            if (members != null) {
                for (JsonNode current : members) {
                    allMembers.add(current.asText());
                }
            }

            recursiveGetConfigMembers(allMembers, child);
        }
    }

    public Set<String> getAnsibleMembers(GHContent groupVarsYaml) throws IOException {
        Set<String> answer = new TreeSet<>();

        String varsContent = getFileYaml(groupVarsYaml);
        JsonNode allVars = mapper.readValue(varsContent, JsonNode.class);

        JsonNode orgs = allVars.get("orgs");
        for (JsonNode org : orgs) {
            JsonNode repos = org.get("repos");
            for (JsonNode repo : repos) {
                JsonNode permissions = repo.get("permissions");
                for (JsonNode inner : permissions) {
                    String type = inner.get("type").asText();
                    if (type.equalsIgnoreCase("user")) {
                        answer.add(inner.get("name").asText());
                    }
                }
            }

            JsonNode teams = org.get("teams");
            for (JsonNode team : teams) {
                JsonNode members = team.get("members");
                if (members != null) {
                    for (JsonNode member : members) {
                        answer.add(member.get("name").asText());
                    }
                }
            }
        }

        return answer;
    }

    private String getFileYaml(GHContent content) throws IOException {
        String answer = null;

        if (content != null) {
            try {
                File outputFile = new File("target/" + content.getOwner().getOwner().getLogin() + "-" + content.getOwner().getName() + "-" + content.getName());
                FileUtils.copyInputStreamToFile(content.read(), outputFile);

                answer = FileUtils.readFileToString(outputFile, Charset.defaultCharset());

                logger.infof("%s written to disk", outputFile);
            } catch (GHFileNotFoundException ex) {
                logger.warnf("Did not find %s/%s/%s - maybe branch has been deleted", content.getOwner().getOwner().getLogin(), content.getOwner().getName(), content.getName());
                logger.warn("Failure", ex);
            }
        }

        return answer;
    }
}
