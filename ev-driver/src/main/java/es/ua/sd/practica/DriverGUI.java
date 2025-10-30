package es.ua.sd.practica;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class DriverGUI extends JFrame {
	private JPanel panelPrincipal;
	private JPanel panelSecundario;
	JPanel Log = new JPanel();
	private JLabel labelCentralStatus;
	private Producer producer;
	private String name;
	public boolean Suministrando = false;
	public static ArrayList<CP> cps = new ArrayList<>();
	public static boolean Central_Status = false;
	
	public ArrayList<String> logBuffer = new ArrayList<>();
	public DriverGUI(String name, Producer p) {
		super(name + " - Driver");
		producer = p;
		this.name = name;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1290, 720);

		panelPrincipal = new JPanel();
		panelPrincipal.setLayout(new GridLayout(0, 2, 10, 10));
		panelPrincipal.setPreferredSize(new Dimension(1290, 300));
		panelPrincipal.setBorder(BorderFactory.createTitledBorder("Puntos de recarga"));
		
		Log.setBorder(BorderFactory.createTitledBorder("Log"));
		Log.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		panelSecundario = new JPanel();
		panelSecundario.setLayout(new BorderLayout());
		panelSecundario.setPreferredSize(new Dimension(1290, 270));
		panelSecundario.setBorder(BorderFactory.createTitledBorder("Driver"));
		
        labelCentralStatus = new JLabel("Central: DESCONECTADA");
        labelCentralStatus.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelCentralStatus.setForeground(Color.RED);
        labelCentralStatus.setHorizontalAlignment(JLabel.CENTER);

        panelSecundario.add(labelCentralStatus, BorderLayout.NORTH);

		this.add(panelPrincipal, BorderLayout.NORTH);
		this.add(panelSecundario, BorderLayout.SOUTH);
		JButton ScritpDeSuministro = new JButton("Ejecutar Script de suministro");
		ScritpDeSuministro.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	if (!Central_Status) return;
		        new Thread(() -> {
		            try {
		                InitCPS();
		                File archivo = new File("request_script.txt");
		                Scanner s = new Scanner(archivo);
		                while (s.hasNextLine()) {
		                    if (Suministrando) continue;
		                    String linea = s.nextLine();
		                    for (CP cp : cps) {
		                        if (cp.UID.equals(linea.split("#")[1])) {
		                            Suministrando = true;
		                            String request = "REQUEST#" + cp.UID + ";" + cp.Price + ";" + cp.Location + "#" + linea.split("#")[2] + "#" + name + "#" + cp.Price;
		                            producer.sendMessage(request);
		                            javax.swing.SwingUtilities.invokeLater(() -> Log("Solicitando Recarga..."));
		                        }
		                    }
		                    Thread.sleep(4000);
		                }
		                s.close();
		            } catch (Exception ex) {
		                ex.printStackTrace();
		            }
		        }).start();
		    }

			
		});
		
		JButton Reload = new JButton("Actualizar CPS");
		Reload.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	producer.sendMessage("RELOAD");
		    }

			
		});
		panelSecundario.add(ScritpDeSuministro, BorderLayout.WEST);
		panelSecundario.add(Reload, BorderLayout.CENTER);
		setVisible(true);
	}
	
	public void updateCentralStatus(boolean status) {
        Central_Status = status;
        SwingUtilities.invokeLater(() -> {
            if (status) {
                labelCentralStatus.setText("Central: CONECTADA");
                labelCentralStatus.setForeground(new Color(0, 180, 0)); // verde
            } else {
                labelCentralStatus.setText("Central: DESCONECTADA");
                labelCentralStatus.setForeground(Color.RED);
            }
            labelCentralStatus.repaint();
        });
    }

	
	private void InitCPS() {
		try {
            File archivo = new File("cpdatabase.txt");
            Scanner s = new Scanner(archivo);

            while (s.hasNextLine()) {
                String linea = s.nextLine();
                String[] splited = linea.split(";");
                CP cp = new CP(splited[0], splited[1], splited[2], "DESCONECTADO");
                cps.add(cp);
            }

            s.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo.");
            e.printStackTrace();
        }
	}
	
	public void CreateButtons(String path)
	{
		panelPrincipal.removeAll();
		panelPrincipal.revalidate();
	    panelPrincipal.repaint();
	    
		try {
            File archivo = new File("cpdatabase.txt");
            Scanner s = new Scanner(archivo);

            while (s.hasNextLine()) {
                String linea = s.nextLine();
                JButton btnIniciarRecarga = new JButton("Iniciar Recarga en " + linea.split(";")[0]);
                CreateButton(btnIniciarRecarga, linea);
            }

            s.close();
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo.");
            e.printStackTrace();
        }
		
	}
	
	public void CreateButton(JButton button, String linea)
	{
		button.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	if (!Central_Status) return;
		        if(Suministrando) return;
		        String mensajeKafka = "REQUEST#" + linea + "#10.0#" + name;
		        producer.sendMessage(mensajeKafka);
		        Log("Solicitando Recarga...");
		        Suministrando = true;
		    }
		});
		
		panelPrincipal.add(button);
		this.revalidate();
		this.repaint();
	}


	public void Log(String message) {
		Log.removeAll();
		if(logBuffer.size() > 8)
		{
			logBuffer.remove(0);
		}
		logBuffer.add(message);

		Log.setLayout(new BoxLayout(Log, BoxLayout.Y_AXIS));
		Log.setPreferredSize(new Dimension(610, 400));
		for(String m : logBuffer)
		{
			JLabel label = new JLabel(m);
			label.setForeground(Color.BLACK);
			Font fuenteActual = label.getFont();
			Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
			label.setFont(fuenteNueva);

			Log.add(label);
		}
		
		
		panelSecundario.add(Log, BorderLayout.EAST);
		this.revalidate();
		this.repaint();
	}
	
}
