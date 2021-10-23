package archivus.commands.generalcommands;

import archivus.commands.ButtonAction;
import archivus.commands.CommandListener;
import archivus.commands.SlashCommand;
import archivus.commands.Type;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.HashMap;

public class HelpCommand implements SlashCommand {


    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        OptionMapping commandOption = event.getOption("command");
        if(commandOption == null)
            event.replyEmbeds(this.getHelp())
                    .addActionRow(
                            Button.primary(event.getUser().getId() + ":account_help", "Account Commands"),
                            Button.primary(event.getUser().getId() + ":posting_help", "Posting Commands"),
                            Button.primary(event.getUser().getId() + ":misc_help", "Miscellaneous Commands"),
                            Button.primary(event.getUser().getId() + ":feed_help", "Feed Commands")).queue();
        else {
            String commandName = commandOption.getAsString();
            SlashCommand slashCommand = CommandListener.commands.get(commandName);
            if(slashCommand == null) {
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                hook.sendMessage("'" + commandName + "' is not a valid command, " +
                        "Try and find the command in the `/help` menu!").queue();
            } else {
                event.replyEmbeds(slashCommand.getHelp()).queue();
            }
        }
    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {
        String[] buttonId = event.getComponentId().split(":");
        if(!buttonId[0].equals(event.getUser().getId()))
            return;
        ButtonAction buttonAction = buttonHandler.get(buttonId[1]);
        buttonAction.buttonExecute(event);
    }

    @Override
    public CommandData getData() {
        return new CommandData("help", "Provides help with bot and commands")
                .addOption(OptionType.STRING, "command", "Receive help on a specific command");
    }

    //TODO
    @Override
    public MessageEmbed getHelp() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setAuthor("Archivus Commands");
        return new EmbedBuilder().build();
    }

    @Override
    public Type getType() {
        return Type.MISC;
    }

    private final HashMap<String, ButtonAction> buttonHandler = new HashMap<>(){
        {
            put("account", event -> {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Account Commands");
                for(SlashCommand c : CommandListener.commands.values()){
                    if(c.getType() == Type.ACCOUNT)
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
                    if(c.getType() == Type.FEED)
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
                    if(c.getType() == Type.POSTING)
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
                    if(c.getType() == Type.MISC)
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


