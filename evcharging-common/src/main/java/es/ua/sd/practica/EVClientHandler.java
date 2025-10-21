package es.ua.sd.practica;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class EVClientHandler implements Runnable {
	private final Socket clientSocket;
	private final Consumer<String> handler;

    public EVClientHandler(Socket socket, Consumer<String> handler) {
        this.clientSocket = socket;
		this.handler = handler;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
        	String ackMessage = "ACK";
            out.println(ackMessage);
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handler.accept(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Conexión perdida con el cliente: " + clientSocket.getInetAddress());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}
