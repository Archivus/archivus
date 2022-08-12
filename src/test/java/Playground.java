import java.util.Arrays;
import java.util.Scanner;

public class Playground {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println(encrypt(input.next()));
    }

    private static String encrypt(String input){
        StringBuilder strB = new StringBuilder();
        for(char c : input.toCharArray())
            strB.append((char) (((int) c) + 3));
        return strB.toString();
    }

}
