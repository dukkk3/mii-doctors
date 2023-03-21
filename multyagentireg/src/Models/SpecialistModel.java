package Models;

import java.util.ArrayList;

public class SpecialistModel {
    public String Name;
    public String Spec;
    public ArrayList<Slot> Schedule;
    public int SpecialistId;

    public SpecialistModel(String name, String spec, ArrayList<Slot> schedule) {
        Name = name;
        Spec = spec;
        Schedule = schedule;
    }

    public SpecialistModel(String name, String spec) {
        Name = name;
        Spec = spec;
    }

    @Override
    public String toString() {
        String schedule = "";

        for (int i = 0; i < Schedule.size(); i++){
            schedule += "\t"+Schedule.get(i).toString() + "\n";
        }
        return String.format("Доктор %s, специальность: %s, \nРасписание:\n%s", Name, Spec, schedule) ;
    }
}
