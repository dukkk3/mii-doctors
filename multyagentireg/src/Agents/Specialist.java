package Agents;

import Classes.CustomDateTime;
import Classes.Logger;
import Models.MessageResult;
import Models.Slot;
import Models.SpecialistModel;
import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Specialist  extends Agent implements Serializable {
    SpecialistModel Props;
    CyclicBehaviour behaviour;


    @Override
    public String toString() {
        String schedule = "";

        for (int i = 0; i < Props.Schedule.size(); i++){
            schedule += "\t"+Props.Schedule.get(i).toString() + "\n";
        }
        return String.format("Agents.Specialist %s, spec %s, \nSchedule:\n%s", Props.Name, Props.Spec, schedule) ;
    }

    @Override
    protected void setup() {
        Logger.WriteLog("Agents.Specialist setup() start");

        Object[] args = getArguments();

        try {
            ParseSelf((String) args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Logger.WriteLog("Agents.Specialist setup() Props:\n" + this.toString());


        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Agents.Specialist");
        sd.setName("OneSpecialist");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            Logger.WriteExeption("Agents.Specialist register()", fe);
        }
        System.out.println(this.getLocalName() + ": зарегистрирован как " + "Agents.Specialist");
        addBehaviour(new SpeakingBehaviour());

    }


    /**
     * Ожидает строку в формате
     * DoctorName;SpecialistSpec;DateTime1,DateTime2,DateTime3
     * где DateTime в формате HH:MM
     **/
    private void ParseSelf(String specialist) throws Exception {
        var props = specialist.split(";");
        if (props.length < 3){
            throw new Exception("Пропишите все 3 свойсвта доктора через точку с запятой");
        }

        Props = new SpecialistModel(props[0], props[1]);
        Props.Schedule = new ArrayList<>();

        if (props[2].isEmpty()){
            return;
        }

        var times = props[2].split(",");
        for (int i = 0; i < times.length; i++){
            Props.Schedule.add(new Slot(i, new CustomDateTime(times[i]), getName(), Props));
        }

    }

    private ArrayList<Slot> GetFreeSlots(){
        ArrayList<Slot> freeSlots = new ArrayList<>();
        for (int i = 0; i < Props.Schedule.size(); i++){
            if (Props.Schedule.get(i).IsFree()){
                freeSlots.add(Props.Schedule.get(i));
            }
        }
        return freeSlots;
    }

    private boolean MakeAppoint(AID ClientAID, int slotId){
        if(Props.Schedule.get(slotId).IsFree()) {
            Props.Schedule.get(slotId).ClientAID = ClientAID;
            return true;
        }
        return false;
    }

    private void CancleAppoint(Slot slot){
        for (int i = 0; i < Props.Schedule.size(); i++){
            var curSlot = Props.Schedule.get(i);
            if (curSlot.Time.IsEqual(slot.Time)){
                if (curSlot.ClientAID == slot.ClientAID){
                    curSlot.ClientAID = null;
                }
            }
        }
    }

    private void SetSlots(ArrayList<Slot> slots){
        Props.Schedule = slots;
    }


    private class SpeakingBehaviour extends CyclicBehaviour {
        private int receiveCount = 0;
        private boolean isFirst = true;
        ArrayList<DFAgentDescription> registry;

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

        /**
         * Подготовка ответного сообщения
         * @param msg пришедшее сообщение
         * @param action вызываемое нашим ответом действие
         * @param content отправляемый контент
         **/
        private ACLMessage prepareAns(ACLMessage msg, String action, String content){
            System.out.println("доктор готовит ответ");
            var msgRes = new MessageResult();
            msgRes.Action = action;
            msgRes.JsonContent = content;
            Gson gson = new Gson();
            var msgResJson = gson.toJson(msgRes);
            var reply = msg.createReply();
            reply.setContent(msgResJson);
            return  reply;
        }


        @Override
        public void action() {
            MessageTemplate mt  = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage      msg = myAgent.blockingReceive(mt, 500);
            registry = searchByType("Agents.Registry");
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for(DFAgentDescription a : registry) {
                cfp.addReceiver(a.getName());
            }
            if(msg != null) {
                if (msg.getContent().isEmpty()){
                    return;
                }
                boolean needAnswer = true;
                Gson gson = new Gson();
                System.out.println("доктору Пришло сообщение: " + msg.getContent());
                MessageResult messageResult = gson.fromJson(msg.getContent(), MessageResult.class);
                ACLMessage reply = null;
                switch (messageResult.Action){
                    case "GetSlots":
                        String schedule = gson.toJson(Props.Schedule);
                        reply = prepareAns(msg, "", schedule);
                        break;
                    case "GetFreeSlots":
                        String freeSlotsJson = gson.toJson(GetFreeSlots());
                        reply = prepareAns(msg, "", freeSlotsJson);
                        break;
                    case "MakeAppoint":
                        Slot slot = gson.fromJson(messageResult.JsonContent, Slot.class);

                        String isSuccess = gson.toJson(MakeAppoint(slot.ClientAID, slot.SlotIndex));
                        reply = prepareAns(msg, "", isSuccess);
                        break;
                    case "CancleAppoint":
                        Slot cancleSlot = gson.fromJson(messageResult.JsonContent, Slot.class);
                        CancleAppoint(cancleSlot);
                        needAnswer = false;
                        break;
                    case "GetFullInfo":
                        String propsJson = gson.toJson(Props);
                        reply = prepareAns(msg, "GetFullInfo", propsJson);
                        break;
                    default:
                        needAnswer = false;
                        break;
                }
                if (needAnswer){
                    Logger.WriteLog("доктор отвечает: " + reply.getContent());
                    System.out.println("доктор отвечает: " + reply.getContent());
                    myAgent.send(reply);
                }
            }
        }
    }
}
