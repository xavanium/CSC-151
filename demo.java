import java.util.Scanner;
public class demo {

    public static int roll() {
        //roll a die, return from 1 to 6
        return 1; //for now just pretend
    }
    public static void main(String[] args) {
        Scanner k = new Scanner(System.in);
        System.out.println("Hey, what's your name?");
        String name = k.nextLine();
        System.out.println();
        System.out.println("Nice to meetcha, "+name);
        int chips = 100;
        System.out.println("Well, " + name + ", you start with " + chips + " chips.");
        System.out.println();
        System.out.println("Rolling the dice...");
        int roll = roll();
        System.out.println("You rolled a: "+roll);
    }
}