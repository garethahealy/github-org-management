package com.garethahealy.githubstats.processors.users.jobs;

import com.garethahealy.githubstats.factories.LdapConnectionLease;
import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.predicates.GHUserFilters;
import com.garethahealy.githubstats.predicates.OrgMemberFilters;
import com.garethahealy.githubstats.services.github.GitHubOrganizationLookupService;
import com.garethahealy.githubstats.services.ldap.LdapSearchService;
import com.garethahealy.githubstats.services.users.OrgMemberCsvService;
import com.garethahealy.githubstats.services.users.OrgMemberValidationService;
import freemarker.template.TemplateException;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Search and collect the GitHub members from the Red Hat LDAP.
 */
@ApplicationScoped
public class CollectMembersFromRedHatLdapProcessor {

    private final Logger logger;
    private final GitHubOrganizationLookupService gitHubOrganizationLookupService;
    private final LdapSearchService ldapSearchService;
    private final OrgMemberCsvService orgMemberCsvService;
    private final OrgMemberValidationService orgMemberValidationService;

    public CollectMembersFromRedHatLdapProcessor(Logger logger, GitHubOrganizationLookupService gitHubOrganizationLookupService, OrgMemberCsvService orgMemberCsvService, LdapSearchService ldapSearchService, OrgMemberValidationService orgMemberValidationService) {
        this.logger = logger;
        this.gitHubOrganizationLookupService = gitHubOrganizationLookupService;
        this.orgMemberCsvService = orgMemberCsvService;
        this.ldapSearchService = ldapSearchService;
        this.orgMemberValidationService = orgMemberValidationService;
    }

    public void run(String organization, File ldapMembersCsv, File supplementaryCsv, boolean validateCsv, int limit) throws IOException, LdapException, TemplateException, ExecutionException, InterruptedException, URISyntaxException {
        logger.infof("Looking up %s", organization);

        OrgMemberRepository ldapMembers = orgMemberCsvService.parse(ldapMembersCsv);
        OrgMemberRepository supplementaryMembers = orgMemberCsvService.parse(supplementaryCsv);

        GHOrganization org = gitHubOrganizationLookupService.getOrganization(organization);

        run(org, ldapMembers, supplementaryMembers, validateCsv, limit);
    }

    private void run(GHOrganization org, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, boolean validateCsv, int limit) throws IOException, LdapException, TemplateException, ExecutionException, InterruptedException, URISyntaxException {
        List<GHUser> githubMembers = gitHubOrganizationLookupService.getMembers(org);

        logger.infof("There are %s GitHub members", githubMembers.size());
        logger.infof("There are %s known members and %s supplementary members in the CSVs, total %s", ldapMembers.size(), supplementaryMembers.size(), (ldapMembers.size() + supplementaryMembers.size()));

        removeFromIfNotGitHubMember(githubMembers, ldapMembers);
        removeFromIfNotGitHubMember(githubMembers, supplementaryMembers);

        if (validateCsv) {
            searchViaLdapForLdapCsvMembers(ldapMembers, supplementaryMembers);
            searchViaLdapForSupplementaryCsvMembers(ldapMembers, supplementaryMembers);

            orgMemberValidationService.validate(ldapMembers);
            orgMemberValidationService.validate(supplementaryMembers);
        }

        searchViaLdapForUnknownMembers(githubMembers, ldapMembers, supplementaryMembers, limit);

        removeLdapFromSupplementary(ldapMembers, supplementaryMembers);

        orgMemberCsvService.write(ldapMembers);
        orgMemberCsvService.write(supplementaryMembers);
    }

    /**
     * Remove any member from the OrgMemberRepository which cannot be found in GitHub anymore
     *
     * @param githubMembers
     * @param foundMembers
     */
    private void removeFromIfNotGitHubMember(List<GHUser> githubMembers, OrgMemberRepository foundMembers) {
        List<OrgMember> toRemove = new ArrayList<>();
        for (OrgMember member : foundMembers.filter(OrgMemberFilters.deleteAfterIsNull())) {
            Optional<GHUser> found = githubMembers.stream().filter(GHUserFilters.equals(member)).findFirst();
            if (found.isEmpty()) {
                logger.infof("%s is in %s CSV but no-longer a GitHub member", member.gitHubUsername(), foundMembers.name());

                toRemove.add(member);
            }
        }

        foundMembers.remove(toRemove);
    }

    /**
     * Search LDAP for everyone that is in the OrgMemberRepository to validate it is still correct
     *
     * @param ldapMembers
     * @param supplementaryMembers
     * @throws IOException
     * @throws LdapException
     */
    private void searchViaLdapForLdapCsvMembers(OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) throws IOException, ExecutionException, InterruptedException {
        LocalDate deleteAfter = LocalDate.now().plusWeeks(1);
        List<OrgMember> filteredMembers = ldapMembers.filter(OrgMemberFilters.deleteAfterIsNull());

        logger.infof("Searching LDAP for %s ldap members from %s", filteredMembers.size(), ldapMembers.name());

        List<OrgMember> replace = new ArrayList<>();
        List<OrgMember> add = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<OrgMember>> futures = new ArrayList<>();
            for (OrgMember current : filteredMembers) {
                futures.add(executor.submit(() -> {
                    try (LdapConnectionLease lease = ldapSearchService.open()) {
                        LdapConnection connection = lease.connection();
                        OrgMember found = ldapSearchService.retrieve(connection, current);
                        if (found == null) {
                            logger.warnf("%s cannot be found in LDAP via PrimaryMail and GitHub social for %s CSV", current.gitHubUsername(), ldapMembers.name());

                            String email = ldapSearchService.searchOnPrimaryMail(connection, current.redhatEmailAddress());
                            if (email.isEmpty()) {
                                replace.add(current.withDeleteAfter(deleteAfter));
                            } else {
                                logger.warnf("-> but found %s in LDAP - have they unlinked their GitHub social? moving to %s", current.redhatEmailAddress(), supplementaryMembers.name());

                                add.add(current);
                            }
                        } else {
                            if (current != found) {
                                // Maybe they've added their quay or extra details we didn't get the first time
                                replace.add(found);
                            }
                        }

                        return current;
                    } catch (LdapException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            for (Future<OrgMember> future : futures) {
                future.get();
            }
        }

        // Update lists
        ldapMembers.replace(replace);
        ldapMembers.remove(add);
        supplementaryMembers.put(add);
    }

    /**
     * Search LDAP for everyone that is in the supplementary OrgMemberRepository to validate it is correct, if they are found, add them to the LDAP OrgMemberRepository
     *
     * @param ldapMembers
     * @param supplementaryMembers
     * @throws IOException
     * @throws LdapException
     * @throws URISyntaxException
     */
    private void searchViaLdapForSupplementaryCsvMembers(OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) throws IOException, ExecutionException, InterruptedException {
        LocalDate deleteAfter = LocalDate.now().plusWeeks(1);
        List<OrgMember> filteredMembers = supplementaryMembers.filter(OrgMemberFilters.deleteAfterIsNullAndMemberNotBot());

        logger.infof("Searching LDAP for %s supplementary members from %s", filteredMembers.size(), supplementaryMembers.name());

        List<OrgMember> replace = new ArrayList<>();
        List<OrgMember> add = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<OrgMember>> futures = new ArrayList<>();
            for (OrgMember current : filteredMembers) {
                futures.add(executor.submit(() -> {
                    try (LdapConnectionLease lease = ldapSearchService.open()) {
                        LdapConnection connection = lease.connection();
                        String primaryMail = ldapSearchService.searchOnPrimaryMail(connection, current.redhatEmailAddress());
                        if (primaryMail.isEmpty()) {
                            logger.warnf("%s cannot be found in LDAP via PrimaryMail for %s CSV", current.gitHubUsername(), supplementaryMembers.name());

                            replace.add(current.withDeleteAfter(deleteAfter));
                        } else {
                            OrgMember found = ldapSearchService.retrieve(connection, current);
                            if (found != null) {
                                logger.infof("%s has linked their account, adding to %s CSV", current.gitHubUsername(), ldapMembers.name());

                                add.add(found);
                            }
                        }

                        return current;
                    } catch (LdapException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            for (Future<OrgMember> future : futures) {
                future.get();
            }
        }

        // Update lists
        supplementaryMembers.replace(replace);
        ldapMembers.put(add);
    }

    /**
     * Search LDAP for everyone that is not in the ldapMembers or supplementaryMembers (i.e.: unknown) CSVs but a GitHub member
     *
     * @param githubMembers
     * @param ldapMembers
     * @param supplementaryMembers
     * @param limit
     * @throws IOException
     * @throws LdapException
     * @throws URISyntaxException
     */
    private void searchViaLdapForUnknownMembers(List<GHUser> githubMembers, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, int limit) throws IOException, ExecutionException, InterruptedException {
        int limitBy = limit <= 0 ? githubMembers.size() : limit;
        List<GHUser> unknownUsers = githubMembers.stream().filter(GHUserFilters.notContains(ldapMembers, supplementaryMembers)).limit(limitBy).toList();

        if (!unknownUsers.isEmpty()) {
            logger.infof("Searching LDAP for %s unknown GitHub members", unknownUsers.size());

            List<OrgMember> add = new ArrayList<>();

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<GHUser>> futures = new ArrayList<>();

                for (GHUser user : unknownUsers) {
                    futures.add(executor.submit(() -> {
                        try (LdapConnectionLease lease = ldapSearchService.open()) {
                            LdapConnection connection = lease.connection();

                            String rhEmail = ldapSearchService.searchOnGitHubSocial(connection, user.getLogin());
                            if (rhEmail.isEmpty()) {
                                logger.warnf("%s cannot be found in LDAP via GitHub social", user.getLogin());
                            } else {
                                logger.infof("Adding %s to %s CSV", user.getLogin(), ldapMembers.name());

                                OrgMember orgMember = orgMemberValidationService.validate(ldapSearchService.retrieve(connection, user.getLogin(), rhEmail));
                                add.add(orgMember);

                            }

                            return user;
                        } catch (LdapException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                }

                for (Future<GHUser> future : futures) {
                    future.get();
                }
            }

            // Update lists
            ldapMembers.put(add);
        }
    }

    /**
     * Remove anyone from the supplementaryMembers, if they exist in ldapMembers
     *
     * @param ldapMembers
     * @param supplementaryMembers
     */
    private void removeLdapFromSupplementary(OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) {
        for (OrgMember current : ldapMembers.items()) {
            if (supplementaryMembers.containsKey(current.gitHubUsername())) {
                logger.infof("%s is in LDAP and Supplementary CSV, removing from Supplementary", current.redhatEmailAddress());

                supplementaryMembers.remove(current);
            }
        }
    }
}
