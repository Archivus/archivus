package archivus.user.interaction.conversation;

public class ConversationException extends Exception{
    private final String reason;

    public ConversationException(String reason){
        this.reason = reason;
    }

    public String getReason(){
        return reason;
    }
}
