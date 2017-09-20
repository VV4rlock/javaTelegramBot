package Main;


import java.util.*;

/**
 * Created by warlock on 18.05.17.
 */
public class Vote {
    HashMap<Integer,Integer> votes;
    HashMap<Integer,Integer> amount;
    Game game;

    Vote(Set<Integer> participants,Game game){
        this.game=game;
        votes=new HashMap<>();
        amount=new HashMap<>();
    }
    Vote(){
        //this.game=game;
        votes=new HashMap<>();
        amount=new HashMap<>();
    }

    public void removeVote(Integer who){
        if (!votes.containsKey(who)) return;
        Integer oldId=votes.get(who);
        amount.put(oldId,amount.get(oldId)-1);
        votes.remove(who);
        if (amount.get(oldId)==0){
            amount.remove(oldId);
        }
    }

    public int getSize(){
        return votes.size();
    }

    public void makeVote(Integer who, Integer forWhom){
        if (votes.containsKey(who)){
            Integer oldId=votes.get(who);
            if (oldId==forWhom) return;
            amount.put(oldId,amount.get(oldId)-1);
            if (amount.get(oldId)==0){
                amount.remove(oldId);
            }
            votes.put(who,forWhom);
            if (amount.keySet().contains(forWhom)){
                amount.put(forWhom,amount.get(forWhom)+1);
            } else {
                amount.put(forWhom,1);
            }
        }
        else {
            votes.put(who, forWhom);
            if (amount.containsKey(forWhom)){
                amount.put(forWhom,amount.get(forWhom)+1);
            }else {
                amount.put(forWhom, 1);
            }
        }
    }
    public Integer getBest() {
        HashSet<Integer> out = new HashSet<>();
        int max=1;
        for (Integer i : amount.keySet()){
            Integer k=amount.get(i);
            if (k==max){
                out.add(i);
            } else if (k>max){
                max=k;
                out.clear();
                out.add(i);
            }
        }
        return randomChooice(out);
    }

    public Integer getAmountById(Integer userId){
        if (amount.containsKey(userId))
            return amount.get(userId);
        else{
            return 0;
        }
    }

    public static Integer randomChooice(Set<Integer> a) {
        //System.out.println(a.size());
        ArrayList<Integer> b=new ArrayList<>(a);
        int size=b.size();
        if (size<1){
            return -1;
        }
        if (size==1){
            return b.get(0);
        }
        return b.get(Game.rnd.nextInt(size));
    }


    public Integer getVoteById(int userId){
        return votes.get(userId);
    }
    public void drop(){
        votes.clear();
        amount.clear();
    }
}
