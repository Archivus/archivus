package archivus.commands;

public class AccountNotFoundException  extends Exception{
    String reason;

    AccountNotFoundException(String reason){
        this.reason = reason;
    }
}
