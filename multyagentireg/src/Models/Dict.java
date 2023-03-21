package Models;

import java.util.ArrayList;

public class Dict {
    public ArrayList<String> Keys = new ArrayList<>();
    public ArrayList<Integer> Values = new ArrayList<>();
    public int Count = 0;

    public void put(String key, Integer value){
        Keys.add(key);
        Values.add(value);
        Count++;
    }

    public Integer GetValue(String key){
        for(int i = 0; i < Keys.size(); i++){
            if (Keys.get(i).compareToIgnoreCase(key) == 0){
                return Values.get(i);
            }
        }
        System.out.println("ВЕРНУЛ NULL");
        return null;
    }

    public String GetByVal(int val){
        for(int i = 0; i < Keys.size(); i++){
            if (Values.get(i) == val){
                return Keys.get(i);
            }
        }
        System.out.println("ВЕРНУЛ NULL");
        return null;
    }
}
