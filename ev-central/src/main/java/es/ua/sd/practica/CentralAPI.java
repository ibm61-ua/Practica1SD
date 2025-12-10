package es.ua.sd.practica;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import spark.Service;
import spark.Spark;
import spark.embeddedserver.EmbeddedServers;
import spark.embeddedserver.jetty.EmbeddedJettyFactory;
import spark.embeddedserver.jetty.JettyServerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.thread.ThreadPool;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;

import static spark.Spark.*;

class Peticion {
    String id; String location; String price; String csrPem;
}

public class CentralAPI extends EV_Central implements Runnable {
    private static final Gson GSON = new Gson();
    private static final String KEYSTORE_FILE = "ca_registry.p12";
    private static final String KEYSTORE_PASS = "Practica2";

    @Override
    public void run() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        Spark.stop();

        Service publicService = Service.ignite();
        publicService.port(API_PORT);

        publicService.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        });

        publicService.get("/api/status/cp", (request, response) -> {
            response.type("application/json");
            String jsonStatus = getSystemStatusAsJson(); 
            return jsonStatus;
        });
        
        publicService.get("/api/status/driver", (request, response) -> {
            response.type("application/json");
            return GSON.toJson(super.gui.ongoingBuffer);
        });
        
        publicService.get("/api/status/log", (request, response) -> {
            response.type("application/json");
            String jsonStatus = RegistroDeAuditoria.LogToJson(); 
            return jsonStatus;
        });
        
        publicService.post("/api/alert", (request, response) -> {
            response.type("application/json");
            
            String body = request.body();
            HandleAlert(body);
            return GSON.toJson(Map.of("status", "RECEIVED"));
        });

        KeyStore ksCargado = null;
        try {
            File f = new File(KEYSTORE_FILE);
            if (!f.exists()) {
                System.err.println("ERROR: El archivo no existe.");
                System.exit(1);
            }
            ksCargado = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(f)) {
                ksCargado.load(fis, KEYSTORE_PASS.toCharArray());
            }
            RegistroDeAuditoria.NewLog(super.IP_DATABASE, "Carga Keystore correcta", "Archivo Keystore cargado correctamente.");
            
            boolean tieneConfianza = false;
            for (String alias : Collections.list(ksCargado.aliases())) {
                if (ksCargado.isCertificateEntry(alias)) tieneConfianza = true;
            }
            if (!tieneConfianza) System.err.println("DIAGNÓSTICO: FALTA LA CA DE CONFIANZA.");

        } catch (Exception e) {
            System.err.println("ERROR CARGANDO KEYSTORE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        final KeyStore keyStoreFinal = ksCargado;

        port(API_AUTH); 

        JettyServerFactory jettyServerFactory = new JettyServerFactory() {
            @Override
            public Server create(ThreadPool threadPool) {
                Server server = threadPool != null ? new Server(threadPool) : new Server();

                @SuppressWarnings("deprecation")
                SslContextFactory sslContextFactory = new SslContextFactory();

                sslContextFactory.setKeyStore(keyStoreFinal);
                sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);
                
                sslContextFactory.setTrustStore(keyStoreFinal);
                sslContextFactory.setTrustStorePassword(KEYSTORE_PASS); 

                sslContextFactory.setWantClientAuth(true);
                sslContextFactory.setNeedClientAuth(true);
                
                sslContextFactory.setEndpointIdentificationAlgorithm(null);

                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.addCustomizer(new SecureRequestCustomizer()); 

                ServerConnector sslConnector = new ServerConnector(server,
                        new SslConnectionFactory(sslContextFactory, "http/1.1"), 
                        new HttpConnectionFactory(httpConfig));                
                sslConnector.setPort(API_AUTH);
                server.addConnector(sslConnector);

                return server;
            }

            @Override
            public Server create(int maxThreads, int minThreads, int threadTimeoutMillis) {
                return create(null);
            }
        };

        EmbeddedServers.add(EmbeddedServers.Identifiers.JETTY, (routes, staticFiles, exceptionMapper, hasMultipleHandler) -> {
            return new EmbeddedJettyFactory(jettyServerFactory).create(routes, staticFiles, exceptionMapper, hasMultipleHandler);
        });

        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        
        configurarEndpointAuteticacion(GSON);
        
        Spark.init();
    }
    
  

    private void HandleAlert(String body) {
    	try {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> alerta = GSON.fromJson(body, type);

            String cpId = alerta.get("cp_id");
            String message = alerta.get("message");
            String severity = alerta.get("severity");
            String temp = alerta.get("temp");

            String logMessage;
            

            if (cpId != null) {
                for (CP cp : cps) {
                    if (cp.UID.equals(cpId)) {
                    	cp.temperature = Double.parseDouble(temp);
                    	if ("HIGH".equals(severity)) {
                            logMessage = "[EV_w] CP: "+cpId + " " + message;
                            RegistroDeAuditoria.NewLog(super.IP_DATABASE, "ALERT", logMessage);
                        } else if ("INFO".equals(severity)) {
                            logMessage = "[EV_w] CP: "+cpId + " "+ message;
                            RegistroDeAuditoria.NewLog(super.IP_DATABASE, "INFO", logMessage);
                        }

                        
                        if ("HIGH".equals(severity) && !"DESCONECTADO".equals(cp.State) && !"CARGANDO".equals(cp.State))  {
                            cp.State = "PARADO"; 
                        } else if ("INFO".equals(severity)) {
                            if ("PARADO".equals(cp.State)) {
                                cp.State = "CONECTADO";
                            }
                        }
                        gui.refreshChargingPoints();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
	}

	private void configurarEndpointAuteticacion(Gson gson) {
        put("/api/auth", (request, response) -> {
            response.type("application/json");

            X509Certificate cpCert = getCertificateFromRequest(request); 
            
            if (cpCert == null) {
            	Peticion registroData = gson.fromJson(request.body(), Peticion.class);
            	RegistroDeAuditoria.NewLog(request.ip(), "Auth",  "Autenticación rechazada: No hay certificado. CP:" + registroData.id);
                response.status(401); 
                return gson.toJson(Map.of("status", "ERROR", "message", "Certificado requerido."));
            }

            String cpIdFromCert = parseCNFromSubject(cpCert.getSubjectX500Principal().getName());
            String jsonBody = request.body();
            Peticion registroData = gson.fromJson(jsonBody, Peticion.class);

            if (cpIdFromCert == null || !cpIdFromCert.equals(registroData.id)) {
                response.status(403);
                RegistroDeAuditoria.NewLog(request.ip(), "Auth",  "Autenticación rechazada: ID no coindice del certificado no coincide.");
                return gson.toJson(Map.of("status", "ERROR", "message", "ID no coincide."));
            }
            
            for (CP cp : cps) {
                if (cp.UID.equals(registroData.id)) {
                    cp.autenticado = true;
                }
            }
            
            String clave = CryptoUtils.generarClave();
            SecretKey keyObj = CryptoUtils.stringToKey(clave);
            CPKeys.put(registroData.id, keyObj);
            
            response.status(200);
            RegistroDeAuditoria.NewLog(request.ip(), "Auth",  "[Auth] Autenticación aceptada CP:" + registroData.id);
            return gson.toJson(Map.of("status", "SUCCESS", "id", registroData.id, "key", clave));
        });
    }
    
    public static X509Certificate getCertificateFromRequest(spark.Request request) {
        if (request.raw() instanceof HttpServletRequest) {
            Object certsAttr = request.raw().getAttribute("javax.servlet.request.X509Certificate");
            if (certsAttr != null && certsAttr instanceof X509Certificate[]) {
                return ((X509Certificate[]) certsAttr)[0];
            }
        }
        return null;
    }
    
    public static String parseCNFromSubject(String subjectDn) {
        if (subjectDn == null) return null;
        int cnIndex = subjectDn.indexOf("CN=");
        if (cnIndex == -1) return null;
        int startIndex = cnIndex + 3; 
        int endIndex = subjectDn.indexOf(",", startIndex);
        return (endIndex == -1) ? subjectDn.substring(startIndex).trim() : subjectDn.substring(startIndex, endIndex).trim();
    }
}