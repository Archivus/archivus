package archivus.user.interaction.feed;

import archivus.user.interaction.posting.PostTopic;

import java.util.ArrayList;

public class FeedData {
    public FeedData(ArrayList<PostTopic> topics, boolean archive) {
        this.topics = topics;
        this.archive = archive;
    }

    ArrayList<PostTopic> topics;
    boolean archive;


    public boolean addTopic(PostTopic topic){
        if(this.topics.contains(topic) || this.topics.size() == 3)
            return false;
        this.topics.add(topic);
        return true;
    }

    public boolean removeTopic(PostTopic topic){
        if(!this.topics.contains(topic) || this.topics.size() == 1)
            return false;
        this.topics.remove(topic);
        return true;
    }

    public void setArchive(boolean archive){
        this.archive = archive;
    }
}
