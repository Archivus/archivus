package archivus.commands;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public interface SlashCommand {
    // Execute the command with the given information
    void execute(SlashCommandEvent event, Mongo mongo);
    // Execute the ButtonAction for a button
    void executeWithButton(ButtonClickEvent event, Mongo mongo);
    // Data of the Command
    CommandData getData();
    // Help message for Command. Can contain own action row therefore ReplyAction and not MessageEmbed
    ReplyAction getHelpMessage(SlashCommandEvent event);
    // Type of Command, assign accordingly
    Type getType();
}
