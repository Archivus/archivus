package archivus.commands.interfacecommands;

import archivus.commands.SlashCommand;
import archivus.commands.CommandType;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class PostCommand implements SlashCommand {
    @Override
    public void execute(SlashCommandEvent event, Mongo mongo) {

    }

    @Override
    public void executeWithButton(ButtonClickEvent event, Mongo mongo) {

    }

    @Override
    public CommandData getData() {
        return null;
    }

    @Override
    public ReplyAction getHelpMessage(SlashCommandEvent event) {
        return null;
    }

    @Override
    public CommandType getType() {
        return CommandType.POSTING;
    }
}
