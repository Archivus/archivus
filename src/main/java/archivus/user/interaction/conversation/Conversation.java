package archivus.user.interaction.conversation;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.jodah.expiringmap.ExpiringMap;
import org.bson.Document;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Conversation {
    public static final ExpiringMap<String, Conversation> conversations = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .build();

    ConversationAction finalAction;
    Call[] calls;
    int callIndex = 0;
    public Document tempDoc;
    public Document doc;

    private final Mongo mongo;

    public Conversation(ConversationAction finalAction, String userId, ArrayList<Call> calls, Mongo mongo)
        throws ConversationException{
        if(conversations.containsKey(userId))
            throw new ConversationException("User is already in a current conversation");

        this.calls = calls.toArray(new Call[0]);
        this.doc = new Document();
        this.tempDoc = this.doc;
        this.finalAction = finalAction;
        this.mongo = mongo;

        conversations.put(userId, this);
    }

    public void onResponse(GuildMessageReceivedEvent event){
        Call call = calls[callIndex];
        tempDoc = call.confirmation(event, doc, mongo);
        if(tempDoc == null) return;
        conversations.replace(event.getAuthor().getId(), this);
    }

    public void conversationButton(ButtonClickEvent event, Mongo mongo){
        String compId = event.getComponentId();

        if(compId.endsWith("yes")){
            doc = tempDoc;
            callIndex++;
            if(calls.length <= callIndex) {
                mongo.useClient(client ->
                        finalAction.execute(event, this, client
                        .getDatabase("account")
                        .getCollection("userdata"))
                        , event.getHook());
                conversations.remove(event.getUser().getId());
                event.getMessage().delete().queue();
                return;
            }

            conversations.replace(event.getUser().getId(), this);

            event.getMessage().delete().queue();
            calls[callIndex].call(event, doc, mongo);
        } else if(compId.endsWith("no")) {
            event.getMessage().delete().queue();
            calls[callIndex].call(event, doc, mongo);
        }
        else if(compId.endsWith("quit")) {

            event.getMessage().delete().queue();
            conversations.remove(event.getUser().getId());
        }
    }
}
