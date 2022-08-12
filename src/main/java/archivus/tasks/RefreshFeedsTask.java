package archivus.tasks;

import archivus.mongo.Mongo;
import archivus.user.interaction.posting.Post;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.RestClient;
import net.dv8tion.jda.api.JDA;

import java.util.HashMap;
import java.util.List;

public class RefreshFeedsTask implements ScheduledTask{
    HashMap<String, String> subreddits = new HashMap<String, String>(){
        {
            put("dankmemes", "shitpost");
            put("comedyheaven", "dark");
            put("gaming", "gaming");
            put("animememes", "anime");
        }
    };

    @Override
    public void task(Mongo mongo, JDA jda) {
        RestClient restClient = new HttpRestClient();
        restClient.setUserAgent("bot/1.0 by name");

        // Connect the user
        User user = new User(restClient, "username", "password");
        try {
            user.connect();
            Submissions s = new Submissions(restClient, user);
            for(String sn : subreddits.keySet()){
                List<Submission> subms = s.ofSubreddit(subreddits.get(sn),
                        SubmissionSort.NEW,
                        -1,
                        100,
                        null,
                        null,
                        true);
                for(Submission sub : subms){
                    // Create post with Reddit data
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void print() {

    }
}
