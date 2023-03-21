package Models;


import Classes.CustomDateTime;

public class MRecord {
    public SpecialistModel SpecialistModel;


    public String NeedVisit;

    /**
     * Длительность приема в часах
     **/
    public int Duration = 1;

    /**
     * Время приема
     **/
    public CustomDateTime DatePr;

    public int CaclWindowNumBetween(MRecord MRecord){
        return (Math.abs(MRecord.DatePr.Hour - DatePr.Hour) - 1)/Duration;
    }
}
