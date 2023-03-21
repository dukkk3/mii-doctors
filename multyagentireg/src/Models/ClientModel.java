package Models;

import java.util.ArrayList;

public class ClientModel {
    public String Name;
    public ArrayList<String> NeedVisit;
    public ArrayList<MRecord> Schedule;
    public Integer Index;
    static int Count = 1;

    public ClientModel() {
        Index = Count;
        Count++;
    }

    @Override
    public String toString() {
        String schedule = "Записи к специалистам: \n";
        for (int i = 0; i < Schedule.size(); i++){
            schedule += String.format("\t Специалист %s по имени %s, на время %d",
                    Schedule.get(i).SpecialistModel.Spec,
                    Schedule.get(i).SpecialistModel.Name,
                    Schedule.get(i).DatePr.Hour);
        }

        return String.format("Пациент %s\n", Name, schedule);
    }
}
