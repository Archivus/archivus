package archivus.commands.interfacecommands;

import archivus.commands.SlashCommand;
import archivus.commands.Type;
import archivus.mongo.Mongo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

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
    public Message getHelp() {
        return null;
    }

    @Override
    public Type getType() {
        return Type.POSTING;
    }
}
