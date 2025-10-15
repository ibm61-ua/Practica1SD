package es.ua.sd.practica;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;

public class CentralMonitorGUI extends JFrame {
	private JPanel panelPrincipal;
	private JPanel panelSecundario;

	public CentralMonitorGUI() {
		super("EVCharging Network - Central Monitor");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1290, 720);

		panelPrincipal = new JPanel();
		panelPrincipal.setLayout(new GridLayout(0, 6, 10, 10)); // 4 CPs por fila
		panelPrincipal.setPreferredSize(new Dimension(1290, 400));

		panelSecundario = new JPanel();
		panelSecundario.setLayout(new BoxLayout(panelSecundario, BoxLayout.X_AXIS));
		panelSecundario.setPreferredSize(new Dimension(1290, 270));

		this.add(panelPrincipal, BorderLayout.NORTH);
		this.add(panelSecundario, BorderLayout.SOUTH);
		setVisible(true);
	}

	public void addChargingPoint(String cpinfo) {
		String[] splited = cpinfo.split(";");
		JPanel cpPanel = new JPanel();
		cpPanel.setLayout(new BoxLayout(cpPanel, BoxLayout.Y_AXIS));
		cpPanel.setBorder(BorderFactory.createTitledBorder(splited[0]));
		cpPanel.setBackground(Color.GRAY); // Estado inicial: Desconectado

		JLabel label = new JLabel(splited[2]);
		label.setForeground(Color.WHITE);

		Font fuenteActual = label.getFont();
		Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
		label.setFont(fuenteNueva);

		cpPanel.add(label);

		label = new JLabel(splited[1] + "€/kWh");
		label.setForeground(Color.WHITE);

		label.setFont(fuenteNueva);
		cpPanel.add(label);

		panelPrincipal.add(cpPanel);
		this.revalidate();
		this.repaint();
	}

	public void OnGoingPanel(String rbuffer) {
		JPanel OGPanel = new JPanel();

		int nuevoTamano = 16;
		Font fuentePanel = new Font("SansSerif", Font.BOLD, nuevoTamano);
		TitledBorder bordeTitulado = BorderFactory.createTitledBorder("ON_GOING DRIVER REQUESTS");
		bordeTitulado.setTitleFont(fuentePanel);
		bordeTitulado.setTitleColor(Color.BLACK);
		OGPanel.setBorder(bordeTitulado);

		panelSecundario.add(OGPanel);
		this.revalidate();
		this.repaint();
	}

	public void MessagePanel(String mbuffer) {

		JPanel MPanel = new JPanel();
		int nuevoTamano = 16;
		Font fuentePanel = new Font("SansSerif", Font.BOLD, nuevoTamano);
		TitledBorder bordeTitulado = BorderFactory.createTitledBorder("APPLICATION MESSAGES");
		bordeTitulado.setTitleFont(fuentePanel);
		bordeTitulado.setTitleColor(Color.BLACK);
		MPanel.setBorder(bordeTitulado);

		panelSecundario.add(MPanel);
		this.revalidate();
		this.repaint();
	}
}
