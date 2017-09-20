package Main;

import Main.accaunt.AccauntService;
import Main.accaunt.GameService;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.User;

import java.util.HashSet;

/**
 * Created by warlock on 19.05.17.
 */
public class test {
    public static void main(String[] args){
        //Game game=new Game(new Chat(),new User(),new AccauntService(new GameService()));
        Vote vote=new Vote();
        //vote.makeVote(1,1);
        //vote.makeVote(2,1);
        boolean flag=true;
        if (flag){
            try {
                System.out.println(vote.getBest());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            for (int i = 0; i < 500; i++) {
                try {
                    System.out.println(vote.getBest());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
