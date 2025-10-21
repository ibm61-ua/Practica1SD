package es.ua.sd.practica;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class CentralMonitorGUI extends JFrame {
	private JPanel panelPrincipal;
	private JPanel panelSecundario;
	private JPanel MPanel = new JPanel();
	private JPanel RPanel = new JPanel();	
	private static ArrayList<CP> cps;
	
	public static ArrayList<String> mBuffer = new ArrayList<>();
	public static ArrayList<String> rBuffer = new ArrayList<>();

	public CentralMonitorGUI(ArrayList<CP> cps) {
		super("EVCharging Network - Central Monitor");

		this.cps = cps;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1290, 720);

		panelPrincipal = new JPanel();
		panelPrincipal.setLayout(new GridLayout(0, 6, 10, 10)); // 4 CPs por fila
		panelPrincipal.setPreferredSize(new Dimension(1290, 400));

		panelSecundario = new JPanel();
		panelSecundario.setLayout(new BorderLayout());
		panelSecundario.setPreferredSize(new Dimension(1290, 270));

		this.add(panelPrincipal, BorderLayout.NORTH);
		this.add(panelSecundario, BorderLayout.SOUTH);
		setVisible(true);
	}

	public void addChargingPoint(CP cp) {
		JPanel cpPanel = new JPanel();
		cpPanel.setLayout(new BoxLayout(cpPanel, BoxLayout.Y_AXIS));
		cpPanel.setBorder(BorderFactory.createTitledBorder(cp.UID));
		if(cp.State.equals("DESCONECTADO"))
			cpPanel.setBackground(Color.GRAY);
		if(cp.State.equals("CONECTADO") || cp.State.equals("CARGANDO"))
			cpPanel.setBackground(Color.GREEN);
		if(cp.State.equals("AVERIADO"))
			cpPanel.setBackground(Color.RED);
		if(cp.State.equals("PARADO"))
			cpPanel.setBackground(Color.ORANGE);

		JLabel label = new JLabel(cp.Location);
		label.setForeground(Color.WHITE);

		Font fuenteActual = label.getFont();
		Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
		label.setFont(fuenteNueva);

		cpPanel.add(label);

		label = new JLabel(cp.Price + "€/kWh");
		label.setForeground(Color.WHITE);

		label.setFont(fuenteNueva);
		cpPanel.add(label);
		
		
		if(cp.State.equals("CARGANDO"))
		{
			label = new JLabel("-------------------");
			label.setForeground(Color.WHITE);

			label.setFont(fuenteNueva);
			cpPanel.add(label);
			
			label = new JLabel(cp.driver);
			label.setForeground(Color.WHITE);

			label.setFont(fuenteNueva);
			cpPanel.add(label);
			
			label = new JLabel(String.valueOf(cp.KWHRequested));
			label.setForeground(Color.WHITE);

			label.setFont(fuenteNueva);
			cpPanel.add(label);
			
			String euros = String.valueOf(Float.parseFloat(cp.Price) * cp.KWHRequested);
			
			label = new JLabel(euros);
			label.setForeground(Color.WHITE);

			label.setFont(fuenteNueva);
			cpPanel.add(label);

		}
		
		if(cp.State.equals("CONECTADO"))
		{
			JButton StopButton = new JButton("STOP");
			StopButton.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
			        cp.State = "PARADO";
			        refreshChargingPoints();
			        MessagePanel(cp.UID + "#OFF");
			    }
			});
			
			cpPanel.add(StopButton);
		}
		else if(cp.State.equals("PARADO"))
		{
			JButton StartButton = new JButton("START");
			StartButton.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent e) {
			        cp.State = "CONECTADO";
			        refreshChargingPoints();
			        MessagePanel(cp.UID + "#ON");
			    }
			});
			
			cpPanel.add(StartButton);
		}
		

		panelPrincipal.add(cpPanel);
		this.revalidate();
		this.repaint();
	}
	
	public void deleteOnGoinMessage(String name)
	{
		for(String s : rBuffer)
		{
			if(s.contains(name))
			{
				int pos = rBuffer.indexOf(s);
				rBuffer.remove(pos);
				OnGoingPanel("");
			}
		}
	}

	public void OnGoingPanel(String rMessage) {
		RPanel.removeAll();
		RPanel.revalidate();
		RPanel.repaint();
		this.rBuffer.add(rMessage);
		int nuevoTamano = 16;
		Font fuentePanel = new Font("SansSerif", Font.BOLD, nuevoTamano);
		TitledBorder bordeTitulado = BorderFactory.createTitledBorder("ON_GOING DRIVER REQUESTS");
		bordeTitulado.setTitleFont(fuentePanel);
		bordeTitulado.setTitleColor(Color.BLACK);
		RPanel.setBorder(bordeTitulado);
		RPanel.setLayout(new BoxLayout(RPanel, BoxLayout.Y_AXIS));
		RPanel.setPreferredSize(new Dimension(610, 400));
		for (String m : rBuffer)
		{
			JLabel label = new JLabel(m);
			label.setForeground(Color.BLACK);

			Font fuenteActual = label.getFont();
			Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
			label.setFont(fuenteNueva);
			RPanel.add(label);
		}

		panelSecundario.add(RPanel, BorderLayout.EAST);
		this.revalidate();
		this.repaint();
	}

	public void MessagePanel(String mMessage) {
		MPanel.removeAll();
		MPanel.revalidate();
		MPanel.repaint();
		
		if( mMessage.contains("#"))
		{
			String[] split = mMessage.split("#");
			if(split[1].equals("OFF"))
			{
				this.mBuffer.add(split[0] + " out of order");
			}
			else
			{
				Iterator<String> it = mBuffer.iterator();
				while (it.hasNext()) {
				    String msg = it.next();
				    if (msg.equals(split[0] + " out of order")) {
				        it.remove();
				    }
				}
				
			}
		}
		
		int nuevoTamano = 16;
		Font fuentePanel = new Font("SansSerif", Font.BOLD, nuevoTamano);
		TitledBorder bordeTitulado = BorderFactory.createTitledBorder("APPLICATION MESSAGES");
		bordeTitulado.setTitleFont(fuentePanel);
		bordeTitulado.setTitleColor(Color.BLACK);
		MPanel.setBorder(bordeTitulado);
		MPanel.setPreferredSize(new Dimension(610, 400));
		
		for (String m : mBuffer)
		{
			JLabel label = new JLabel(m);
			label.setForeground(Color.BLACK);

			Font fuenteActual = label.getFont();
			Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
			label.setFont(fuenteNueva);
			MPanel.add(label);
		}
		panelSecundario.add(MPanel, BorderLayout.WEST);
		this.revalidate();
		this.repaint();
	}

	public void clearAllChargingPoints() {
		panelPrincipal.removeAll();
		panelPrincipal.revalidate();
	    panelPrincipal.repaint();
		
	}
	
	public void AddCPToGui() {
		for(CP cp : cps)
		{
			addChargingPoint(cp);
		}
		
	}
	
	public static void Serialize()
	{
		try (FileWriter fileWriter = new FileWriter("cpdatabase.txt", false); PrintWriter printWriter = new PrintWriter(fileWriter)) {

				for (CP cp : cps)
				{
					String str = cp.UID + ";" + cp.Price + ";" + cp.Location + ";" + cp.State;
		            printWriter.println(str);
				}
	        } catch (IOException e) {
	            System.err.println("Error al escribir en el archivo: " + e.getMessage());
	        }
	}
	
	public void refreshChargingPoints() {
		Serialize();
	    this.clearAllChargingPoints(); 
	    AddCPToGui(); 
	}
}
