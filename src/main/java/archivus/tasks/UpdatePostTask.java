package archivus.tasks;

import archivus.mongo.Mongo;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.JDA;
import org.bson.Document;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class UpdatePostTask implements ScheduledTask{
    int deleted;

    @Override
    public void task(Mongo mongo, JDA  jda) {
        mongo.useClient(action -> {
            MongoCollection<Document> posts = action.getDatabase("post")
                    .getCollection("availableposts");

            posts.find().forEach(a -> {
               if((System.currentTimeMillis() - a.getLong("timeUploaded"))
                       > TimeUnit.DAYS.toMillis(1)) {
                   posts.deleteOne(a);
                   deleted++;
               }
            });
        });
    }

    @Override
    public void print() {
        Logger logger = Logger.getAnonymousLogger();
        logger.info("Posts deleted: " + deleted);
    }
}
