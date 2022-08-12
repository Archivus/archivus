package archivus.user.interaction.posting;

import archivus.Archivus;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class Post {
    public String imageUrl, title, userId, postId;
    public int vote, reports;
    public long timeUploaded;
    public String memeType, userTag;
    public List<Document> tags;
    public ArrayList<String> viewedUsers;


    public Post(String imageUrl, String title, String userId, List<Document> tags, String userTag){
        this.imageUrl = imageUrl;
        this.userTag = userTag;
        this.title = title;
        this.vote = 0;
        this.timeUploaded = System.currentTimeMillis();
        this.userId = userId;
        this.tags = tags;
        this.viewedUsers = new ArrayList<>();
        this.postId = this.generatePostId();
        this.memeType = "Non-Archived Memes";
        this.reports = 0;

        Logger logger = Logger.getAnonymousLogger();
        logger.info("Post Created -- userID: " + userId +
                ", postId: " + postId + ", title: " + title + ", tags: " + tags + ", imageURL: " +
                imageUrl);
    }

    public Post(Document doc){
        this.imageUrl = doc.getString("imageUrl");
        this.title = doc.getString("title");
        this.vote = doc.getInteger("vote");
        this.timeUploaded = doc.getLong("timeUploaded");
        this.userId = doc.getString("userId");
        this.userTag = doc.getString("userTag");
        this.tags = doc.getList("tags", Document.class);
        this.viewedUsers = new ArrayList<>(doc.getList("viewedUsers", String.class));
        this.postId = doc.getString("postId");
        this.memeType = doc.getString("memeType");
    }

    //Generate a custom post ID
    private String generatePostId(){
        StringBuilder stringBuilder = new StringBuilder("@");
        //Produces approx. 7 billion possibilities O(5)
        while(stringBuilder.length() != 6) {
            int x = (int)(Math.random() * ((90 - 65) + 1)) + 65;
            int y = (int)(Math.random() * ((122 - 97) + 1)) + 97;
            int v = new Random().nextBoolean() ? x : y;
            stringBuilder.append((char) v);
        }


        return stringBuilder.toString();
    }


    //Get Post

    public EmbedBuilder postEmbed(String userId, boolean addView){
        EmbedBuilder embed = new EmbedBuilder();
        if(!this.viewedUsers.contains(userId) && addView){
            this.viewedUsers.add(userId);
        }

        StringBuilder str = new StringBuilder("Tag(s): ");
        for(int i = 0; i < tags.size(); i++){
            PostTopic topic = PostTopic.toTopicByDoc(tags.get(i));
            str.append(topic.toString().charAt(0))
                    .append(topic.toString().substring(1).toLowerCase());
            if(i != tags.size() -1) str.append(", ");
        }
        embed.setTitle(this.title)
                .setDescription(str)
                .addField("Votes", String.valueOf(this.vote), true)
                .addField("Views", String.valueOf(this.viewedUsers.size()), true)
                .addField("Posted By", this.userTag, true)
                .setImage(this.imageUrl)
                .setColor(Archivus.colorPicker())
                .setFooter("Post ID: " + this.postId);

        try(InputStream input = Files.newInputStream(Paths.get("src/main/resources/archivus_links.properties"))){
            Properties prop = new Properties();
            prop.load(input);
            embed.setAuthor(memeType, null, prop.getProperty("archivus.wojak"));
        } catch (IOException ex) {
            embed.setAuthor("Confirm Followers");
            System.err.println("There's an issue with the path given to or data in the " +
                    "archivus_links.properties file");
            ex.printStackTrace();
        }



        return embed;
    }

    // TODO
    public void listPost(MongoCollection<Document> collection){
        Document doc = new Document("imageUrl", this.imageUrl)
                .append("title", this.title)
                .append("vote", this.vote)
                .append("timeUploaded", this.timeUploaded)
                .append("userId", this.userId)
                .append("tags", this.tags)
                .append("viewedUsers", this.viewedUsers)
                .append("postId", this.postId)
                .append("userTag", this.userTag)
                .append("memeType ", this.memeType);

        collection.insertOne(doc);
    }

    // TODO
    public void updatePost(MongoCollection<Document> collection){
        Document doc = new Document("imageUrl", this.imageUrl)
                .append("title", this.title)
                .append("vote", this.vote)
                .append("timeUploaded", this.timeUploaded)
                .append("userId", this.userId)
                .append("tags", this.tags)
                .append("viewedUsers", this.viewedUsers)
                .append("postId", this.postId)
                .append("userTag", this.userTag)
                .append("memeType ", this.memeType);

        collection.replaceOne(new Document("postId", this.postId), doc);
    }
}
