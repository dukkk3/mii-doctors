import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Server {
    static AgentController Registry;

    public static void main(String[] args) {
        System.out.println("Start main");
        Runtime rt = Runtime.instance();
        Profile p  = new ProfileImpl();
        p.setParameter("gui", "true");
        ContainerController cc = rt.createMainContainer(p);
        rt.setCloseVM(true);

        try {
            Registry = cc.createNewAgent("Registry"+System.currentTimeMillis(),"Agents.Registry", null);
            Registry.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
