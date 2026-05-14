package com.garethahealy.githubstats.commands;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.AutoComplete;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "github-stats",
    description = "GitHub helper utility",
    mixinStandardHelpOptions = true,
    subcommands = {UsersCommand.class, CommandLine.HelpCommand.class, AutoComplete.GenerateCompletion.class})
public class GitHubStatsCommand {
}
