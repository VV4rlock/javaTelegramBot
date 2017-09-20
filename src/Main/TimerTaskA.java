package Main;

import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.HashSet;
import java.util.TimerTask;

/**
 * Created by warlock on 16.05.17.
 */
public class TimerTaskA extends TimerTask {

    Game game;
    int time=5;
    TimerTaskA(Game game){
        this.game=game;
    }
    @Override
    public void run() {
        //game.printGameMember();
        if (time==0){
            time=5;
            String text="``` Наступило утро! Сводка новостей за ночь: ```";
            Integer mafiavote;
            if (game.mafiaVotes.getSize()==0){
                HashSet<Integer> e=new HashSet<>();
                for(Integer i : game.alive){
                    if (!game.mafia.contains(i)){
                        e.add(i);
                    }
                }
                mafiavote=Vote.randomChooice(e);
            } else {
                mafiavote=game.mafiaVotes.getBest();
            }
            //System.out.println(game.participants.get(mafiavote));
            if (mafiavote==null) {
                System.out.println("Не удалось провести мафиозное голосование");
                game.printGameMember();
            }
            game.usualVotes.removeVote(mafiavote);
            int k=game.kill(mafiavote);
            text+=" ``` "+game.userToString(game.participants.get(mafiavote))+" был убит мафией ``` ";
            game.sendMsg(text);
            game.mafiaVotes.drop();

        } else if (time == 1){
            Integer usualVote;
            if (game.usualVotes.getSize()==0){
                usualVote=Vote.randomChooice(game.alive);
            } else {
                usualVote=game.usualVotes.getBest();
            }
            //System.out.println(game.participants.get(usualVote) + " "+usualVote==null);
            if (usualVote==null) {
                System.out.println("Не удалось провести мирное голосование ");
                game.printGameMember();
            }
            game.mafiaVotes.removeVote(usualVote);
            String text="``` Вечер.На собрании жители города устроили самосуд над "
                    +game.userToString(game.participants.get(usualVote))+".";
            int k=game.kill(usualVote);
            if (k==Game.mafiaInt) text+="У него дома нашли оружие, кокаин и огромное количество налички. Все указывает на то, что он был мафией. ```";
            else text+="О нем ходили разные слухи, но он никогда не убивал людей. Жители Еще долго будут помнить эту расправу над невиновным. ```";
            game.sendMsg(text);
            game.usualVotes.drop();
        }
        if (game.isTheEnd()){
            game.theEnd();
        }
        if (time>1)
            game.sendMsg("``` До вечерней расправы осталось " + (time-1) + "минут.\nСделайте свой выбор. ```");
        else if (time==1) {
            game.sendMsg("``` Наступила ночь. На улицах города стало не безопасно. До утра осталось " + (time) + "минут. ```");
        }
        time--;
    }
}
