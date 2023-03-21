package Agents;

import Classes.Logger;
import Models.*;
import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Client extends Agent {
    String Name;
    ArrayList<String> NeedVisitDoctors;
    ArrayList<Integer> MySpecialists = new ArrayList<>();
    ArrayList<String> NotExistsspect = new ArrayList<>();
    Dict DoctorIndexs = new Dict();
    ArrayList<Integer> SpecialistExclude = new ArrayList<>();

    ClientModel Props;

    protected ArrayList<Specialist> AllSpecialist;

    private AID myManager;

    @Override
    public void setup() {
        Object[] args = getArguments();

        try {
            ParseSelf((String) args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Agents.Client");
        sd.setName("OneClient");
        dfd.addServices(sd);

        try {
            FileOutputStream writer = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(this.getLocalName() + ": зарегистрирован как " + "Agents.Client");
        addBehaviour(new RequestPerformer());
    }

    private ArrayList<Integer> CalcMyspecIndexes(){
        String msg = "";
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < NeedVisitDoctors.size(); i++) {
            var spectIndex = Storage.DoctorIndex.GetValue(NeedVisitDoctors.get(i));
            if (spectIndex != null){
                if (!IsExcluded(spectIndex)){
                    result.add(spectIndex);
                    msg += " + " + NeedVisitDoctors.get(i);
                }
                else {
                    msg += " excluded " + NeedVisitDoctors.get(i);
                }
            }
            else {
                msg += " - " + NeedVisitDoctors.get(i);
                NotExistsspect.add(NeedVisitDoctors.get(i));
            }
        }
        return result;
    }

    private boolean IsExcluded(int specialist){
        for (var exc : SpecialistExclude){
            if (exc == specialist){
                return true;
            }
        }
        return false;
    }

    private boolean IsSpecialistHaveTime(Integer[][] schedule, int specialist){
        for (int j = 0; j < schedule[specialist].length; j++){
            if (schedule[specialist][j] == 0){
                return true;
            }
        }
        return false;
    }

    private FullTable ReadyChedule(Integer[][] schedule){
        System.out.println(String.format("Пациент %s %d ReadyChedule()", Props.Name, Props.Index));
        Integer[][] readySchedule = Copy(schedule);
        FullTable result = new FullTable(readySchedule);
        boolean isChanged = true;
        var Specialists = CalcMyspecIndexes();
        for (var spec : Specialists) {
            if (!IsSpecialistHaveTime(schedule, spec)) {
                SpecialistExclude.add(spec);
            }
        }
        boolean globalChange = false;
        while (isChanged){
            isChanged = false;
            Specialists = CalcMyspecIndexes();
            for (var spec : Specialists){
                result =  CheckSpecialistOneTime(result.FullSchedule, spec);
                if (result.IsChanged){
                    isChanged = true;
                    globalChange = true;
                }
            }
        }
        result.IsChanged = globalChange;
        return result;
    }

    private FullTable CheckSpecialistOneTime(Integer[][] schedule, int specialist){
        ArrayList<Integer> freeIndexes = new ArrayList<>();
        boolean isChanged = false;
        for (int j = 0; j < schedule[specialist].length; j++){
            if (schedule[specialist][j] == 0){
                freeIndexes.add(j);
            }
        }
        if (freeIndexes.size() == 1){
            if (CanMakeAppoint(schedule, freeIndexes.get(0))){
                schedule[specialist][freeIndexes.get(0)] = Props.Index;
                isChanged = true;
            }
        }
        FullTable result = new FullTable(schedule);
        result.IsChanged = isChanged;
        if (isChanged){
            Print2d(result.FullSchedule, String.format("Пациент %s %d CheckSpecialistOneTime() SpecialistExclude.add EXCLUDE spec = %s, TIME = %d", Props.Name, Props.Index, specialist, freeIndexes.get(0)));
        }
        return result;
    }

    private boolean CanMakeAppoint(Integer[][] schedule, int time){
        for (int i = 0; i < schedule.length; i++){
            if (schedule[i][time] == Props.Index){
                return false;
            }
        }
        return true;
    }

    int Shift = 0;
    private ArrayList<Integer> ShiftSpecialists(int shift){
        var curList = CalcMyspecIndexes();
        if (shift == 0){
            return curList;
        }
        ArrayList<Integer> shifted = new ArrayList<>();
        for (int i = shift; i < curList.size();i++){
            shifted.add(curList.get(i));
        }
        for (int i = 0; i < shift; i++){
            shifted.add(curList.get(i));
        }
        return shifted;
    }

    private int EstimateSchedule(Integer[][] schedule){
        int lastIndex = -1;
        int myAppCnt = 0;
        ArrayList<Integer> windows = new ArrayList<>();
        for (int i = 0; i < schedule[0].length; i++){
            for (int j = 0; j < schedule.length; j++){
                if (schedule[j][i] == Props.Index){
                    if (lastIndex != -1){
                        int curWin = Math.abs(lastIndex - i);
                        if (curWin > 1){
                            windows.add(curWin);
                        }
                    }
                    lastIndex = i;
                    myAppCnt++;
                }
            }
        }
        int mark = 0;
        for (var win : windows){
            mark += win*windows.size();
        }

        return mark + ((NeedVisitDoctors.size() - myAppCnt + 1) * 10);
    }

    private Integer[][] Copy(Integer[][] array){
        Integer[][] result = new Integer[array.length][array[0].length];
        for (int i = 0; i<array.length; i++){
            for (int j = 0; j<array[i].length; j++){
                result[i][j] = array[i][j];
            }
        }
        return result;
    }

    private Integer[][] MakeSchedule(Integer[][] schedule){
        Print2d(schedule, String.format("Пациент %s %d MakeSchedule()", Props.Name, Props.Index));
        var curSchedule = Copy(schedule);
        var res = ReadyChedule(curSchedule);
        curSchedule = res.FullSchedule;
        var mainSchedule = Copy(curSchedule);


        MySpecialists = CalcMyspecIndexes();
        var curSpecialists = CalcMyspecIndexes();

        String msg = "";
        for (int i = 0; i < curSpecialists.size(); i++){
            msg += "\t"+curSpecialists.get(i);
        }
        System.out.println("CURSpecialists = " + msg);

        Print2d(curSchedule, String.format("Пациент %s %d НАЧАЛО MakeSchedule() curSol", Props.Name, Props.Index));
        System.out.println(String.format("curSchedule Sizes %d %d", curSchedule.length, curSchedule[0].length));
        ArrayList<Integer[][]> solutions = new ArrayList<>();
        ArrayList<Integer> marks = new ArrayList<>();
        int shift = 0;
        while (shift < curSpecialists.size()){
            for (int i = 0; i<curSchedule[0].length; i++){
                for (int k = 0; k < MySpecialists.size(); k ++) {
                    curSpecialists = ShiftSpecialists(shift);
                    curSchedule = Copy(mainSchedule);
                    if (curSchedule[MySpecialists.get(k)][i] == 0 && !(Storage.DoctorIndex.GetByVal(MySpecialists.get(k)).equals("Флюрография"))){
                        curSchedule[MySpecialists.get(k)][i] = Props.Index;
                        curSpecialists.remove(MySpecialists.get(k));
                        var curSol = MakeSteps(curSchedule, i, curSpecialists);
                        if (curSol != null){

                            solutions.add(curSol);
                            var mark = EstimateSchedule(curSol);
                            marks.add(mark);
                            if (mark == 0){
                                break;
                            }
                        }
                    }
                }

            }
            shift++;
        }

        if (marks.size() == 0){
            return mainSchedule;
        }
        int minIndex = 0;
        int minMark = marks.get(0);
        for (int i = 0; i < marks.size(); i++){
            var curMark = marks.get(i);
            if (curMark < minMark){
                minIndex = i;
                minMark = curMark;
            }
        }
        return solutions.get(minIndex);
    }


    private Integer[][] MakeSteps(Integer[][] schedule, int times, ArrayList<Integer> leftSpecialists){
        var curSchedule = Copy(schedule);
        if (leftSpecialists.size() == 0){
            return curSchedule;
        }

        int massageIndex = -1;
        for (int j = 0; j< leftSpecialists.size(); j++){
            System.out.println(String.format("WHAT IN LEFTSPECS %d", leftSpecialists.get(j)));
            System.out.println(Storage.DoctorIndex.GetByVal(leftSpecialists.get(j)));
        }
       for (int i = times+1; i<curSchedule[0].length; i++){
            for (int j = 0; j< leftSpecialists.size(); j++){
                if (curSchedule[leftSpecialists.get(j)][i] == 0 && CanMakeAppoint(curSchedule, i))
                {
                    var spectName = Storage.DoctorIndex.GetByVal(leftSpecialists.get(j));
                    curSchedule[leftSpecialists.get(j)][i] = Props.Index;
                    leftSpecialists.remove(leftSpecialists.get(j));
                    return MakeSteps(curSchedule, i, leftSpecialists);
                }
            }
        }
        return curSchedule;
    }

    private void Print2d(Integer[][] array, String name){
        String msg = "";
        for (int i = 0; i<array[0].length; i++){
            for(int j = 0; j< array.length; j++){
                msg += String.format("%d\t", array[j][i]);
            }
            msg += "\n";
        }
        var mark = EstimateSchedule(array);
        System.out.println(name + "\tМАТРИЦА оценка:" + mark + "\n" + msg);
    }

    @Override
    protected void takeDown()
    {
        super.takeDown();
        System.out.println(this.getLocalName() + ": завершает работу");
    }

    private void ParseSelf(String Client) throws Exception {
        var props = Client.split(";");
        if (props.length < 2){
            throw new Exception("Пропишите все 2 свойства Пациента через точку с запятой");
        }
        this.Name = props[0];
        NeedVisitDoctors = new ArrayList<>();

        var flag = false;
        var specs = props[1].split(",");
        for (int i = 0; i < specs.length; i++){
            NeedVisitDoctors.add(specs[i]);
            if (specs[i].equals("Массаж")) flag = true;
        }
        if (flag)
            NeedVisitDoctors.add("Флюрография");



        this.Props = new ClientModel();
        Props.NeedVisit = NeedVisitDoctors;
        Props.Name = Name;
        Storage.ClientIndex.put(Props.Index, Props.Name);
    }

    private MRecord SlotToRecord(Slot slot){
        MRecord record = new MRecord();
        record.DatePr = slot.Time;

        return record;
    }




    private class RequestPerformer extends Behaviour {
        ArrayList<DFAgentDescription> questionsAgent, tickets;
        private int step = 0, receiver = 0, questionsCount = 0, request, propose, skipCount = 0;
        private boolean done = false;
        private MessageTemplate mt;

        DFAgentDescription registryAgent;

        ArrayList<DFAgentDescription> searchByType(String type) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd       = new ServiceDescription();
            sd.setType(type);
            template.addServices(sd);
            ArrayList<DFAgentDescription> desc = new ArrayList<>();
            try {
                desc.addAll(Arrays.asList(DFService.search(myAgent, template)));
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            for(DFAgentDescription a : desc) {
                if(a.getName().equals(myAgent.getAID())) {
                    desc.remove(a);
                    break;
                }
            }
            return desc;
        }

        private ACLMessage prepareAns(ACLMessage msg, String action, String content, boolean isSuccess){
            var msgRes = new MessageResult();
            msgRes.Action = action;
            msgRes.JsonContent = content;
            msgRes.IsSeccess = isSuccess;
            Gson gson = new Gson();
            var msgResJson = gson.toJson(msgRes);
            var reply = msg.createReply();
            reply.setContent(msgResJson);
            return  reply;
        }

        private ACLMessage prepareAns(ACLMessage msg, String action, String content){
            return prepareAns(msg, action, content, true);
        }


        @Override
        public void action() {
            Gson gson = new Gson();
            switch (step) {
                case 0:
                    System.out.println(String.format("Пациент %s %d в поиске регистратуры", Props.Name, Props.Index));
                    var registryAgents = searchByType("Agents.Registry");
                    if(registryAgents.size() != 0) {
                        registryAgent = registryAgents.get(0);
                        step = 1;
                    } else {
                        System.err.println(String.format("Пациент %s %d не нашел регистратуру", Props.Name, Props.Index));
                        done = true;
                        return;
                    }
                    break;
                case 1:
                    System.out.println(String.format("Пациент %s %d отправляет запрос на получение расписания", Props.Name, Props.Index));
                    MessageResult createMsg = new MessageResult();
                    createMsg.Action = "GetSchedule";
                    createMsg.Sender = getName();

                    ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
                    cfp.addReceiver(registryAgent.getName());
                    cfp.setConversationId("Client-Registry");
                    cfp.setContent(createMsg.toJson());
                    myAgent.send(cfp);
                    step = 2;
                    break;
                case 2:
                    MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage      msg = myAgent.blockingReceive( 500);
                    ACLMessage reply = null;
                    if (msg != null){
                        System.out.println(String.format("Пациент %s %d получает расписание: \n\t%s", Props.Name, Props.Index, msg.getContent()));
                        MessageResult messageResult = gson.fromJson(msg.getContent(), MessageResult.class);

                        if (messageResult.Action.equals("GetSchedule")){
                            FullTable loadedSchedule = gson.fromJson(messageResult.JsonContent, FullTable.class);
                            DoctorIndexs = loadedSchedule.DoctorIndex;
                            var readySchedule = MakeSchedule(loadedSchedule.FullSchedule);
                            FullTable ftab = new FullTable(readySchedule);
                            String scheduleJson = gson.toJson(ftab);
                            reply = prepareAns(msg, "TrySetSchedule", scheduleJson);
                            System.out.println(String.format("Пациент %s %d выбрал время", Props.Name, Props.Index));
                            myAgent.send(reply);
                            step = 3;
                        }
                    }
                    break;
                case 3:
                    mt  = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    msg = myAgent.blockingReceive(500);
                    if (msg != null)
                    {
                        System.out.println(String.format("Пациент %s %d получил ответ на свое расписание: %s", Props.Name, Props.Index, msg.getContent()));
                        MessageResult messageResult = gson.fromJson(msg.getContent(), MessageResult.class);
                        if (messageResult.Action.equals("TrySetSchedule")){
                            if (messageResult.IsSeccess){
                                done = true;
                            }
                            else {
                                System.out.println(String.format("Пациент %s %d не смог протолкнуть свой выбор, выбирвает повторно", Props.Name, Props.Index));
                                FullTable loadedSchedule = gson.fromJson(messageResult.JsonContent, FullTable.class);
                                var readySchedule = MakeSchedule(loadedSchedule.FullSchedule);
                                String scheduleJson = gson.toJson(readySchedule);
                                reply = prepareAns(msg, "TrySetSchedule", scheduleJson);
                                myAgent.send(reply);
                            }
                        }
                    }
                    break;
            }
        }



        String newLine = System.lineSeparator();

        @Override
        public boolean done() {
            if (done){
                Logger.WriteLog("Пациент " + Props.Name + " " + Props.Index + " закончил с расписанием");
            }
            return done;
        }

    }
}
