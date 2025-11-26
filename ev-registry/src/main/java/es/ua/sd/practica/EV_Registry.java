package es.ua.sd.practica;

import com.google.gson.Gson;
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

import javax.servlet.http.HttpServletRequest;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import static spark.Spark.*;

class RegistroPeticion { 
    String id; String location; String price; String csrPem; 
}

public class EV_Registry {

    private static int API_PORT;
    private static String databaseIP;
    private static final Gson GSON = new Gson();
    private static final String KEYSTORE_FILE = "ca_registry.p12";
    private static final String KEYSTORE_PASS = "Practica2";

    public static void main(String[] args) {
    	
    	if (args.length < 2) return;
    	
    	API_PORT = Integer.parseInt(args[0]);
    	databaseIP = args[1];
    	
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Spark.stop();

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
            
            System.out.println("Archivo cargado correctamente.");
            
            boolean tienePrivada = false;
            boolean tieneConfianza = false;
            
            for (String alias : Collections.list(ksCargado.aliases())) {
                boolean isKey = ksCargado.isKeyEntry(alias);
                boolean isCert = ksCargado.isCertificateEntry(alias);
                System.out.println("   -> Alias: '" + alias + "' [Privada: " + isKey + ", Confianza: " + isCert + "]");
                
                if (isKey) tienePrivada = true;
                if (isCert) tieneConfianza = true;
            }
            
            if (tienePrivada && tieneConfianza) {
                System.out.println("KEYSTORE CORRECTO");
            } else if (!tieneConfianza) {
                System.err.println("DIAGNÓSTICO: FALTA LA CA DE CONFIANZA. El servidor rechazará todos los clientes.");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR CARGANDO KEYSTORE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("==========================================\n");

        port(API_PORT);
        
        final KeyStore keyStoreFinal = ksCargado;

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
                sslContextFactory.setNeedClientAuth(false); 
                
                sslContextFactory.setEndpointIdentificationAlgorithm(null);

                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.addCustomizer(new SecureRequestCustomizer()); 

                ServerConnector sslConnector = new ServerConnector(server,new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpConfig));                
                sslConnector.setPort(API_PORT);
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
        
        configurarEndpointRegistro(GSON);
        configurarEndpointBaja(GSON);

        Spark.init();
        
        System.out.println("EV_Registry iniciado correctamente...");
    }

    
    public static void configurarEndpointRegistro(Gson gson) {
        put("/api/registry/register", (request, response) -> {
             response.type("application/json");
             String jsonBody = request.body();
             RegistroPeticion registroData = gson.fromJson(jsonBody, RegistroPeticion.class);
             
             boolean exito = registrarNuevoCP(registroData.id, registroData.location, registroData.price);
             
             if (exito) {
                 try {
                     X509Certificate certificadoFirmado = CertificateSigner.firmarCertificadoCP(registroData.csrPem, 365);
                     String certPem = CertificateSigner.codificarCertificadoAPEM(certificadoFirmado);
                     response.status(201);
                     return gson.toJson(Map.of("status", "SUCCESS", "id", registroData.id, "certificate_pem", certPem));
                 } catch (Exception e) {
                     response.status(500);
                     return gson.toJson(Map.of("status", "ERROR", "message", e.getMessage()));
                 }
             }
             response.status(409);
             return "{}";
        });
    }

    public static void configurarEndpointBaja(Gson gson) {
        delete("/api/registry/delete", (request, response) -> {
            response.type("application/json");

            X509Certificate cpCert = getCertificateFromRequest(request); 
            
            if (cpCert == null) {
                System.out.println("Baja rechazada: No hay certificado.");
                response.status(401); 
                return gson.toJson(Map.of("status", "ERROR", "message", "Certificado requerido."));
            }

            String cpIdFromCert = parseCNFromSubject(cpCert.getSubjectX500Principal().getName());
            String jsonBody = request.body();
            RegistroPeticion registroData = gson.fromJson(jsonBody, RegistroPeticion.class);

            if (cpIdFromCert == null || !cpIdFromCert.equals(registroData.id)) {
                response.status(403); 
                return gson.toJson(Map.of("status", "ERROR", "message", "ID no coincide."));
            }
            
            boolean exito = eliminarCP(registroData.id, registroData.location, registroData.price);
            if (exito) {
                response.status(200);
                return gson.toJson(Map.of("status", "SUCCESS", "id", registroData.id));
            }
            response.status(404);
            return "{}";
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

    private static boolean registrarNuevoCP(String id, String loc, String price) 
    { 
    	DatabaseManager dbm = new DatabaseManager(databaseIP, "evcharging_db", "evcharging", "practica2");
    	dbm.createTable(); // crea si no existe la tabla
    	for (String cp : dbm.GetAllCPS())
    	{
    		System.out.println(cp);
    	}
    	return dbm.InsertCP(id, loc, Double.parseDouble(price), "DESCONECTADO"); 
    	
    }
    private static boolean eliminarCP(String id, String loc, String price) 
    { 
    	DatabaseManager dbm = new DatabaseManager(databaseIP, "evcharging_db", "evcharging", "practica2");
    	return dbm.DeleteCP(id); 
    }
}