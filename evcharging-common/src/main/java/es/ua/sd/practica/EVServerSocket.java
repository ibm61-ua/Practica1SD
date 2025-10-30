package es.ua.sd.practica;

import java.awt.List;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class EVServerSocket implements Runnable{

	private final int port;
	private final Consumer<String> handler;
	private ServerSocket serverSocket;
	private ArrayList<EVClientHandler> clients = new ArrayList<>();
    public EVServerSocket(int port, Consumer<String> handler) {
        this.port = port;
		this.handler = handler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            while (!Thread.currentThread().isInterrupted()) {
            	try {
                    Socket clientSocket = serverSocket.accept();
                    EVClientHandler client = new EVClientHandler(clientSocket, handler);
                    Thread clientThread = new Thread(client);
                    clients.add(client);
                    clientThread.start();
                } catch (SocketException e) {
                    if (serverSocket.isClosed()) {
                        break;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                for (EVClientHandler client : clients) {
                    client.stop();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
