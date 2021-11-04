package archivus.user.interaction;

public class NSFWException extends Exception{
    String imageInQuestion;
    String userID;

    public NSFWException(String imageInQuestion, String userID){
        this.imageInQuestion = imageInQuestion;
        this.userID = userID;
    }
}
