package es.ua.sd.practica;

import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.bouncycastle.util.io.pem.PemReader;

import com.google.gson.Gson;
import okhttp3.*;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

class RegistroPeticion { 
    String id; String location; String price; String csrPem;
    public RegistroPeticion(String id, String location, String price, String csrPem) {
        this.id = id; this.location = location; this.price = price; this.csrPem = csrPem;
    }
    public RegistroPeticion(String id, String location, String price) {
        this.id = id; this.location = location; this.price = price; this.csrPem = null;
    }
}

class Key {
	public String key;
}

class RegistroRespuesta {
    public String status; public String id; public String certificate_pem; 
    public String message; public RegistroRespuesta() {}
}

public class MonitorGUI extends JFrame {
    private JPanel panelPrincipal;
    private JPanel panelBotones;
    private JTextArea panelLog;
    
    private static String API_REGISTRY; 
    private static String API_BAJA;
    private static String API_AUTH;
    
    private JButton alta, baja, autentication, editarcp;
    
    private static String name;
    private static String location; 
    private static String price;
    private static Gson GSON = new Gson();
    
    private static final String STORE_PASSWORD = "CP_Password_123"; 
    private static final String KEY_ALIAS = "cp_key";
    
    private static final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private static int portEngine;
    private static String ipEngine;
    
    private static SecretKey sessionKey;

    static {
        try {
            client = createSecureClient(); 
        } catch (Exception e) {
            throw new RuntimeException("Fallo al inicializar SSL.", e);
        }
    }

    private static OkHttpClient createSecureClient() throws Exception {
        final String TRUSTSTORE_FILE = "registry_ca.cer"; 
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509"); // Usamos proveedor default
        X509Certificate caCert;
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE_FILE)) {
            caCert = (X509Certificate) cf.generateCertificate(fis);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("registry_ca_root", caCert); 

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
            .hostnameVerifier((hostname, session) -> true)
            .build();
    }

    public MonitorGUI(String name, String EV_registry, String Central, String ipEngine, int portEngine) {
        super(name + " - Monitor");
        this.name = name;
        this.ipEngine = ipEngine;
        this.portEngine = portEngine;
        
        API_REGISTRY = "https://" + EV_registry + "/api/registry/register";
        API_BAJA = "https://" + EV_registry  + "/api/registry/delete";
        API_AUTH = "https://" + Central  + "/api/auth";
        
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1280, 720);

        panelPrincipal = new JPanel(new BorderLayout());
        initButtonPanel();
        
        panelLog = new JTextArea("Log del Monitor: " + name + "\n", 10, 50);
        panelLog.setFont(new Font("Monospaced", Font.PLAIN, 20));
        panelLog.setEditable(false); 
        panelPrincipal.add(new JScrollPane(panelLog), BorderLayout.CENTER);
        
        ActionListeners();
        this.add(panelPrincipal);
        setVisible(true);
    }
    
    private void initButtonPanel() {
        panelBotones = new JPanel();
        editarcp = new JButton("Editar CP"); editarcp.setPreferredSize(new Dimension(300, 150)); 
        alta = new JButton("Dar de Alta"); alta.setPreferredSize(new Dimension(300, 150)); 
        baja = new JButton("Dar de Baja"); baja.setPreferredSize(new Dimension(300, 150));
        autentication = new JButton("Autenticar en la Central"); autentication.setPreferredSize(new Dimension(300, 150)); 

        panelBotones.add(editarcp); panelBotones.add(alta);
        panelBotones.add(baja); panelBotones.add(autentication);
        panelPrincipal.add(panelBotones, BorderLayout.NORTH);
    }
    
    private void repaintButtonPanel() {
        panelBotones.removeAll();
        panelBotones.add(editarcp); panelBotones.add(alta);
        panelBotones.add(baja); panelBotones.add(autentication);
        panelBotones.revalidate(); panelBotones.repaint();
    }

    void ActionListeners() {
        editarcp.addActionListener(e -> {
            panelBotones.removeAll();
            panelBotones.add(crearPanelEdicion(name, location, price)); 
            panelBotones.revalidate(); panelBotones.repaint();
        });
        
        alta.addActionListener(e -> {
            if (location == null || price == null) {
                NewMessage("ERROR: Configura Location y Price primero.");
                return;
            }
            new Thread(() -> {
                try { enviarPeticionRegistro(); } 
                catch (Exception ex) { NewMessage("ERROR: " + ex.getMessage()); ex.printStackTrace(); }
            }).start();
        });
        
        baja.addActionListener(e -> {
            new Thread(this::enviarPeticionBaja).start();
        });
        
        autentication.addActionListener(e -> {
            new Thread(this::solicitarAuth).start();
        });
    }
    
    private JPanel crearPanelEdicion(String nombreCP, String loc, String pr) {
        JPanel p = new JPanel(new GridLayout(4, 2, 10, 10)); 
        JTextField locField = new JTextField(loc != null ? loc : "", 15);
        JTextField prField = new JTextField(pr != null ? pr : "", 15);
        JButton ok = new JButton("Aceptar");

        ok.addActionListener(e -> {
            location = locField.getText(); price = prField.getText();
            if(location.isEmpty() || price.isEmpty()) { NewMessage("Rellena los campos."); return; }
            repaintButtonPanel();
            NewMessage("Configurado: Loc=" + location + ", Price=" + price);
        });

        p.add(new JLabel("CP: " + nombreCP)); p.add(new JLabel("")); 
        p.add(new JLabel("Location:")); p.add(locField);
        p.add(new JLabel("Price:")); p.add(prField);
        p.add(new JLabel("")); p.add(ok); 
        return p;
    }
    
    void NewMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            panelLog.append(message + "\n");
            panelLog.setCaretPosition(panelLog.getDocument().getLength());
        });
    }
    
    public void enviarPeticionRegistro() throws Exception {
        KeyPair kp = generarParDeClaves(); 
        String csrPem = crearCSR(kp, name, location); 
        
        guardarClavePrivadaSegura(kp.getPrivate(), kp.getPublic(), name); 
        
        RegistroPeticion datos = new RegistroPeticion(name, location, price, csrPem);
        RequestBody body = RequestBody.create(GSON.toJson(datos), JSON);
        Request request = new Request.Builder().url(API_REGISTRY).put(body).build();

        try (Response response = client.newCall(request).execute()) {
            String jsonResponse = response.body().string(); 
            if (response.code() == 201) {
                RegistroRespuesta resp = GSON.fromJson(jsonResponse, RegistroRespuesta.class);
                X509Certificate certReal = decodificarPEM_a_Certificado(resp.certificate_pem);
                actualizarKeystoreConCadena(certReal); 
                NewMessage("Alta exitosa en la base de datos.");
            } else {
                 NewMessage("Error registro: " + response.code() + " " + jsonResponse);
            }
        }
    }

    private void actualizarKeystoreConCadena(X509Certificate certReal) throws Exception {
        KeyStore ks = cargarKeystoreExistente(name); 
        PrivateKey pk = (PrivateKey) ks.getKey(KEY_ALIAS, STORE_PASSWORD.toCharArray());
        
        KeyStore caTrust = createTrustStoreWithCARoot();
        X509Certificate caCert = (X509Certificate) caTrust.getCertificate("registry_ca_root");

        Certificate[] chain = new Certificate[]{ certReal, caCert };
        ks.setKeyEntry(KEY_ALIAS, pk, STORE_PASSWORD.toCharArray(), chain);
        guardarKeystoreModificado(ks, name);
    }
    
    public void enviarPeticionBaja() {
        RegistroPeticion datos = new RegistroPeticion(name, location, price);
        RequestBody body = RequestBody.create(GSON.toJson(datos), JSON);
        
        try {
            OkHttpClient mTLSClient = createMtlsClient(name, STORE_PASSWORD);
            Request request = new Request.Builder().url(API_BAJA).delete(body).build();

            try (Response response = mTLSClient.newCall(request).execute()) {
                if (response.code() == 200) {
                    NewMessage("Baja exitosa.");
                    eliminarKeystoreLocal(); 
                } else {
                     NewMessage("Error Baja: " + response.code() + " " + response.body().string());
                }
            } 
        } catch (Exception e) {
            NewMessage("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void solicitarAuth() {
        RegistroPeticion datos = new RegistroPeticion(name, location, price);
        RequestBody body = RequestBody.create(GSON.toJson(datos), JSON);
        
        try {
            OkHttpClient mTLSClient = createMtlsClient(name, STORE_PASSWORD);
            Request request = new Request.Builder().url(API_AUTH).put(body).build();

            try (Response response = mTLSClient.newCall(request).execute()) {
                if (response.code() == 200) {
                	String jsonResponse = response.body().string();
                    NewMessage("Autenticación exitosa.");
                    Key k = GSON.fromJson(jsonResponse, Key.class);
                    sessionKey = CryptoUtils.stringToKey(k.key);
                    NewMessage("[Monitor] Guarda clave de crifrado.");
                    EVClient cpClient = new EVClient(ipEngine, portEngine);
                    boolean isConnected = cpClient.startConnection();
                    if(isConnected)
                    {
                    	String statusRequest = "KEY#"+k.key;
                    	cpClient.sendMessage(statusRequest); 
                    }
                } else {
                    NewMessage("Error Autenticación: " + response.code() + " " + response.body().string());
                }
            } 
        } catch (Exception e) {
            NewMessage("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private OkHttpClient createMtlsClient(String cpName, String password) throws Exception {
        final String TRUSTSTORE_FILE = "registry_ca.cer"; 
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE_FILE)) {
            caCert = (X509Certificate) cf.generateCertificate(fis);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("registry_ca_root", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        KeyStore clientKs = cargarKeystoreExistente(cpName);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(clientKs, password.toCharArray()); 
        
        SSLContext sslContext = SSLContext.getInstance("TLS"); 
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return client.newBuilder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)tmf.getTrustManagers()[0])
            .hostnameVerifier((hostname, session) -> true)
            .build();
    }


    public KeyPair generarParDeClaves() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(256); 
        return keyGen.generateKeyPair();
    }
    
    public String crearCSR(KeyPair kp, String id, String loc) throws Exception {
        PublicKey pk = kp.getPublic();
        SubjectPublicKeyInfo pkInfo = SubjectPublicKeyInfo.getInstance(pk.getEncoded());
        X500Name subject = new X500Name("CN=" + id + ",L=" + loc + ",O=EVChargingCompany");
        
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC").build(kp.getPrivate());
        
        PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(subject, pkInfo); 
        return codificarCSRA_PEM(builder.build(signer)); 
    }
    
    public String codificarCSRA_PEM(PKCS10CertificationRequest csr) throws Exception {
        StringWriter sw = new StringWriter();
        try (PemWriter pw = new PemWriter(sw)) { pw.writeObject(new PemObject("CERTIFICATE REQUEST", csr.getEncoded())); }
        return sw.toString();
    }

    public void guardarClavePrivadaSegura(PrivateKey pk, PublicKey pub, String name) throws Exception {
        Certificate[] dummy = crearCertificadoDummy(pk, pub, name); 
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null); 
        ks.setKeyEntry(KEY_ALIAS, pk, STORE_PASSWORD.toCharArray(), dummy);
        try (FileOutputStream fos = new FileOutputStream(name + "_cp.p12")) {
            ks.store(fos, STORE_PASSWORD.toCharArray());
        }
    }

    private Certificate[] crearCertificadoDummy(PrivateKey pk, PublicKey pub, String name) throws Exception {
        SubjectPublicKeyInfo pkInfo = SubjectPublicKeyInfo.getInstance(pub.getEncoded());
        X500Name subject = new X500Name("CN=" + name); 
        
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider("BC").build(pk); 

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            subject, BigInteger.valueOf(System.currentTimeMillis()), 
            new Date(), new Date(System.currentTimeMillis() + 86400000L), subject, pkInfo);

        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
        return new Certificate[] { cert };
    }

    private static KeyStore createTrustStoreWithCARoot() throws Exception {
        final String TRUSTSTORE_FILE = "registry_ca.cer"; 
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE_FILE)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("registry_ca_root", cf.generateCertificate(fis));
            return ks;
        }
    }
    
    public KeyStore cargarKeystoreExistente(String name) throws Exception {
        try (FileInputStream fis = new FileInputStream(name + "_cp.p12")) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, STORE_PASSWORD.toCharArray());
            return ks;
        }
    }
    
    public void guardarKeystoreModificado(KeyStore ks, String name) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(name + "_cp.p12")) {
            ks.store(fos, STORE_PASSWORD.toCharArray());
        }
    }
    
    public X509Certificate decodificarPEM_a_Certificado(String pem) throws Exception {
        String clean = pem.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\", "").trim();
        int start = clean.indexOf("-----BEGIN CERTIFICATE-----");
        if (start == -1) return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(pem.getBytes()));
        String block = clean.substring(start, clean.indexOf("-----END CERTIFICATE-----") + "-----END CERTIFICATE-----".length());
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(new PemReader(new StringReader(block)).readPemObject().getContent()));
    }
    
    private void eliminarKeystoreLocal() {
        File ksFile = new File(name + "_cp.p12");
        if (ksFile.exists()) {
            ksFile.delete();
            NewMessage("[MONITOR] Archivo .p12 local eliminado.");
        }
    }
}

