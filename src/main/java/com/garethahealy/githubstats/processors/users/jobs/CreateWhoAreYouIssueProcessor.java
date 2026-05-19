package com.garethahealy.githubstats.processors.users.jobs;

import com.garethahealy.githubstats.model.users.OrgMember;
import com.garethahealy.githubstats.model.users.OrgMemberRepository;
import com.garethahealy.githubstats.predicates.GHIssueFilters;
import com.garethahealy.githubstats.predicates.GHUserFilters;
import com.garethahealy.githubstats.predicates.OrgMemberFilters;
import com.garethahealy.githubstats.services.github.GitHubOrganizationLookupService;
import com.garethahealy.githubstats.services.github.GitHubOrganizationWriterService;
import com.garethahealy.githubstats.services.ldap.DefaultLdapGuessService;
import com.garethahealy.githubstats.services.ldap.LdapGuessService;
import com.garethahealy.githubstats.services.ldap.NoopLdapGuessService;
import com.garethahealy.githubstats.services.users.OrgMemberCsvService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.quarkiverse.freemarker.TemplatePath;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.jboss.logging.Logger;
import org.kohsuke.github.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/**
 * Check the GitHub members are in the CSVs, collected via `CollectMembersFromRedHatLdapProcessor`
 * If not, raise an issue
 */
@ApplicationScoped
public class CreateWhoAreYouIssueProcessor {

    private final Logger logger;
    private final GitHubOrganizationLookupService gitHubOrganizationLookupService;
    private final GitHubOrganizationWriterService gitHubOrganizationWriterService;
    private final OrgMemberCsvService orgMemberCsvService;
    private final DefaultLdapGuessService defaultLdapGuessService;
    private final NoopLdapGuessService noopLdapGuessService;
    private LdapGuessService ldapGuessService;

    @Inject
    @TemplatePath("LinkSocialToLDAPComment.ftl")
    Template linkSocialToLDAPComment;

    @Inject
    @TemplatePath("GitHubMemberNotFoundInLdap.ftl")
    Template gitHubMemberNotFoundInLdap;

    @Inject
    public CreateWhoAreYouIssueProcessor(Logger logger, GitHubOrganizationLookupService gitHubOrganizationLookupService, GitHubOrganizationWriterService gitHubOrganizationWriterService, OrgMemberCsvService orgMemberCsvService, DefaultLdapGuessService defaultLdapGuessService, NoopLdapGuessService noopLdapGuessService) {
        this.logger = logger;
        this.gitHubOrganizationLookupService = gitHubOrganizationLookupService;
        this.gitHubOrganizationWriterService = gitHubOrganizationWriterService;
        this.orgMemberCsvService = orgMemberCsvService;
        this.defaultLdapGuessService = defaultLdapGuessService;
        this.noopLdapGuessService = noopLdapGuessService;
    }

    public void run(String organization, String issueRepo, File membersCsv, File supplementaryCsv, GHPermissionType perms, int limit, boolean isDryRun, boolean shouldGuess) throws IOException, ExecutionException, InterruptedException, TemplateException, LdapException {
        logger.infof("Looking up %s/%s", organization, issueRepo);

        ldapGuessService = shouldGuess ? defaultLdapGuessService : noopLdapGuessService;

        OrgMemberRepository ldapMembers = orgMemberCsvService.parse(membersCsv);
        OrgMemberRepository supplementaryMembers = orgMemberCsvService.parse(supplementaryCsv);

        GHOrganization org = gitHubOrganizationLookupService.getOrganization(organization);
        GHRepository orgRepo = gitHubOrganizationLookupService.getRepository(org, issueRepo);

        run(org, orgRepo, ldapMembers, supplementaryMembers, perms, limit, isDryRun);
    }

    private void run(GHOrganization org, GHRepository orgRepo, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, GHPermissionType perms, int limit, boolean isDryRun) throws IOException, ExecutionException, InterruptedException, TemplateException, LdapException {
        List<GHUser> githubMembers = gitHubOrganizationLookupService.getMembers(org);

        logger.infof("There are %s GitHub members", githubMembers.size());
        logger.infof("There are %s known members and %s supplementary members in the CSVs, total %s", ldapMembers.size(), supplementaryMembers.size(), (ldapMembers.size() + supplementaryMembers.size()));

        List<OrgMember> usersToInform = collectUnknownUsers(org, githubMembers, ldapMembers, supplementaryMembers, perms, limit);
        createLinkUsersIssue(usersToInform, orgRepo, perms, isDryRun);

        List<OrgMember> toBeDeleted = collectedMembersMarkedForDeletion(ldapMembers, supplementaryMembers);
        createRemoveNonRHIssue(toBeDeleted, orgRepo, isDryRun);

        removeMarkedForDeletion(ldapMembers);
        removeMarkedForDeletion(supplementaryMembers);

        orgMemberCsvService.write(ldapMembers);
        orgMemberCsvService.write(supplementaryMembers);
    }

    private List<OrgMember> collectUnknownUsers(GHOrganization org, List<GHUser> githubMembers, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, GHPermissionType perms, int limit) throws IOException, ExecutionException, InterruptedException, LdapException {
        List<OrgMember> usersToInform;
        if (GHPermissionType.READ == perms) {
            usersToInform = collectUnknownUsersWithRead(githubMembers, ldapMembers, supplementaryMembers);
        } else {
            usersToInform = collectUnknownUsersWithAdminOrWrite(org, ldapMembers, supplementaryMembers, perms, limit);
        }

        return usersToInform;
    }

    /**
     * Collect anyone who is a member of the GitHub org not in a OrgMemberRepository
     *
     * @param members
     * @param ldapMembers
     * @param supplementaryMembers
     * @return
     * @throws IOException
     * @throws LdapException
     */
    private List<OrgMember> collectUnknownUsersWithRead(List<GHUser> members, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) throws IOException, LdapException {
        List<OrgMember> usersToInform = new ArrayList<>();

        List<GHUser> unknownUsers = members.stream().filter(GHUserFilters.notContains(ldapMembers, supplementaryMembers)).toList();
        if (!unknownUsers.isEmpty()) {
            logger.infof("Collecting %s unknown members with %s", unknownUsers.size(), GHPermissionType.READ);

            for (GHUser member : unknownUsers) {
                usersToInform.add(guessWho(OrgMember.from(member)));
            }
        }

        return usersToInform;
    }

    /**
     * Collect anyone with ADMIN or WRITE access who is a member of the GitHub org not in a OrgMemberRepository
     *
     * @param org
     * @param ldapMembers
     * @param supplementaryMembers
     * @param perms
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private List<OrgMember> collectUnknownUsersWithAdminOrWrite(GHOrganization org, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, GHPermissionType perms, int limit) throws IOException, ExecutionException, InterruptedException, LdapException {
        Map<String, OrgMember> usersToInform = new ConcurrentHashMap<>();

        List<Future<Integer>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            List<GHTeam> teams = gitHubOrganizationLookupService.listTeams(org).toList();
            int limitBy = limit <= 0 ? teams.size() : limit;
            List<GHTeam> teamsLimited = teams.stream().limit(limitBy).toList();

            for (GHTeam team : teamsLimited) {
                futures.add(executor.submit(() -> runnable(usersToInform, team, ldapMembers, supplementaryMembers, perms)));
            }

            for (Future<Integer> future : futures) {
                future.get();
            }
        }

        return new ArrayList<>(usersToInform.values());
    }

    private Integer runnable(Map<String, OrgMember> usersToInform, GHTeam team, OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers, GHPermissionType perms) throws LdapException {
        Integer count = 0;

        try {
            List<GHRepository> teamRepositories = team.listRepositories().toList();
            List<GHUser> unknownUsers = team.getMembers().stream().filter(GHUserFilters.notContains(ldapMembers, supplementaryMembers)).toList();

            if (!unknownUsers.isEmpty()) {
                logger.infof("Looking at %s unknown members in %s team with %s", unknownUsers.size(), team.getName(), perms);

                for (GHUser member : unknownUsers) {
                    if (!usersToInform.containsKey(member.getLogin())) {
                        for (GHRepository repository : teamRepositories) {
                            if (repository.hasPermission(member, perms)) {
                                logger.warnf("Member %s has %s on %s - but we don't know who they are", member.getLogin(), perms, repository.getName());

                                usersToInform.put(member.getLogin(), guessWho(OrgMember.from(member)));
                                count++;

                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.error(ex);
        }

        return count;
    }

    /**
     * Attempt to guess who someone is via LDAP
     *
     * @param userToGuess
     * @return
     * @throws IOException
     * @throws LdapException
     */
    private OrgMember guessWho(OrgMember userToGuess) throws IOException, LdapException {
        OrgMember answer;

        OrgMember guessed = ldapGuessService.attempt(userToGuess);
        if (guessed == null) {
            logger.infof("Unable to guess %s", userToGuess.gitHubUsername());
            answer = userToGuess;
        } else {
            logger.infof("Guessed %s / %s via LDAP", guessed.gitHubUsername(), guessed.redhatEmailAddress());

            answer = guessed;
        }

        return answer;
    }

    /**
     * Collect anyone who has been marked for deletion - as they've left RH
     *
     * @param ldapMembers
     * @param supplementaryMembers
     * @return
     */
    private List<OrgMember> collectedMembersMarkedForDeletion(OrgMemberRepository ldapMembers, OrgMemberRepository supplementaryMembers) {
        List<OrgMember> toBeDeleted = new ArrayList<>();
        toBeDeleted.addAll(ldapMembers.filter(OrgMemberFilters.deleteAfter(LocalDate.now())));
        toBeDeleted.addAll(supplementaryMembers.filter(OrgMemberFilters.deleteAfter(LocalDate.now())));

        return toBeDeleted;
    }

    private void createLinkUsersIssue(List<OrgMember> usersToInform, GHRepository orgRepo, GHPermissionType permissions, boolean isDryRun) throws TemplateException, IOException {
        if (!usersToInform.isEmpty()) {
            usersToInform.sort(Comparator.comparing(OrgMember::gitHubUsername));

            Map<String, Object> root = new HashMap<>();
            root.put("users", usersToInform);
            root.put("permissions", permissions.toString());
            root.put("system", "GitHub");

            StringWriter stringWriter = new StringWriter();
            linkSocialToLDAPComment.process(root, stringWriter);

            if (isDryRun) {
                logger.warnf("DRY-RUN: Would have created issue in %s/%s for %s", orgRepo.getOwner().getLogin(), orgRepo.getName(), linkSocialToLDAPComment.getName());

                logger.warnf(stringWriter.toString());
            } else {
                GHOrganization orgWrite = gitHubOrganizationWriterService.getOrganization(orgRepo.getOwner().getLogin());
                GHRepository orgRepoWrite = gitHubOrganizationWriterService.getRepository(orgWrite, orgRepo.getName());

                List<GHIssue> openIssues = orgRepoWrite.getIssues(GHIssueState.OPEN).stream().filter(GHIssueFilters.isLinkUsersWith(permissions)).toList();
                if (openIssues.isEmpty()) {
                    GHIssue createdIssue = orgRepoWrite.createIssue("Request GitHub to Red Hat ID linkage for users with " + permissions)
                        .label("admin")
                        .body(stringWriter.toString())
                        .create();

                    logger.infof("Created issue: %s", createdIssue.getUrl());
                } else {
                    logger.warnf("There are %s open issues for %s, ignoring", openIssues.size(), linkSocialToLDAPComment.getName());
                }
            }
        }
    }

    private void createRemoveNonRHIssue(List<OrgMember> usersToRemove, GHRepository orgRepo, boolean isDryRun) throws TemplateException, IOException {
        if (!usersToRemove.isEmpty()) {
            usersToRemove.sort(Comparator.comparing(OrgMember::gitHubUsername, String.CASE_INSENSITIVE_ORDER));

            Map<String, Object> root = new HashMap<>();
            root.put("users", usersToRemove);

            StringWriter stringWriter = new StringWriter();
            gitHubMemberNotFoundInLdap.process(root, stringWriter);

            if (isDryRun) {
                logger.warnf("DRY-RUN: Would have created issue in %s/%s for %s", orgRepo.getOwner().getLogin(), orgRepo.getName(), gitHubMemberNotFoundInLdap.getName());

                logger.warnf(stringWriter.toString());
            } else {
                GHOrganization orgWrite = gitHubOrganizationWriterService.getOrganization(orgRepo.getOwner().getLogin());
                GHRepository orgRepoWrite = gitHubOrganizationWriterService.getRepository(orgWrite, orgRepo.getName());

                List<GHIssue> openIssues = orgRepoWrite.getIssues(GHIssueState.OPEN).stream().filter(GHIssueFilters.isRemoveNonRH()).toList();
                if (openIssues.isEmpty()) {
                    GHIssue createdIssue = orgRepo.createIssue("Remove users - Not in RH LDAP")
                        .label("admin")
                        .body(stringWriter.toString())
                        .create();

                    logger.infof("Created issue: %s", createdIssue.getUrl());
                } else {
                    logger.warnf("There are %s open issues for %s, ignoring", openIssues.size(), gitHubMemberNotFoundInLdap.getName());
                }
            }
        }
    }

    private void removeMarkedForDeletion(OrgMemberRepository members) {
        members.remove(LocalDate.now());
    }
}
