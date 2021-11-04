package archivus.commands.generalcommands;

import archivus.commands.ButtonAction;
import archivus.commands.CommandListener;
import archivus.commands.SlashCommand;
import archivus.commands.CommandType;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.HashMap;

public class HelpCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        // Get the command chosen, if it is not available, return the help message for the bot.
        OptionMapping commandOption = event.getOption("command");
        if(commandOption == null)
            this.getHelpMessage(event).queue();
        else {
            // Convert Command to normal Slash Command
            String commandName = commandOption.getAsString();
            SlashCommand slashCommand = CommandListener.commands.get(commandName);

            // Possibility of command being invalid so ephemerally message user about the incorrect command
            if(slashCommand == null) {
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                hook.sendMessage("'" + commandName + "' is not a valid command, " +
                        "Try and find the command in the `/help` menu!").queue();
            } else {
                slashCommand.getHelpMessage(event).queue();
            }
        }
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        // Verify the interaction is coming from the user who ran the help command
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;

        // Get and execute the Command for the corresponding button
        ButtonAction buttonAction = buttonHandler.get(buttonId[1].substring(0, buttonId[1].indexOf('_')));
        buttonAction.buttonExecute(event);
    }

    @Override
    public CommandData getData() {
        return new CommandData("help", "Provides help with bot and commands")
                .addOption(OptionType.STRING, "command", "Receive help on a specific command");
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Archivus Commands", event.getJDA().getSelfUser().getAvatarUrl());
        embed.setDescription("Click on a command topic below or enter `/help COMMAND_NAME` for help on a specific " +
                "command.");
        embed.addField("Useful Links",
                "[Top.gg Page](INSERT PAGE)\n" +
                        "[Website](INSERT SITE)\n" +
                        "[Support Server](INSERT SERVER)\n" +
                        "[Patreon](INSERT PATREON)", false);
        return event.replyEmbeds(embed.build()) .addActionRow(
                Button.primary(event.getUser().getId() + ":account_help", "Account Commands")
                        .withEmoji(Emoji.fromUnicode("U+1F9D1")),
                Button.primary(event.getUser().getId() + ":posting_help", "Posting Commands")
                        .withEmoji(Emoji.fromUnicode("U+1F4EC")),
                Button.primary(event.getUser().getId() + ":misc_help", "Miscellaneous Commands")
                        .withEmoji(Emoji.fromUnicode("U+1F5D1")),
                Button.primary(event.getUser().getId() + ":feed_help", "Feed Commands")
                        .withEmoji(Emoji.fromUnicode("U+1F4DC")));
    }

    @Override
    public CommandType getType() {
        return CommandType.MISC;
    }

    //Map for all actions buttons can do
    private final HashMap<String, ButtonAction> buttonHandler = new HashMap<>(){
        {
            put("account", event -> {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Account Commands");
                for(SlashCommand c : CommandListener.commands.values()){
                    if(c.getType() == CommandType.ACCOUNT)
                        embed.addField(c.getData().getName(),
                                c.getData().getDescription() +
                                        "\nEnter `/help " + c.getData().getName() + "` for help",
                                true);
                }
                embed.setFooter("Enter `/help COMMAND_NAME` for help on any command!");
                event.replyEmbeds(embed.build()).queue();
            });
            put("feed", event -> {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Feed Commands");
                for(SlashCommand c : CommandListener.commands.values()){
                    if(c.getType() == CommandType.FEED)
                        embed.addField(c.getData().getName(),
                                c.getData().getDescription() +
                                        "\nEnter `/help " + c.getData().getName() + "` for help",
                                true);
                }
                embed.setFooter("Enter `/help COMMAND_NAME` for help on any command!");
                event.replyEmbeds(embed.build()).queue();
            });
            put("posting", event -> {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Posting Commands");
                for(SlashCommand c : CommandListener.commands.values()){
                    if(c.getType() == CommandType.POSTING)
                        embed.addField(c.getData().getName(),
                                c.getData().getDescription() +
                                        "\nEnter `/help " + c.getData().getName() + "` for help",
                                true);
                }
                embed.setFooter("Enter `/help COMMAND_NAME` for help on any command!");
                event.replyEmbeds(embed.build()).queue();
            });
            put("misc", event -> {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Miscellaneous Commands");
                for(SlashCommand c : CommandListener.commands.values()){
                    if(c.getType() == CommandType.MISC)
                        embed.addField(c.getData().getName(),
                                c.getData().getDescription() +
                                        "\nEnter `/help " + c.getData().getName() + "` for help",
                                true);
                }
                embed.setFooter("Enter `/help COMMAND_NAME` for help on any command!");
                event.replyEmbeds(embed.build()).queue();
            });
        }
    };
}


