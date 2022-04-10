package archivus.mongo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.LoggerFactory;

public class Mongo {
    String secretKey;

    public Mongo(String secretKey){
        this.secretKey = secretKey;
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);
    }

    public void useClient(ClientAction action, InteractionHook hook){
        try (MongoClient mongoClient = MongoClients.create(secretKey)) {
            action.clientAction(mongoClient);
        } catch(Exception e){
            System.out.println("Error");
            hook.setEphemeral(true);
            hook.sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("Error ⛔")
                    .setDescription("There seems to be an issue on our end. ")
                    .addField("Contact some mods, we'll fix the issue ASAP.", "", false)
                    .build()).queue();
            e.printStackTrace();
        }
    }
}
