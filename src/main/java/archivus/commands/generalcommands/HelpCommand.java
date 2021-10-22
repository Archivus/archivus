package archivus.commands.generalcommands;

import archivus.commands.ButtonAction;
import archivus.commands.CommandListener;
import archivus.commands.SlashCommand;
import archivus.commands.Type;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import java.util.HashMap;

public class HelpCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {
        OptionMapping commandOption = event.getOption("command");
        if(commandOption == null)
            event.getChannel().sendMessage(this.getHelp()).queue();
        else {
            String commandName = commandOption.getAsString();
            SlashCommand slashCommand = CommandListener.commands.get(commandName);
            if(slashCommand == null){
                InteractionHook hook = event.getHook();
                hook.setEphemeral(true);
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("'" + commandName + "' is not a valid command")
                        .setDescription("Try and find the command in the `/help` menu!")
                        .setFooter("Psst ... No one else can see this blunder!");
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

    @Override
    public Message getHelp() {
        return null;
    }

    @Override
    public Type getType() {
        return Type.MISC;
    }

    private final HashMap<String, ButtonAction> buttonHandler = new HashMap<>(){

    };


}


