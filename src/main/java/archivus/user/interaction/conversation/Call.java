package archivus.user.interaction.conversation;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bson.Document;

public interface Call {
    void call(ButtonClickEvent event, Document doc, Mongo mongo);
    Document confirmation(GuildMessageReceivedEvent event, Document doc, Mongo mongo);
}
