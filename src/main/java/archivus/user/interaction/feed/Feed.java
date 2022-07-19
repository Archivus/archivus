package archivus.user.interaction.feed;

import archivus.mongo.Mongo;
import archivus.user.interaction.posting.Post;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Feed {


    FeedData data;
    final String creator;
    public int downvotes;
    public int upvotes;
    public ArrayList<String> members;
    public Post currentPost;

    public Feed(FeedData data, String creator, MongoCollection<Document> collection) {
        this.data = data;
        this.creator = creator;
        this.members = new ArrayList<>();
        this.members.add(creator);
        this.downvotes = 0;
        this.upvotes = 0;

        Document doc = collection.find(Sorts.ascending("vote")).first();
        assert doc != null;
        this.currentPost = new Post(doc);
    }


    public void setData(FeedData data) {
        this.data = data;
    }

    public boolean addMember(String member){
        if(members.contains(member) || members.size() == 4)
            return false;
        this.members.add(member);
        return true;
    }

    public boolean removeMember(String member){
        if(!members.contains(member) || members.size() == 1)
            return false;
        this.members.remove(member);
        return true;
    }

    public void queryPost(Mongo mongo){
        // Vene Vidi Vici 😈
        mongo.useClient(client -> {
            MongoCollection<Document> collection
                    = client.getDatabase("post")
                    .getCollection("availableposts");
            AtomicInteger count = new AtomicInteger();
            collection.find().sort(Sorts.ascending("vote"))
                    .filter(Filters.lt("vote", this.currentPost.vote))
                    .filter(Filters.all("tags", this.data.topics)).batchSize(10)
                    .forEach(doc -> {
                        count.getAndIncrement();
                        int viewers = 0;
                        for(String member : this.members)
                            if (doc.getList("viewedUsers", String.class).contains(member))
                                viewers++;

                        if(viewers > (members.size()/4))
                            if(count.get() > 9)
                                this.currentPost = new Post(doc);
                        else this.currentPost = new Post(doc);
                    });
        });
    }

}
