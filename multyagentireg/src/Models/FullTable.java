package Models;

import java.util.HashMap;
import java.util.Map;

public class FullTable {
    public Integer[][] FullSchedule;
    public boolean IsOk = true;
    public boolean IsChanged = false;

    public Dict DoctorIndex = new Dict();

    public FullTable(Integer[][] fullSchedule) {
        FullSchedule = fullSchedule;
    }
}
