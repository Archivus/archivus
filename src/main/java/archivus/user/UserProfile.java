package archivus.user;

import archivus.Archivus;
import archivus.commands.CommandType;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.util.ArrayList;

public class UserProfile {
    int reputation,  archives;
    String userId, tag, description, guildId;
    ArrayList<Document> topics;

    // Create new Profile
    public UserProfile(Document userDocument, MongoCollection<Document> mongo){
        this.userId = userDocument.getString("userId");
        this.guildId = userDocument.getString("guildId");
        this.tag = userDocument.getString("tag");
        this.description = userDocument.getString("desc");
        this.reputation = userDocument.getInteger("reputation");
        this.archives = userDocument.getInteger("archives");
        this.topics = new ArrayList<>(userDocument.getList("topics", Document.class));

        System.out.println(userDocument);

        mongo.insertOne(userDocument);
    }

    // Retrieve existing profile
    public UserProfile(MongoCollection<Document> collection, User user)
            throws AccountDoesNotExistException {
        Document userDocument = collection.find(new Document("userId", user.getId())).first();

        if(userDocument == null)
            throw new AccountDoesNotExistException(user.getId(), user.getAsTag());
        else {
            this.userId = userDocument.getString("userId");
            this.guildId = userDocument.getString("guildId");
            this.tag = userDocument.getString("tag");
            this.description = userDocument.getString("desc");
            this.reputation = userDocument.getInteger("reputation");
            this.archives = userDocument.getInteger("archives");
            this.topics = new ArrayList<>(userDocument.getList("topics", Document.class));
        }
    }

    // Only use if userDocument is definitely in the collection
    public UserProfile(Document userDocument){
            this.userId = userDocument.getString("userId");
            this.guildId = userDocument.getString("guildId");
            this.tag = userDocument.getString("tag");
            this.description = userDocument.getString("desc");
            this.reputation = userDocument.getInteger("reputation");
            this.archives = userDocument.getInteger("archives");
            this.topics = new ArrayList<>(userDocument.getList("topics", Document.class));

    }

    public static boolean profileExists(MongoCollection<Document> c, String userId){
        return c.find(new Document("userId", userId)).first() != null;
    }

    public void setReputation(int rep){
        this.reputation += rep;
    }

    //TODO
    public MessageEmbed userEmbed(String botUrl, String avatar){
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String str = CommandType.capitalize(this.getTopics().get(0).getString("name")) +
                ", " +
                CommandType.capitalize(this.getTopics().get(1).getString("name")) +
                ", " +
                CommandType.capitalize(this.getTopics().get(2).getString("name"));

        embedBuilder.setAuthor("View Profile", null, botUrl)
                .setTitle(this.getTag())
                .setDescription(this.getDescription() + "\n\n" +
                        "\uD83D\uDD25 **Reputation**: " + this.getReputation() + "\n" +
                        "\uD83D\uDD12 **Archives**: " + this.getArchives() + "\n" +
                        "\uD83D\uDC4D **Favourite Memes**: " + str)
                //.addField("\uD83D\uDD25 Reputation: " + this.getReputation(), "", true)
                //.addField("\uD83D\uDD12 Archives: " + this.getArchives(), "", true)
                //.addField("\uD83D\uDC4D Favourite Memes", str, false)
                .setThumbnail(avatar)
                .setColor(Archivus.colorPicker());


        return embedBuilder.build();
    }
    public void updateProfile(MongoCollection<Document> collection, InteractionHook hook) {
        Document userDocument = new Document("userId", this.userId)
                .append("desc", this.description)
                .append("tag", this.tag)
                .append("reputation", this.reputation)
                .append("archives", this.archives)
                .append("topics", this.topics);
        collection.replaceOne(new Document("userId", this.userId), userDocument);
    }

    public int getReputation() {
        return reputation;
    }


    public int getArchives() {
        return archives;
    }

    public String getUserId() {
        return userId;
    }

    public String getTag() {
        return tag;
    }

    public String getDescription() {
        return description;
    }

    public String getGuildId() {
        return guildId;
    }

    public ArrayList<Document> getTopics() {
        return topics;
    }

    public void setDescription(String str){
        this.description = str;
    }
}
