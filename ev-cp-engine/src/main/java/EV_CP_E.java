import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.util.UUID;



import es.ua.sd.practica.CommonConstants;
import es.ua.sd.practica.EVServerSocket;
import es.ua.sd.practica.KConsumer;
import es.ua.sd.practica.Producer;

public class EV_CP_E {
	public static String broker;
	public static int port;
	public static String CP;

	public static void main(String[] args) {
		if (args.length < 2)
		{
			System.err.println("Escriba la IP del broker y el puerto del socket"); return;
		}
		
		broker = args[0];
		port = Integer.parseInt(args[1]);
		
		EVServerSocket EVServer = new EVServerSocket(port, EV_CP_E::handleMonitor);
        Thread socket = new Thread(EVServer);
        socket.start();
        
        Runnable consumerRequest = new KConsumer(broker, CommonConstants.REQUEST_CP, UUID.randomUUID().toString() , EV_CP_E::handleDriverRequest);
        Thread consumer = new Thread(consumerRequest);
        consumer.start();

        Scanner sc = new Scanner(System.in);

        while (true) {
            String input = sc.nextLine().trim().toLowerCase();
            if (input.equals("k")) {
                if (socket.isAlive()) {
                    System.out.println("KO...");
                    EVServer.stop();   
                    socket.interrupt();   
                    consumer.interrupt();
                } else {
                    System.out.println("VIVO...");
                    EVServer = new EVServerSocket(port, EV_CP_E::handleMonitor);
                    socket = new Thread(EVServer);
                    socket.start();
                    consumer = new Thread(consumerRequest);
                    consumer.start();

                }
            }
        }
	}
	
	static void handleDriverRequest(String message)
	{
		// ejemplo de request -> REQUEST#ALC001;0.35;Carrer de Sant Vicent;CONECTADO#10.0#DV001#0.35
		System.out.println(message);
		if(!CP.equals(message.split("#")[1].split("\\|")[1])) return;
		Producer r = new Producer(broker, CommonConstants.TELEMETRY);
		r.sendMessage("ACKREQUEST#" + CP + "#" + message.split("#")[3]);
		
		float kwrequested = Float.parseFloat(message.split("#")[2]);
		float kws = Float.parseFloat(message.split("#")[4]); // deberia dividirse entre 3600 ya que esta en KWH pero la simulación tardaría mucho
		float kwpassed = 0;
		while(true)
		{
			if(kwpassed + kws > kwrequested)
			{
				kwpassed = kwrequested;
				r.sendMessage("CHARGING#" + CP + "#" + message.split("#")[3] + "#" + kwpassed + "#" + kwrequested);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					break;
				}
				r.sendMessage("END#" + CP + "#" + message.split("#")[3]);
				break;
			}
			
			kwpassed += kws;
			r.sendMessage("CHARGING#" + CP + "#" + message.split("#")[3] + "#" + kwpassed + "#" + kwrequested);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				break;
			} 
		}
		
		r.close();
	}
	
	static void handleMonitor(String message)
	{
		CP = message.split("#")[1];
	}

}
