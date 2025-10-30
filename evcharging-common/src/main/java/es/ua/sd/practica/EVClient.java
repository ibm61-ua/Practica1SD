package es.ua.sd.practica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class EVClient {
    
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String serverIp;
    private final int serverPort;
    
    public EVClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public boolean startConnection() {
        try {
            clientSocket = new Socket(serverIp, serverPort);
            
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            System.out.println("Cliente conectado al CP en " + serverIp + ":" + serverPort);
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Host desconocido: " + serverIp);
            return false;
        } catch (IOException e) {
            System.err.println("No se pudo establecer la conexión con el servidor CP.");
            return false;
        }
    }

    public String sendMessage(String msg) {
        if (out == null) {
            System.err.println("Error: Conexión no iniciada.");
            return null;
        }
        try {
            out.println(msg);
            return in.readLine();
        } catch (IOException e) {
            System.err.println("Error de I/O al enviar/recibir mensaje: " + e.getMessage());
            return null;
        }
    }
    
    public void sendOnly(String msg) throws IOException {
        if (out == null) {
            throw new IOException("Error: Conexión no iniciada.");
        }

        out.println(msg);
        if (out.checkError()) {
            throw new IOException("Error al enviar mensaje, conexión posiblemente cerrada.");
        }
    }

    public void stopConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
        }
    }
}