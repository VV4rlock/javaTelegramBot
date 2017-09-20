package Main.accaunt;

import org.telegram.telegrambots.api.objects.User;

import java.util.HashMap;

/**
 * Created by warlock on 18.05.17.
 */
public class AccauntService {
    HashMap<Integer,Long> userInGame; //id_игрока id_chat
    GameService gameService;

    public AccauntService(GameService gs){
        this.userInGame=new HashMap<>();
        this.gameService=gs;
    }

    public boolean userInSomeGame(Integer userId){
        return userInGame.containsKey(userId);
    }

    public Long chatIdByUserId(Integer userId){
        return userInGame.get(userId);
    }

    public void addUserToGame(Integer userId,Long chatId){
        userInGame.put(userId,chatId);
    }

    public void deleteUserFromGame(Integer userId){
        userInGame.remove(userId);
    }
}
