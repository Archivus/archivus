package archivus.user.interaction.posting;

import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

public class Post {
    String imageUrl, title, userId, postId;
    int vote;
    long timeUploaded;
    String[] tags;
    ArrayList<String> viewedUsers;

    Post(String imageUrl, String title, String userId, String ... tags){
        this.imageUrl = imageUrl;
        this.title = title;
        this.vote = 0;
        this.timeUploaded = 0;
        this.userId = userId;
        this.tags = tags;
        this.viewedUsers = new ArrayList<>();
        this.postId = this.generatePostId();

        Logger logger = Logger.getAnonymousLogger();
        logger.info("Post Created -- userID: " + userId +
                ", postId: " + postId + ", title: " + title + ", tags: " + Arrays.toString(tags) + ", imageURL: " +
                imageUrl);
    }

    //Generate a custom post ID
    private String generatePostId(){
        StringBuilder stringBuilder = new StringBuilder("@");
        //Produces approx. 7 billion possibilities O(5)
        while(stringBuilder.length() != 5)
            stringBuilder.append((char) (126 - new Random().nextInt(94)));


        return stringBuilder.toString();
    }

    //Get Post

    // TODO
    public void listPost(MongoCollection<Document> collection){
        Document doc = new Document("imageUrl", this.imageUrl)
                .append("title", this.title)
                .append("vote", this.vote)
                .append("timeUploaded", this.timeUploaded)
                .append("userId", this.userId)
                .append("tags", this.tags)
                .append("viewedUsers", this.viewedUsers)
                .append("postId", this.postId);

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
                .append("postId", this.postId);

        collection.replaceOne(new Document("postId", this.userId), doc);
    }
}
