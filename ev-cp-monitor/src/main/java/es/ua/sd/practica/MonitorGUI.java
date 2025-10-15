package es.ua.sd.practica;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;

public class MonitorGUI extends JFrame {
	private JPanel panelPrincipal;

	public MonitorGUI(String name) {
		super(name + " - Monitor");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1290, 720);

		panelPrincipal = new JPanel();
		panelPrincipal.setLayout(new GridLayout(0, 2, 10, 10));
		panelPrincipal.setPreferredSize(new Dimension(1290, 720));

		this.add(panelPrincipal);
		setVisible(true);

	}

	void Message(String message) {
		JPanel MPanel = new JPanel();
		MPanel.setLayout(new BoxLayout(MPanel, BoxLayout.Y_AXIS));
		MPanel.setBorder(BorderFactory.createTitledBorder("Status"));
		MPanel.setBackground(Color.WHITE);
		JLabel label = new JLabel(message);
		label.setForeground(Color.BLACK);

		Font fuenteActual = label.getFont();
		Font fuenteNueva = new Font(fuenteActual.getName(), fuenteActual.getStyle(), 16);
		label.setFont(fuenteNueva);

		MPanel.add(label);

		panelPrincipal.add(MPanel);
		this.revalidate();
		this.repaint();
	}

	void ButtonPressed() {
		JPanel MPanel = new JPanel();
		MPanel.setLayout(new BoxLayout(MPanel, BoxLayout.Y_AXIS));
		MPanel.setBorder(BorderFactory.createTitledBorder("Status"));
		MPanel.setBackground(Color.WHITE);

		panelPrincipal.add(MPanel);
		this.revalidate();
		this.repaint();
	}
}
