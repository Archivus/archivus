package archivus.user.interaction.conversation;

import archivus.mongo.Mongo;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import org.bson.Document;


@FunctionalInterface
public interface ConversationAction {
    void execute(ButtonClickEvent event, Conversation conversation, Mongo mongo);
}
