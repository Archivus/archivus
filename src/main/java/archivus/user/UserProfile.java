package archivus.user;

import archivus.mongo.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

public class UserProfile {
    int reputation;
    String userId;
    String tag;
    int followers;
    int archives;
    String[] following;
    String[] topics;

    // Retrieve existing profile
    public UserProfile(Mongo mongo, User user) throws AccountDoesNotExistException {
        MongoClient client = mongo.getClient();
        MongoDatabase db = client.getDatabase("account");
        MongoCollection<Document> collection = db.getCollection("userdata");

        if(!collection.find(new Document("userId", user.getId())).iterator().hasNext())
            throw new AccountDoesNotExistException(user.getId(), user.getAsTag());


        Document userDocument = collection.find(new Document("userId", user.getId())).first();
        this.userId = userDocument.getString("userId");
        this.tag = userDocument.getString("tag");
        this.reputation = userDocument.getInteger("reputation");
        this.followers = userDocument.getInteger("followers");
        this.archives = userDocument.getInteger("archives");
        this.following = userDocument.get("following", String[].class);
        this.topics = userDocument.get("topics", String[].class);
    }

    // New Profile
    public UserProfile(User user, MongoCollection<Document> collection){
        Document userDocument = new Document("userId", user.getId())
                                    .append("tag", user.getAsTag())
                                    .append("reputation", 0)
                                    .append("followers", 1)
                                    .append("archives", 0)
                                    .append("following", new String[]{user.getId()})
                                    .append("topics", new String[]{"archived"});
        collection.insertOne(userDocument);

        this.userId = userDocument.getString("userId");
        this.tag = userDocument.getString("tag");
        this.reputation = userDocument.getInteger("reputation");
        this.followers = userDocument.getInteger("followers");
        this.archives = userDocument.getInteger("archives");
        this.following = userDocument.get("following", String[].class);
        this.topics = userDocument.get("topics", String[].class);
    }

    //TODO
    public ReplyAction userEmbed(){
        EmbedBuilder embedBuilder = new EmbedBuilder();

        return null;
    }


    public void updateProfile(Mongo mongo){
        Document userDocument = new Document("userId", this.userId)
                .append("tag", this.tag)
                .append("reputation", this.reputation)
                .append("followers", this.followers)
                .append("archives", this.archives)
                .append("following", this.following)
                .append("topics", this.topics);

        MongoCollection<Document> collection = mongo
                .getClient()
                .getDatabase("account")
                .getCollection("userData");

        collection.replaceOne(new Document("userId", this.userId), userDocument);
    }
}
