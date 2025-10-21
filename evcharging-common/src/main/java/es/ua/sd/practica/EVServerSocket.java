package es.ua.sd.practica;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class EVServerSocket implements Runnable{

	private final int port;
	private final Consumer<String> handler;
    public EVServerSocket(int port, Consumer<String> handler) {
        this.port = port;
		this.handler = handler;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            
            while (!Thread.currentThread().isInterrupted()) {
                
                Socket clientSocket = serverSocket.accept(); 
                
                Runnable clientHandler = new EVClientHandler(clientSocket, handler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
