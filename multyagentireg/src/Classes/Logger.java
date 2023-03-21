package Classes;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class Logger {
    public static String FileName = String.format("log%s.txt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE).toString());

    public static void WriteLog(String log){
        WriteExeption(log, null);
    }
    public static void WriteLog(){
        ArrayList<String> msg = new ArrayList<>();
        msg.add("\n");
        WriteFile(new ArrayList<>());
    }

    public static void WriteExeption(String message, Exception e){
        boolean isEx = e != null;
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_TIME).toString();
        message = (isEx ? "ERROR:" : "") + time + ":\t" + message;
        ArrayList<String> result = new ArrayList<>();
        result.add(message);
        result.add("");

        if (isEx){
            result.add(e.getMessage());
            result.add(e.getStackTrace().toString());
        }

        WriteFile(result);
    }

    private static void WriteFile(ArrayList<String> content){
        Charset utf8 = StandardCharsets.UTF_8;
        try {
            Files.write(Paths.get(FileName),content, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void DeleteOutput(){
        try {
            Files.delete(Paths.get("output.txt"));
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void WriteOutput(String message){
        Charset utf8 = StandardCharsets.UTF_8;
        ArrayList<String> msg = new ArrayList<>();
        msg.add(message);
        try {
            Files.write(Paths.get("output.txt"),msg, utf8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void WriteOutput(){
        WriteOutput("");
    }

    public static void DrawLine(){
        var line = new ArrayList<String>();
        line.add("____________________________________________________________________________________");
        WriteFile(line);
    }
}
