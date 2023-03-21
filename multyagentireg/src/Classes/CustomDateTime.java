package Classes;

public class CustomDateTime {
    public int Year = 0;
    public int Month = 0;
    public int Day = 0;
    public int Hour = 0;
    public int Minutes = 0;

    public CustomDateTime(int year, int month, int day, int hour, int minutes){
        Year = year;
        Month = month;
        Day = day;
        Hour = hour;
        Minutes = minutes;
    }

    /**
     * Ожидает строку в формате:
     * yyyy-MM-ddTHH:MM
     **/
    public CustomDateTime(String datetime){
        ParseTime(datetime);
    }

    public CustomDateTime(){}

    public boolean IsLargerThan(CustomDateTime dateTime){
        if (Year < dateTime.Year)
            return false;
        if (Month < dateTime.Month)
            return false;
        if (Day < dateTime.Day)
            return false;
        if (Hour < dateTime.Hour)
            return false;
        if (Minutes <= dateTime.Minutes)
            return false;
        return true;
    }

    public boolean IsLessThan(CustomDateTime dateTime){
        if (Year > dateTime.Year)
            return false;
        if (Month > dateTime.Month)
            return false;
        if (Day > dateTime.Day)
            return false;
        if (Hour > dateTime.Hour)
            return false;
        if (Minutes >= dateTime.Minutes)
            return false;
        return true;
    }

    public boolean IsEqual(CustomDateTime dateTime){
        return Year == dateTime.Year &&
                Month == dateTime.Month &&
                Day == dateTime.Day &&
                Hour == dateTime.Hour &&
                Minutes == dateTime.Minutes;
    }


    @Override
    public String toString() {
        return String.format("%s:%s",
                TimeElemToString(Hour),
                TimeElemToString(Minutes));
    }

    /**
     * Добавляет "0" перед однозначными числами
     **/
    private String TimeElemToString(int timeElem){
        return timeElem < 10 ? "0"+timeElem : String.valueOf(timeElem);
    }

    /**
     * Ожидает строку в формате:
     * yyyy-MM-ddTHH:MM
     **/
    public void ParseDatetime(String datetime){
        var splitted = datetime.split("T");
        ParseDate(splitted[0]);
        ParseTime(splitted[1]);
    }

    /**
     * Ожидает строку в формате:
     * yyyy-MM-dd
     **/
    public void ParseDate(String date){
        var dates = date.split("-");
        try {
            Year =  Integer.parseInt(dates[0]);
            Month =  Integer.parseInt(dates[1]);
            Day =  Integer.parseInt(dates[2]);
        }
        catch (Exception e){
            Logger.WriteExeption("Date Parse error", e);
        }
    }

    /**
     * Ожидает строку в формате:
     * HH:MM
     **/
    public void ParseTime(String time){
        var times = time.split(":");
        try {
            Hour =  Integer.parseInt(times[0]);
            Minutes =  Integer.parseInt(times[1]);
        }
        catch (Exception e){
            Logger.WriteExeption("Time Parse error", e);
        }
    }

}
