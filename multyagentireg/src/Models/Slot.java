package Models;

import Classes.CustomDateTime;
import jade.core.AID;


public class Slot {
    public AID ClientAID = null;
    public int SlotIndex = -1;
    public CustomDateTime Time;
    public String DoctorName;

    public Slot(int slotIndex, CustomDateTime time, String DoctorName, SpecialistModel SpecialistModel) {
        SlotIndex = slotIndex;
        Time = time;
        DoctorName = DoctorName;
    }

    public Slot(CustomDateTime time) {
        Time = time;
    }


    public Slot(int slotIndex, CustomDateTime time) {
        SlotIndex = slotIndex;
        Time = time;
    }

    public Slot(AID ClientAID, int slotIndex, CustomDateTime time) {
        ClientAID = ClientAID;
        SlotIndex = slotIndex;
        Time = time;
    }

    public void SetClient(AID ClientAID){
        ClientAID = ClientAID;
    }

    public boolean IsFree(){
        return ClientAID == null;
    }

    public void MakeFree(){
        ClientAID = null;
    }

    @Override
    public String toString() {
        String pName = (ClientAID != null)?ClientAID.getName():"null";
        return String.format("ClientName = %s; Time = %s", pName, Time.toString());
    }
}
