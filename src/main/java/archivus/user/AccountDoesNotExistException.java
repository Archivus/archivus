package archivus.user;

public class AccountDoesNotExistException extends Exception{
    String userID;
    String tag;

    AccountDoesNotExistException(String userId, String tag){
        this.userID = userId;
        this.tag = tag;
    }

    public String getUserID() {
        return userID;
    }

    public String getTag() {
        return tag;
    }
}
