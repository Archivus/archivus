package archivus.commands;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommand {
    void execute(SlashCommandEvent event, Mongo mongo);
    void executeWithButton(ButtonClickEvent event, Mongo mongo);
    CommandData getData();
    MessageEmbed getHelp();
    Type getType();
}
