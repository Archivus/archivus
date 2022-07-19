package archivus.commands;

import java.util.Locale;

public enum CommandType {
    ACCOUNT, POSTING, FEED, MISC;


    public static String capitalize(String s){
        s = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
