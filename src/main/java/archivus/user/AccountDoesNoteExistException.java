package archivus.user;

public class AccountDoesNoteExistException extends Exception{
    String userID;

    AccountDoesNoteExistException(String userId){
        this.userID = userId;
    }
}
