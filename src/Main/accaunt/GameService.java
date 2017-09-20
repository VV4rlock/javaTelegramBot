package Main.accaunt;

import Main.Game;

import java.util.HashMap;

/**
 * Created by warlock on 18.05.17.
 */
public class GameService {
    HashMap<Long, Game> gameByChatId;

    public GameService(){
        this.gameByChatId=new HashMap<>();
    }

    public boolean isGameExist(Long chatId){
        return gameByChatId.containsKey(chatId);
    }

    public Game gameByChatId(Long chatId){
        return gameByChatId.get(chatId);
    }

    public void addGame(Long chatId, Game game){
        gameByChatId.put(chatId,game);
    }

    public void deleteGame(Long chatId){
        gameByChatId.remove(chatId);
    }
}
