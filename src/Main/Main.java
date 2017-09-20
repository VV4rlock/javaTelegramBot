package Main;

import Main.accaunt.AccauntService;
import Main.accaunt.GameService;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.groupadministration.*;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.List;
import java.util.logging.Logger;

public class Main extends TelegramLongPollingBot{

    static String url="https://api.telegram.org/bot";
    static String botUsername="the_best_mafia_bot";
    static String botToken="";
    static int botId=3;
    public static SomeApi someApi;

    public AccauntService accauntService;
    public static GameService gameService;


    private String mail="example@mail.ru";
    private String manual="Бот для игры в мафию. Для создания игры:\n 1.Создайте группу с ботом (только вы и бот должны быть в группе)\n"+
            "2.Уберите галочку в настройках группы \"All members are administrators\".(Manage administrators\n)"
            +"3.Добавьте меня в администраторы (там же).\n"+
            "4.напишите что-нибудь в чат и получите сообщение о том что игра создана.\n"+
            "5.Добавьте участников игры.\n6.введите \"/start\"\n7.Наслаждайтесь игрой.\n\nЕсли столкнулись с багом или есть предложения по улучшению, пишите сюда: "+mail;




     //1min=60000ms
    @Override
    public void onUpdateReceived(Update update) {
        //System.out.println(update.getMessage());
        Chat chat=null;
        try {
            chat = update.getMessage().getChat();
        } catch (NullPointerException e){
            System.out.print("Не удалось получить чат.");
            System.out.println(update);
            return;
        }

        if (chat.isGroupChat()){

            Message msg = update.getMessage();

            if (gameService.isGameExist(chat.getId())){
                gameService.gameByChatId(chat.getId()).onUpdateReceived(update);
            } else {
                User admin = msg.getFrom();
                if (accauntService.userInSomeGame(admin.getId())){
                    SendMessage backMessage =new SendMessage();
                    backMessage.setChatId(chat.getId());
                    backMessage.setText("Вы уже находитесь в другой игре.\nЗавершите ее прежде чем создавать новую.");
                    try {
                        sendMessage(backMessage);
                        leaveChat(new LeaveChat().setChatId(chat.getId()));
                    } catch (TelegramApiException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }
                try {
                    if (getChatMemberCount(new GetChatMemberCount().setChatId(chat.getId())) != 2) {
                        SendMessage backMessage =new SendMessage();
                        backMessage.setChatId(chat.getId());
                        backMessage.setText("Игра создана не правильно! Прочитайте правила создание игры еще раз\n\n"+manual);
                        try {
                            sendMessage(backMessage);
                            //leaveChat(new LeaveChat().setChatId(chat.getId()));
                        } catch (TelegramApiException e1) {
                            e1.printStackTrace();
                        }
                        return;
                    }
                } catch (TelegramApiException e) {
                    SendMessage backMessage =new SendMessage();
                    backMessage.setChatId(chat.getId());
                    backMessage.setText("что то пошло не так. пересоздвайте игру");
                    try {
                        sendMessage(backMessage);
                        leaveChat(new LeaveChat().setChatId(chat.getId()));
                    } catch (TelegramApiException e1) {
                        //e1.printStackTrace();
                    }
                    return;
                }
                if (chat.getAllMembersAreAdministrators()){
                    SendMessage backMessage =new SendMessage();
                    backMessage.setChatId(chat.getId());
                    backMessage.setText("Уберите в чате галочку \"все члены игры -администраторы\".И сделайте меня администратором.После этого напишите что-нибудь.");
                    try {
                        sendMessage(backMessage);
                    } catch (TelegramApiException e) {
                    }
                    return;
                }

                try{
                    GetChatAdministrators cm =new GetChatAdministrators();
                    cm.setChatId(msg.getChatId());
                    List<ChatMember> e=getChatAdministrators(cm);
                    boolean flag=true;
                    for (ChatMember i : e){
                        if(i.getUser().getId()==botId){
                            flag=false;
                        }
                    }
                    if (flag){
                        SendMessage backMessage =new SendMessage();
                        backMessage.setChatId(chat.getId());
                        backMessage.setText("Я не администратор. Сделайте меня администратором и напишите что-нибудь в этот чат.");
                        try {
                            sendMessage(backMessage);
                        } catch (TelegramApiException l) {
                        }
                        return;
                    }
                } catch (TelegramApiException e) {
                    SendMessage backMessage =new SendMessage();
                    backMessage.setChatId(chat.getId());
                    backMessage.setText("Не удалось получить мой статус");
                    try {
                        sendMessage(backMessage);
                    } catch (TelegramApiException l) {
                    }
                    return;
                }

                Logger.getGlobal().info("игра создана"+chat.getId());
                Game game = new Game(chat,admin,accauntService);
                gameService.addGame(game.getChat().getId(),game);
                SendMessage backMessage =new SendMessage();
                backMessage.setChatId(chat.getId());
                backMessage.setText("Игра создана!\n"+
                        "Администатор игры: "+Game.userToString(admin));
                try {
                    sendMessage(backMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if(chat.isUserChat()) {
            Message msg=update.getMessage();
            if (accauntService.userInSomeGame(msg.getFrom().getId())){
                Game game=gameService.gameByChatId(accauntService.chatIdByUserId(msg.getFrom().getId()));
                game.messageFromParticipant(msg);

            } else {
                SendMessage response=new SendMessage();
                response.setChatId(chat.getId());
                response.setText(manual);
                try {
                    sendMessage(response);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else {
            SendMessage response=new SendMessage();
            response.setChatId(chat.getId());
            response.setText(manual);
            try {
                sendMessage(response);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }


    }



    class SomeApi{
        void sendMsg(SendMessage mes) throws TelegramApiException {
            sendMessage(mes);
        }
        void kick(KickChatMember k) throws TelegramApiException {
            kickMember(k);
        }

        void leave(LeaveChat lc) throws TelegramApiException {
            leaveChat(lc);
        }
    }



    public static void main(String[] args) {

        //инициализируем
        ApiContextInitializer.init();
        //создаем и регистрируем
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

        try {
            telegramBotsApi.registerBot(new Main());
        } catch (TelegramApiRequestException e) {}

    }

    Main(){
        gameService=new GameService();
        accauntService=new AccauntService(gameService);
        someApi=new SomeApi();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
