import java.util.Scanner;
public class demo {
    public static void main(String[] args) {
        Scanner k = new Scanner(System.in);
        System.out.println("Hey, what's your name?");
        String name = k.nextLine();
        System.out.println();
        System.out.println("Nice to meetcha, "+name);
    }
}