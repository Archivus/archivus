package archivus.user;

import archivus.mongo.Mongo;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class UserProfile {
    UserProfile(User user){

    }
    public UserProfile(){}

    public ReplyAction userEmbed(){
        return null;
    }

    public void retrieveProfile(Mongo mongo, User user) throws AccountDoesNoteExistException{

    }

    public void updateProfile(Mongo mongo){

    }
}
