package Main;

import Main.accaunt.AccauntService;
import org.glassfish.grizzly.utils.ArraySet;
import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Chat;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.exceptions.TelegramApiException;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by warlock on 28.04.17.
 */
public class Game {

    public static Random rnd=new Random();

    public AccauntService accauntService;

    public static int mafiaInt=1;
    public static int usualInt=0;

    private Chat chat;
    private int administrator;
    private boolean gameIsRunning=false;
    private int period=60000;
    public HashMap<Integer, User> participants;
    public Set<Integer> alive; //
    public HashMap<Integer,Integer> participantIdByNumber;
    public HashMap<Integer,Integer> numberById;
    public Set<Integer> mafia;
    public Vote mafiaVotes;
    public Vote usualVotes;
    public TimerTask task;
    public Timer timer;


    public boolean isTheEnd(){
        if (mafia.size()==0) return true;
        if (alive.size()>mafia.size()){
            return false;
        } else if (mafia.size()>=alive.size()-mafia.size()){
            return true;
        }
        else{
            for (Integer i : alive){
                if (!mafia.contains(participantIdByNumber.get(i))){
                    return false;
                }
            }
            return true;
        }

    }
    public int kill(Integer userId){
        if (mafia.contains(userId)) {
            alive.remove(userId);
            mafia.remove(userId);
            return mafiaInt;
        }else{
            alive.remove(userId);
            return usualInt;
        }
    }


    Pattern startPattern = Pattern.compile("^/start$");
    Pattern listPattern = Pattern.compile("^/list$");
    Pattern voteInGroupePattern = Pattern.compile("^/vote \\d+$");
    public void onUpdateReceived(Update update) {
        Message msg=update.getMessage();
        User newChatMember = msg.getNewChatMember();
        if (newChatMember!=null){
            if (isGameRunning()){
                kickAndSend(newChatMember.getId(),"Игра уже запущена. Добавление новых членов невозможно");
                return;
            } else {
                if (accauntService.userInSomeGame(newChatMember.getId())){
                    kickAndSend(newChatMember.getId(),"Добавление невозможно. "+Game.userToString(newChatMember)
                            +" уже состоит в другой игре");
                    return;
                } else {
                    addUserToGame(newChatMember);
                    sendMsg(Game.userToString(newChatMember) + " успешно добавлен в игру");
                    return;
                }
            }
        }
        User leftChatMember=msg.getLeftChatMember();
        if (leftChatMember!=null){
            if (accauntService.userInSomeGame(leftChatMember.getId())){
                deleteUserFromGame(leftChatMember.getId());
                //System.out.println(leftChatMember.getFirstName()+"ушел");
            }
        }
        //System.out.println("rem"+msg);
        if (isGameRunning() && !alive.contains(msg.getFrom().getId())){
            deleteMessage(chat.getId(),msg.getMessageId());
            return;
        }
        if (msg.hasText()){
            if (msg.getFrom().getId()==getAdminId() && startPattern.matcher(msg.getText()).matches()){
                if (!isGameRunning()) start();
                else {
                    sendMsg(" ``` Игра уже запущена ``` ");
                }
                return;
            }
            if (isGameRunning() && voteInGroupePattern.matcher(msg.getText()).matches()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 6; i < msg.getText().length(); i++) {
                    sb.append(msg.getText().charAt(i));
                }
                Integer vote = participantIdByNumber.get(Integer.valueOf(sb.toString()));
                if (isAlive(vote)) {
                    //System.out.println(vote+"проголосовал");
                    usualVotes.makeVote(msg.getFrom().getId(), vote);
                    User u = participants.get(usualVotes.getVoteById(msg.getFrom().getId()));
                    User vouter=msg.getFrom();
                    sendMessage(msg.getChatId(), " ``` "+Game.userToString(vouter)+
                            " проголосовал(а) за изгнание " + Game.userToString(u)+" ``` ");
                } else {
                    SendMessage sendMessage=new SendMessage();
                    sendMessage.setReplyToMessageId(msg.getMessageId());
                    sendMessage.setChatId(chat.getId());
                    sendMessage.setText("Такого игрока не существует или он мертв");
                    try {
                        Main.someApi.sendMsg(sendMessage);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
            if (isGameRunning() && listPattern.matcher(msg.getText()).matches()) {
                sendAliveList(chat.getId());
            }

        }
    }

    Pattern votePattern = Pattern.compile("^/vote$");
    Pattern killPattern = Pattern.compile("^/kill \\d+$");
    public void messageFromParticipant(Message msg){
        if(isGameRunning() && msg.hasText()) {
            if (isMafia(msg.getFrom().getId())) {
                if(listPattern.matcher(msg.getText()).matches()){
                    sendMafiaList(msg.getChatId());
                    return;
                }
                if (killPattern.matcher(msg.getText()).matches()){
                    StringBuilder sb= new StringBuilder();
                    for(int i=6;i<msg.getText().length();i++)
                    {
                        sb.append(msg.getText().charAt(i));
                    }
                    Integer vote=participantIdByNumber.get(Integer.valueOf(sb.toString()));
                    if (isAlive(vote)) {
                        if (mafia.contains(vote)) {
                            sendMessage(msg.getChatId(),"Вы не можете убить себя или другую мафию");
                            return;
                        }
                        mafiaVotes.makeVote(msg.getFrom().getId(), vote);
                        User u = participants.get(mafiaVotes.getVoteById(msg.getFrom().getId()));
                        sendMessage(msg.getChatId(),"Вы проголосовали за убийство "+Game.userToString(u));
                    } else {
                        sendMessage(msg.getChatId(),"Данного игрока не существует");
                    }
                    return;
                } else if (votePattern.matcher(msg.getText()).matches()){
                    User u = participants.get(mafiaVotes.getVoteById(msg.getFrom().getId()));
                    sendMessage(msg.getChatId(),"Вы проголосовали за убийство "+Game.userToString(u));
                    return;
                } else {
                    sendMessage(msg.getChatId(),"Доступные команды:\n/list\n/vote\n/kill");
                }
            } else {
                sendMessage(msg.getChatId(),"Вы не мафия((\nВы можете запросить лист выживших командой ```\"/list\"```");
            }
        }
        else {
            sendMessage(msg.getChatId(),"Игра еще не началась");
        }
    }

    private boolean isAlive(Integer userNumber){
        return alive.contains(userNumber);
    }

    private void sendMessage(Long chatId,String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableMarkdown(true);
        sendMessage.setText(text);
        try {
            Main.someApi.sendMsg(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendAliveList(Long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder result = new StringBuilder();
        result.append("Живые:\n");
        for (Integer i : alive){
            User u=participants.get(i);
            result.append(numberById.get(i)+". "+Game.userToString(u)+
                    ". "+usualVotes.getAmountById(i)+" голосов против."+"\n");
        }
        sendMessage.setText(result.toString());
        try {
            Main.someApi.sendMsg(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMafiaList(Long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        StringBuilder result = new StringBuilder();
        result.append("Живые:\n");
        for (Integer i : alive){
            if (mafia.contains(i)) continue;
            User u=participants.get(i);
            result.append(numberById.get(i)+". "+Game.userToString(u)+
                    ". "+mafiaVotes.getAmountById(i)+" голосов за убийство."+"\n");
        }
        sendMessage.setText(result.toString());
        try {
            Main.someApi.sendMsg(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static String userToString(User user){
        StringBuilder sb=new StringBuilder();
        if (user==null) return "null";
        if (user.getFirstName()!=null) sb.append(user.getFirstName());
        if (user.getLastName()!=null ) sb.append(user.getLastName());
        if (user.getUserName()!=null ) sb.append(" ("+user.getUserName()+")");
        return sb.toString();
    }

    private void kickAndSend(Integer userId,String text){
        KickChatMember kcm=new KickChatMember();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat.getId());
        sendMessage.setText(text);
        kcm.setChatId(chat.getId());
        kcm.setUserId(userId);
        try {
            Main.someApi.sendMsg(sendMessage);
            Main.someApi.kick(kcm);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chat.getId());
        sendMessage.setText(text);
        try {
            Main.someApi.sendMsg(sendMessage);
        } catch (TelegramApiException e) {
            //e.printStackTrace();
        }
    }

    public boolean isGameRunning(){
        return gameIsRunning;
    }

    private void chooseMafia(){
        int size=participants.size();
        int n=size/4;
        mafiaVotes=new Vote(mafia, this);
        while (n>0){
            int r=rnd.nextInt(size);
            for (Integer i:participants.keySet()){
                if (r<1){
                    if (!mafia.contains(i)) {
                        mafia.add(i);
                        n--;
                    }
                    break;
                }
                r--;
            }
        }
        StringBuilder sb=new StringBuilder();
        for (Integer i : mafia) {
            sb.append(" ``` "+userToString(participants.get(i))+" ``` \n");
        }
        SendMessage sendMessage=new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setText("Поздравляем, вы мафия!!!\nЧтобы запросить список живых игроков и посмотреть против кого проголосовала мафия - напишите ``` /list ``` \n"+
            "Чтобы проголосовать за убийство игрока, напишите ``` /kill НомерИгрокаВсписке ``` в этот чат\n"+
            "Будет учтен только последний голос. Чтобы посмотреть свой голос введите /vote\n"+
            "Также в составе мафии:\n"+sb.toString()+"Вы можете связаться с ними и согласовывать ваш выбор. В случае равенства выбор будет случайным.");
        for (Integer i : mafia){
            sendMessage.setChatId(Long.valueOf(i));
            //System.out.println(userToString(participants.get(i)));
            try {
                Main.someApi.sendMsg(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println("Не удалось отправить личное сообщение мафии((");
            }
        }

        sendMessage.setText("Вы обычный житель.\nЧтобы запросить список живых игроков напишите ``` /list ``` \n");
        for (Integer i : participants.keySet()){
            if (mafia.contains(i)) continue;
            sendMessage.setChatId(Long.valueOf(i));
            try {
                Main.someApi.sendMsg(sendMessage);
            } catch (TelegramApiException e) {
            }
        }
    }

    public void start(){
        //ограничить число людей 5
        if (participants.size()<4){
            sendMsg("Недостаточно людей");
            return;
        }
        sendMsg("Посмотрите личные сообщения с ботом. Проверка доступности.");
        SendMessage sm=new SendMessage();
        sm.enableMarkdown(true);
        sm.setText("Проверка досдупности личного чата");
        boolean flag=false;
        for (Integer i : participants.keySet()){
            try{
                sm.setChatId(Long.valueOf(i));
                Main.someApi.sendMsg(sm);
            } catch (TelegramApiException e) {
                flag=true;
                sendMsg(" ``` Не удалось достучаться в ЛС. "+userToString(participants.get(i))+", напишите мне в ЛС ``` ");
            }
        }
        if (flag) {
            sendMsg("Игра не создана.((Не все игроки создали чат со мной.");
            return;
        }
        chooseMafia();
        numerationOfParticipant();
        gameIsRunning=true;
        sendMsg("``` Проверьте личные сообщения, чтобы узнать свою роль.\nИгра началась!!!!\n"+
                "Вы можете использовать команду \"/list\" для просмотра списка выживших, или команду \"/vote НомерИгрока\" для голосования.");
        task=new TimerTaskA(this);
        timer=new Timer();
        timer.schedule(task,5000,period);
        sendMsg("Жители города, обессилевшие от разгула мафии, выносят решение п повесить всех мафиози до единого. В ответ мафия объявляет войну до полного уничтожения всех мирных горожан.");
    }

    public void numerationOfParticipant(){
        participantIdByNumber = new HashMap<>();
        numberById=new HashMap<>();
        alive=new HashSet<>();
        usualVotes=new Vote(participants.keySet(),this);
        int number=1;
        for (Integer i:participants.keySet()){
            alive.add(i);
            participantIdByNumber.put(number,i);
            numberById.put(i,number++);
        }
    }

    public boolean isMafia(Integer userId){
        return mafia.contains(userId);
    }

    Chat getChat(){
        return chat;
    }

    public int getAdminId(){return administrator;}

    void addUserToGame(User participant){
        participants.put(participant.getId(), participant);
        accauntService.addUserToGame(participant.getId(),chat.getId());
    }

    public void deleteUserFromGame(Integer userId){
        if (participants.containsKey(userId)) {
            if (!isGameRunning()){
                sendMsg(userToString(participants.get(userId))+" удален из игры.");
                participants.remove(userId);
                accauntService.deleteUserFromGame(userId);
                return;
            }
            int k=kill(userId);
            mafiaVotes.removeVote(userId);
            usualVotes.removeVote(userId);
            String text=" ``` "+userToString(participants.get(userId))+" сбежал(а) в другой город";
            if (k==mafiaInt) sendMsg(text+" Он был(а) мафией ``` ");
            else sendMsg(text+" Он был(а) обычным жителем ``` ");
            participants.remove(userId);
            accauntService.deleteUserFromGame(userId);
            if(isTheEnd()){
                theEnd();
            }
        }
    }

    public void theEnd(){
        if (mafia.size()!=0){
            StringBuilder sb=new StringBuilder();
            for (Integer i : mafia)
                sb.append(" ``` "+Game.userToString(participants.get(i))+" ``` \n");
            sendMsg("Победила мафия! \n ``` Выжившая мафия: ``` \n"+sb.toString());
        }else{
            sendMsg("``` Победили граждане! ```");
        }

        try {
            Main.someApi.leave(new LeaveChat().setChatId(getChat().getId()));
        } catch (TelegramApiException e) {
            //e.printStackTrace();
        }
        for (Integer i: participants.keySet()) {
            accauntService.deleteUserFromGame(i);
        }
        Main.gameService.deleteGame(getChat().getId());
        timer.cancel();
    }

    public void printGameMember(){
        System.out.println("\n\n\nAdministrator:"+Game.userToString(participants.get(administrator)));
        System.out.println("--------participant---------------------------");
        for (Map.Entry<Integer,User> i:participants.entrySet()){
            System.out.println(i);
        }
        System.out.println("\n--------alive---------------------------\n");
        for (Integer i : alive){
            System.out.println(userToString(participants.get(i)));
        }

        System.out.println("\n---------------mafia--------------------");
        for (Integer i:mafia){
            System.out.println(participants.get(i));
        }
        System.out.println("\n--------------------------------------------\n\n");
    }

    private boolean deleteMessage(long chat_id,int messageId){
        String reqUrl=Main.url+Main.botToken+"/deleteMessage?chat_id="+chat_id+"&message_id="+messageId;
        try {
            //System.out.println("try to delete");
            URL url = new URL(reqUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Game(Chat chat, User admin, AccauntService accauntSetvice){
        this.chat=chat;
        participants=new HashMap<>();
        mafia=new HashSet<>();
        administrator=admin.getId();
        this.accauntService=accauntSetvice;
        accauntService.addUserToGame(admin.getId(),chat.getId());
        participants.put(admin.getId(),admin);
    }

}
