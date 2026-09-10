package com.garethahealy.githuborgmanagement.commands;

import com.garethahealy.githuborgmanagement.commands.users.CollectMembersFromRedHatLdapCommand;
import com.garethahealy.githuborgmanagement.commands.users.CreateWhoAreYouIssueCommand;
import com.garethahealy.githuborgmanagement.commands.users.ListenToIssuesCommand;
import com.garethahealy.githuborgmanagement.commands.users.ListenToPullRequestsCommand;
import picocli.CommandLine;

@CommandLine.Command(
    name = "users",
    description = "Users operations",
    subcommands = {CollectMembersFromRedHatLdapCommand.class,
        CreateWhoAreYouIssueCommand.class,
        ListenToIssuesCommand.class,
        ListenToPullRequestsCommand.class, CommandLine.HelpCommand.class})
public class UsersCommand {
}
