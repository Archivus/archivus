package archivus.user.interaction.conversation;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.bson.Document;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Conversation {
    public static final ExpiringMap<String, Conversation> conversations = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .asyncExpirationListener((k, v) -> {
                MessageChannel channel = ((Conversation) v).channel;
                channel.sendMessage("This interaction will end, as there has been no reply for 10 minutes").queue();
            })
            .build();

    ConversationAction finalAction;
    Call[] calls;
    int callIndex = 0;
    public Document tempDataDocument;
    public Document conversationDataDocument;
    public boolean hasrun = false;
    private final MessageChannel channel;

    private final Mongo mongo;

    public Conversation(ConversationAction finalAction, String userId, ArrayList<Call> calls, Mongo mongo,
                        MessageChannel channel)
        throws ConversationException{
        if(conversations.containsKey(userId))
            throw new ConversationException("User is already in a current conversation");

        this.calls = calls.toArray(new Call[0]);
        this.conversationDataDocument = new Document();
        this.tempDataDocument = this.conversationDataDocument;
        this.finalAction = finalAction;
        this.mongo = mongo;
        this.channel = channel;
        conversations.put(userId, this);
    }

    public void onResponse(GuildMessageReceivedEvent event){
        Call call = calls[callIndex];
        tempDataDocument = call.confirmation(event, conversationDataDocument, mongo);
        hasrun = true;
        if(tempDataDocument == null) return;
        conversations.replace(event.getAuthor().getId(), this);
    }

    public void conversationButton(ButtonClickEvent event, Mongo mongo){
        String compId = event.getComponentId();

        if(compId.endsWith("yes")){
            conversationDataDocument = tempDataDocument;
            callIndex++;
            if(calls.length <= callIndex) {
                finalAction.execute(event, this,
                        mongo);
                conversations.remove(event.getUser().getId());
                event.getMessage().delete().queue();
                return;
            }

            conversations.replace(event.getUser().getId(), this);

            event.getMessage().delete().queue();
            hasrun = false;
            calls[callIndex].call(event, conversationDataDocument, mongo);
        } else if(compId.endsWith("no")) {
            event.getMessage().delete().queue();
            hasrun = false;
            conversations.replace(event.getUser().getId(), this);
            calls[callIndex].call(event, conversationDataDocument, mongo);
        }
        else if(compId.endsWith("quit")) {

            event.getMessage().delete().queue();
            conversations.remove(event.getUser().getId());
        }
    }
}
