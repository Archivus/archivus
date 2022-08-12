package archivus.user.interaction.posting;

import org.bson.Document;

import java.util.EnumSet;
import java.util.Optional;

public enum PostTopic {
    SHITPOST("21st Century, Gen Z Humour. You know exactly what I mean."),
    DARK("For those edge-lords out there. No NSFW"),
    GAMING("All things Gaming. NO AMOGUS."),
    ANIME("Finally, you can post Anime memes without being called a weeb. You freaking weeb.");

    public final String data;

    PostTopic(String data){
        this.data = data;
    }

    public Document toDoc() {
        return new Document("name", this.toString())
                .append("data", this.data);

    }

    public static PostTopic toTopicByDoc(Document doc){
        Optional<PostTopic> topic = EnumSet.allOf(PostTopic.class)
                .stream().filter(p -> p.data.equals(doc.getString("data"))).findFirst();

        return topic.orElse(null);
    }

    public static PostTopic toTopicByString(String str){
        Optional<PostTopic> topic = EnumSet.allOf(PostTopic.class)
                .stream().filter(p -> p.toString().equalsIgnoreCase(str)).findFirst();

        return topic.orElse(null);
    }
}
