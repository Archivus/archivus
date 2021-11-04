package archivus.user.interaction;

import java.util.ArrayList;

public class Post {
    String imageUrl, title, userId, postId;
    int vote;
    long timeUploaded;
    String[] tags;
    ArrayList<String> viewedUsers;

    Post(String imageUrl, String title, String userId, String ... tags) throws NSFWException {
        // Run check on imageURL for NSFW content
        if(this.NSFWCheck(imageUrl) > 0.9)
            throw new NSFWException(imageUrl, userId);
        this.imageUrl = imageUrl;
        this.title = title;
        this.vote = 0;
        this.timeUploaded = 0;
        this.userId = userId;
        this.tags = tags;
        this.viewedUsers = new ArrayList<>();
        this.postId = this.generatePostId();
    }

    // Check for NSFW content, returns a value of 0 - 1 depending on the chance
    // the image contains NSFW content
    private double NSFWCheck(String imageUrl) {
        return 0.0;
    }

    //Generate a custom post ID
    private String generatePostId(){
        return null;
    }

    // TODO
    public void listPost(){

    }

    // TODO
    public void updatePost(){

    }

    // TODO Other methods and fields
}
