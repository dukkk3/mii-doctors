package Models;

import com.google.gson.Gson;

public class MessageResult {
    public String Action;
    public String JsonContent;
    public String Sender;
    public String Receiver;
    public boolean IsSeccess;


    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
