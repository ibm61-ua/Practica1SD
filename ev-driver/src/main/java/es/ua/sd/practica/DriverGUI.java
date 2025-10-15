package es.ua.sd.practica;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class DriverGUI extends JFrame{
	private JPanel panelPrincipal;

	public DriverGUI(String name) {
		super(name + " - Driver");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1290, 720);

		panelPrincipal = new JPanel();
		panelPrincipal.setLayout(new GridLayout(0, 2, 10, 10));
		panelPrincipal.setPreferredSize(new Dimension(1290, 720));

		this.add(panelPrincipal);
		setVisible(true);
	}
	
	
	public void CreateButtons(String path)
	{
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
		    // Este método se ejecuta al pulsar el botón
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        
		        System.out.println("Botón pulsado. Iniciando transacción...");
		        
		        // ⬅️ LÓGICA CLAVE: Aquí es donde iría tu código para enviar el mensaje Kafka:
		        
		        // 1. Obtener la cantidad de kWh de un campo de texto (ej. txtKWh.getText())
		        // String kWhSolicitados = obtenerKWh(); 
		        
		        // 2. Formatear el mensaje
		        String mensajeKafka = "REQUEST#DRV007#" + linea.split(":")[0] + "#10.0";
		        
		        // 3. Usar tu objeto Producer de Kafka
		        // producer.sendMessage(mensajeKafka); 
		        
		        // Opcional: Deshabilitar el botón mientras se espera la respuesta de la Central
		        // btnIniciarRecarga.setEnabled(false); 
		    }
		});
		
		panelPrincipal.add(button);
		this.revalidate();
		this.repaint();
	}
	
	
	
	
}
