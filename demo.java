import java.util.Scanner;
import java.util.Random;
public class demo {

    public static int roll() {
        //roll a die, return from 1 to 6
        Random roller = new Random();
        int roll = roller.nextInt(5) + 1; //0-5 becomes 1-6
        return roll;
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
        //loop
        for (int i=0; i<20; i++){
        System.out.println("Rolling the dice...");
        int roll = roll() + roll();
        System.out.println("You rolled a: "+roll); }
        //end of program, closing scanner
        k.close();
    }
}