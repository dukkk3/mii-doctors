package Agents;

import Classes.Logger;
import Models.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import static Models.Storage.ClientIndex;
import static Models.Storage.DoctorIndex;


public class Registry extends Agent {
    ArrayList<SpecialistModel> FullSpecialistinfo;
    Integer[][] FullSchedule;
    boolean CanClientAnswer = false;


    @Override
    public void setup() {
        Logger.DrawLine();
        Logger.DrawLine();
        Logger.WriteLog();

        this.AddSpecialists();

        this.AddClients();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Agents.Registry");
        sd.setName("Agents.Registry");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(this.getLocalName() + ": зарегистрирован как " + "Agents.Registry");

        addBehaviour(new SpecialistInfoExchange());
        addBehaviour(new ClientSpeaking());

    }

    public void ReadySchedule(){
        int minH = FullSpecialistinfo.get(0).Schedule.get(0).Time.Hour;
        int maxH = FullSpecialistinfo.get(0).Schedule.get(0).Time.Hour;
        for (SpecialistModel Specialist : FullSpecialistinfo){
            int curMin = Specialist.Schedule.get(0).Time.Hour;
            int curMax = Specialist.Schedule.get(0).Time.Hour;
            for (Slot slot : Specialist.Schedule){
                int curH = slot.Time.Hour;
                if (curH < curMin){
                    curMin = curH;
                }
                if (curH > curMax){
                    curMax = curH;
                }
            }
            if (curMax > maxH){
                maxH = curMax;
            }
            if (curMin < minH){
                minH = curMin;
            }
        }
        int scheduleHigh = maxH - minH + 1;

        FullSchedule = new Integer[FullSpecialistinfo.size()][scheduleHigh];

        for(int i = 0; i < FullSpecialistinfo.size(); i++){
            for (int j = 0; j < scheduleHigh; j++){
                FullSchedule[i][j] = -1;
                for (Slot slot : FullSpecialistinfo.get(i).Schedule){
                    if (j == slot.Time.Hour - minH){
                        FullSchedule[i][j] = 0;
                    }
                }
            }
        }
        for (int i = 0; i<scheduleHigh; i++){
            Storage.TimeIndex.put(i, minH+i);
        }
    }

    public void PrintFullSchedule(){
        String schedule = "Расписание: \n";
        schedule += "Время\t|";
        for (SpecialistModel Specialist : FullSpecialistinfo){
            int cut = 5;
            if (Specialist.Spec.length() < 5) cut = Specialist.Spec.length();
            schedule += String.format("\t %s \t|", Specialist.Spec.substring(0, cut));
        }

        int len = schedule.length();
        schedule += "\n";
        for(int i = 0; i < len; i++)
            schedule += "-";
        schedule += "\n";

        for (int j = 0; j < FullSchedule[0].length; j++) {
            schedule += String.format("%d:00 \t|", Storage.TimeIndex.get(j));
            for(int i = 0; i < FullSchedule.length; i++){
                schedule += String.format("\t  %d  \t|", FullSchedule[i][j]);
            }
            schedule += "\n";
            for(int i = 0; i < len; i++)
                schedule += "-";
            schedule += "\n";
        }
        Logger.WriteLog(schedule);
        System.out.println(schedule);
        PrintClientNames();
    }

    public void PrintFullSchedule(boolean toOut){
        String schedule = "FullSchedule: \n";
        schedule += "Time\t|";
        int cnt = -1;
        for (SpecialistModel Specialist : FullSpecialistinfo){
            int cut = 5;
            cnt++;
            if (Specialist.Spec.length() < 5) cut = Specialist.Spec.length();
                schedule += String.format("\t %s \t|", Specialist.Spec.substring(0, cut));
        }
        schedule += "\n";
        for (int j = 0; j < FullSchedule[0].length; j++) {
            schedule += String.format("%d:00 \t|", Storage.TimeIndex.get(j));
            for(int i = 0; i < FullSchedule.length; i++){
                schedule += String.format("\t  %d  \t|", FullSchedule[i][j]);
            }
            schedule += "\n";
        }
        Logger.WriteOutput(schedule);
        Logger.WriteOutput();
        Logger.WriteOutput();

    }

    public void PrintFullScheduleWithNames(){
        String schedule = "FullSchedule: \n";
        schedule += "Time\t|";
        int restIndex = -1;
        for (SpecialistModel Specialist : FullSpecialistinfo){
            int cut = 5;
            if (Specialist.Spec.length() < 5) cut = Specialist.Spec.length();
            else
                schedule += String.format(" %s |", MakeSpaceForWord(Specialist.Spec, 16));
        }
        int width = schedule.length();
        schedule += "\n";
        for (int j = 0; j < FullSchedule[0].length; j++) {
            schedule += String.format("%d:00 \t|", Storage.TimeIndex.get(j));
            for(int i = 0; i < FullSchedule.length; i++){
                String name = "";
                if (ClientIndex.containsKey(FullSchedule[i][j])){
                    name = ClientIndex.get(FullSchedule[i][j]);
                }
                else {
                    if (FullSchedule[i][j] == 0){
                        name = "Empty";
                    }
                    else
                        name = "NotPossible";
                }
                schedule += String.format(" %s |", MakeSpaceForWord(name, 16));
            }
            schedule += "\n";
        }
        Logger.WriteOutput(schedule);
    }

    public String MakeLine(int length){
        String line = "";
        for (int i = 0; i < length; i++){
            line += "_";
        }
        return line;
    }

    public String MakeSpaceForWord(String word, int size){
        int curSize = word.length();
        String newWord = "";
        if (curSize < size){
            String last = "";
            while (newWord.length()*2 + curSize < size){
                last = newWord;
                newWord+= " ";
            }
            var copyNew = newWord;

            if (curSize % 2 == 0){
                newWord +=word+copyNew;
            }
            else {
                newWord += word+last;
            }
        }
        else {
            newWord = word.substring(0, size);
        }
        return newWord;
    }
    public void PrintClientNames(){
        String msg = "Пациенты и их индексы: \n" + Storage.ClientIndex.toString();
        System.out.println(msg);
    }

    public boolean TrySetSchedule(Integer[][] ClientSchedule){
        ArrayList<Integer> is = new ArrayList<>();
        ArrayList<Integer> js = new ArrayList<>();

        for(int i = 0; i < FullSchedule.length; i++){
            for (int j = 0; j < FullSchedule[i].length; j++){
                if (FullSchedule[i][j] != ClientSchedule[i][j]){
                    if (FullSchedule[i][j] == 0){
                        is.add(i);
                        js.add(j);
                    }
                    else {
                        return false;
                    }
                }
            }
        }
        for(int i = 0; i<is.size(); i++){
            FullSchedule[is.get(i)][js.get(i)] = ClientSchedule[is.get(i)][js.get(i)];
        }
        return true;
    }

    public void IndexSpecialist(){
        for (int i = 0; i < FullSpecialistinfo.size(); i++){
            Storage.DoctorIndex.put(FullSpecialistinfo.get(i).Spec, i);
        }
    }

    public void AddSpecialists() {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ContainerController cc =  getContainerController();
                var folderPath = System.getProperty("user.dir");
                var DoctorFileName = "Specialists.txt";
                var SpecialistsPath = Paths.get(folderPath, "Data", DoctorFileName);
                try {
                    var SpecialistString = Files.readString(SpecialistsPath);
                    Logger.WriteLog("AddSpecialists: SpecialistString All specs=" + SpecialistString);
                    var SpecialistsStrings = SpecialistString.split("\r\n");

                    for (int i = 0; i < SpecialistsStrings.length; i++){
                        Logger.WriteLog("AddSpecialists: SpecialistsStrings=" + SpecialistsStrings[i]);
                        CreateAgent("Specialist-agent"+System.currentTimeMillis(), "Agents.Specialist", new Object[]{SpecialistsStrings[i]}).start();
                        doWait(5);
                    }

                } catch (IOException | StaleProxyException e) {
                    Logger.WriteExeption("AddSpecialists: read file", e);
                    e.printStackTrace();
                }
            }
        });
    }

    public  void AddClients(){
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ContainerController cc =  getContainerController();
                var folderPath = System.getProperty("user.dir");
                var ClientFileName = "Clients.txt";
                var ClientsPath = Paths.get(folderPath, "Data", ClientFileName);
                try {
                    var ClientsString = Files.readString(ClientsPath);
                    Logger.WriteLog("AddSpecialists: ClientsString=" + ClientsString);
                    var ClientsStrings = ClientsString.split("\r\n");

                    for (int i = 0; i < ClientsStrings.length; i++){
                        Logger.WriteLog("AddSpecialists: ClientsStrings=" + ClientsStrings[i]);
                        CreateAgent("Client-agent"+System.currentTimeMillis(), "Agents.Client", new Object[]{ClientsStrings[i]}).start();
                        doWait(5);
                    }

                } catch (IOException | StaleProxyException e) {
                    Logger.WriteExeption("AddClients: read file", e);
                    e.printStackTrace();
                }
            }
        });
    }

    public AgentController CreateAgent(String agentName, String className, Object[] args) throws StaleProxyException {
        ContainerController cc =  getContainerController();
        return cc.createNewAgent(agentName, className, args);
    }

    public void PrintAllSpecialist(){
        String message = "Информация о всех докторах: \n";
        for (SpecialistModel Specialist : FullSpecialistinfo){
            message += String.format("%s\n\n", Specialist.toString());
        }
        System.out.println(message);
        Logger.WriteLog(message);
    }

    public String PrintScheduleForClient(int id){
        String schedule = "";

        if (ClientIndex.containsKey(id)){
            schedule += "client " + ClientIndex.get(id) + "\n";
        }

        for (int j = 0; j < FullSchedule[0].length; j++){
            for (int i = 0; i < FullSchedule.length; i++){
                if (FullSchedule[i][j] == id){
                    schedule += String.format("%s\t|\t%s\n", String.format("%d:00", Storage.TimeIndex.get(j)), MakeSpaceForWord(Storage.DoctorIndex.GetByVal(i), 16));
                }
            }
        }

        return schedule;
    }

    public void PrintSchedulesForClients(){
        String schedule = "Timetable for each client\n";
        for (var p : ClientIndex.keySet()){
            schedule += PrintScheduleForClient(p) + "\n";
        }
        Logger.WriteOutput(schedule);
    }




    private class RegistryBehaviour extends CyclicBehaviour {
        private int receiveCount = 0;
        ArrayList<DFAgentDescription> Specialists;

        ArrayList<DFAgentDescription> searchByType(String type) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
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


        @Override
        public void action() {
            Logger.WriteLog("Agents.Registry: RegistryBehaviour.action():");
            MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage      msg = myAgent.blockingReceive(mt, 500);
            if(msg != null) {
                Logger.WriteLog("Registry got message: " + msg.getContent());
            }
            else {
                Logger.WriteLog("регистратура опрашивает докторов");
                Specialists = searchByType("Agents.Specialist");
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for(DFAgentDescription a : Specialists) {
                    Logger.WriteLog("Добавляем в получатели: " + a.getName());
                    cfp.addReceiver(a.getName());
                }
                MessageResult message = new MessageResult();
                message.Sender = getName();
                message.Action = "GetSlots";
                Gson gson = new Gson();
                String msgJson = gson.toJson(message);

                cfp.setContent(msgJson);
                cfp.setReplyWith("cfp" + System.currentTimeMillis());
                myAgent.send(cfp);
            }
        }
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


    private class SpecialistInfoExchange extends Behaviour {
        ArrayList<DFAgentDescription> Specialists;
        private int SpecialistCount = 0;
        private boolean SpecialistsLoaded = false;
        private int step = 1;
        private boolean done = false;

        ArrayList<DFAgentDescription> searchByType(String type) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
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


        @Override
        public void action() {
            if (SpecialistsLoaded){
                ACLMessage      msg = new ACLMessage(ACLMessage.CFP);
                Gson gson = new Gson();

                switch (step){
                    case 1:
                        System.out.println("регистратура: Рассылка сообщений по докторам");
                        for (int i = 0; i < Specialists.size(); i++){
                            msg.addReceiver(Specialists.get(i).getName());
                        }
                        SpecialistCount = Specialists.size();
                        MessageResult getFullInfMsg = new MessageResult();
                        getFullInfMsg.Sender = getName();
                        getFullInfMsg.Action = "GetFullInfo";

                        String msgJson = gson.toJson(getFullInfMsg);

                        msg.setContent(msgJson);
                        msg.setReplyWith("cfp" + System.currentTimeMillis());
                        FullSpecialistinfo = new ArrayList<>();
                        myAgent.send(msg);
                        step = 2;
                        break;
                    case 2:
                        System.out.println("Прием сообщений от докторов");

                        MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                        ACLMessage      msgInp = myAgent.blockingReceive(mt, 500);
                        if (msgInp != null){
                            System.out.println("В регистратуру пришло сообщение: " +msgInp.getContent());
                            MessageResult messageResult = gson.fromJson(msgInp.getContent(), MessageResult.class);
                            System.out.println("В регистратуре messageResult.Action:" + messageResult.Action);
                            if (messageResult.Action.equals("GetFullInfo")){
                                var SpecialistInfo = gson.fromJson(messageResult.JsonContent, SpecialistModel.class);
                                Logger.WriteLog("Пришла информация о докторе: " + SpecialistInfo.Name);
                                System.out.println("Пришла информация о докторе: " + SpecialistInfo.Name);
                                System.out.println(String.format("Пришла информация о докторе %d/%d: %s", FullSpecialistinfo.size()+1, SpecialistCount, SpecialistInfo.Name));
                                FullSpecialistinfo.add(SpecialistInfo);
                            }
                            if (FullSpecialistinfo.size() == SpecialistCount){
                                Logger.WriteLog("Сбор данных завершен! ");
                                System.out.println("Сбор данных завершен! ");
                                step = 3;
                                IndexSpecialist();
                                PrintAllSpecialist();
                                ReadySchedule();
                                PrintFullSchedule();
                                CanClientAnswer = true;
                                done = true;
                            }
                        }
                        break;
                }

            }
            else {
                Specialists = searchByType("Agents.Specialist");
                if (Specialists.size() > 0){
                    SpecialistsLoaded = true;
                    step = 1;
                }
                else {
                    System.out.println("Специалистов не обнаружено");
                }
            }


        }

        @Override
        public boolean done() {
            if (done){
                Logger.WriteLog("регистратура закончила сбор данных о докторах");
            }
            return done;
        }
    }



    private class ClientSpeaking extends Behaviour {

        private boolean done = false;
        private int ClientCnt = 0;

        @Override
        public void action() {
            if (CanClientAnswer){
                Gson gson = new Gson();
                MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage      msg = myAgent.blockingReceive(mt, 100);
                if (msg != null){
                    if (msg.getContent().isEmpty()){
                        return;
                    }
                    MessageResult messageResult = gson.fromJson(msg.getContent(), MessageResult.class);
                    ACLMessage reply = null;
                    boolean needAnswer = true;
                    switch (messageResult.Action){
                        case "GetSchedule":
                            var sched = new FullTable(FullSchedule);
                            sched.DoctorIndex = DoctorIndex;
                            String schedule = gson.toJson(sched);
                            reply = prepareAns(msg, "GetSchedule", schedule);
                            ClientCnt++;
                            needAnswer = true;
                            break;
                        case "TrySetSchedule":
                            System.out.println(String.format("регистратура принимает попытку пациента изменить расписание:\n%s", messageResult.JsonContent));
                            FullTable ClientSchedule = null;
                            Integer[][] ClientScheduleArr = null;
                            try{
                                ClientSchedule = gson.fromJson(messageResult.JsonContent, FullTable.class);
                            }catch (Exception e){
                                ClientScheduleArr = gson.fromJson(messageResult.JsonContent, new TypeToken<Integer[][]>() {}.getType());
                            }
                            boolean isSuccess = false;
                            if (ClientSchedule != null){
                                isSuccess = TrySetSchedule(ClientSchedule.FullSchedule);
                            }
                            else {
                                isSuccess = TrySetSchedule(ClientScheduleArr);
                            }
                            System.out.println("регистратура проверила расписание: " + isSuccess);
                            sched = new FullTable(FullSchedule);
                            String scheduleJson = gson.toJson(sched);
                            reply = prepareAns(msg, "TrySetSchedule", scheduleJson, isSuccess);
                            if (isSuccess){
                                PrintFullSchedule();
                                ClientCnt--;
                                if (ClientCnt == 0){
                                    PrintFullScheduleWithNames();
                                    PrintSchedulesForClients();
                                }
                            }
                            else {
                                PrintFullSchedule();
                            }
                            needAnswer = true;
                            break;
                        default:
                            needAnswer = false;
                            break;
                    }

                    if (needAnswer){
                        Logger.WriteLog("регистратура отвечает: " + reply.getContent());
                        System.out.println("регистратура отвечает: " + reply.getContent());
                        myAgent.send(reply);
                    }
                }
            }
        }


        @Override
        public boolean done() {
            if (done){
                Logger.WriteLog("Регистратура закончила сбор данных о докторах");
            }
            return done;
        }
    }
}
