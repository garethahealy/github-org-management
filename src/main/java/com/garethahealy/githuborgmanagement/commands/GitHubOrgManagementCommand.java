package com.garethahealy.githuborgmanagement.commands;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.AutoComplete;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "github-org-management",
    description = "GitHub helper utility",
    mixinStandardHelpOptions = true,
    subcommands = {UsersCommand.class, CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class})
public class GitHubOrgManagementCommand {
}
