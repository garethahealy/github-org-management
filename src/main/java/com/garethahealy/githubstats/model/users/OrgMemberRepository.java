package com.garethahealy.githubstats.model.users;

import com.garethahealy.githubstats.predicates.OrgMemberFilters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class OrgMemberRepository {

    private final Map<String, OrgMember> members;
    private final File input;

    public OrgMemberRepository(File input, Map<String, OrgMember> members) {
        this.input = input;
        this.members = members;
    }

    public String name() {
        return input.getName();
    }

    public Path path() {
        return input.toPath();
    }

    public List<OrgMember> items() {
        return new ArrayList<>(members.values());
    }

    public int size() {
        return members.size();
    }

    public void put(List<OrgMember> add) throws IOException {
        for (OrgMember current : add) {
            put(current);
        }
    }

    public OrgMember put(OrgMember add) throws IOException {
        return members.put(add.gitHubUsername(), add);
    }

    public void remove(List<OrgMember> remove) {
        for (OrgMember current : remove) {
            remove(current);
        }
    }

    public void remove(OrgMember remove) {
        members.remove(remove.gitHubUsername());
    }

    public void remove(LocalDate date) {
        List<OrgMember> remove = members.values().stream().filter(OrgMemberFilters.deleteAfter(date)).toList();
        remove(remove);
    }

    public void replace(List<OrgMember> replace) throws IOException {
        for (OrgMember current : replace) {
            members.replace(current.gitHubUsername(), current);
        }
    }

    public boolean containsKey(String key) {
        return members.containsKey(key);
    }

    public List<OrgMember> filter(Predicate<OrgMember> predicate) {
        return members.values().stream().filter(predicate).toList();
    }

    /**
     * Switch the keys around for the map to work against quay IDs
     *
     * @return
     */
    public OrgMemberRepository convertToQuay() {
        Map<String, OrgMember> quayBasedMembers = new HashMap<>();
        for (Map.Entry<String, OrgMember> entry : members.entrySet()) {
            if (!entry.getValue().linkedQuayUsernames().isEmpty()) {
                for (String quayUsername : entry.getValue().linkedQuayUsernames()) {
                    quayBasedMembers.put(quayUsername, entry.getValue());
                }
            }
        }

        return new OrgMemberRepository(input, quayBasedMembers);
    }
}
