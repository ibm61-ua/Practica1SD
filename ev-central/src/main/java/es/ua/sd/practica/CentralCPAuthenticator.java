package es.ua.sd.practica;

import com.google.gson.Gson;
import spark.Service; // IMPORTANTE: Necesario para crear el segundo servidor
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

import static spark.Spark.*;

class Peticion {
    String id; String location; String price; String csrPem;
}

public class CentralCPAuthenticator extends EV_Central implements Runnable {
    private static final Gson GSON = new Gson();
    private static final String KEYSTORE_FILE = "ca_registry.p12";
    private static final String KEYSTORE_PASS = "Practica2";

    // DEFINICIÓN DE PUERTOS SEPARADOS
    private static final int PORT_MTLS = 8082;   // Seguro para CPs
    private static final int PORT_PUBLIC = 3000; // Público para Front-end

    @Override
    public void run() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        // Detenemos cualquier instancia previa de Spark
        Spark.stop();

        // ========================================================================
        // 1. SERVIDOR PÚBLICO (HTTP - Puerto 3000)
        //    Para el Front-end (JavaScript). Sin certificados, sin errores SSL.
        // ========================================================================
        Service publicService = Service.ignite();
        publicService.port(PORT_PUBLIC);

        // Configuración CORS para el Front-end
        publicService.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        });

        // Endpoint de Estado (accesible vía HTTP plano)
        publicService.get("/api/status/all", (request, response) -> {
            response.type("application/json");
            String jsonStatus = getSystemStatusAsJson(); 
            System.out.println("HTTP (Público 3000): Estado enviado al Front-end.");
            return jsonStatus;
        });
        
        publicService.post("/api/alert", (request, response) -> {
            response.type("application/json");
            
            // 1. Recibir el JSON
            String body = request.body();
            System.out.println("⚠️ ALERTA RECIBIDA DE EV_W: " + body);
            

            // 3. Responder OK
            return GSON.toJson(Map.of("status", "RECEIVED"));
        });

        System.out.println("✅ [Central] API Pública (HTTP) lista en: http://localhost:" + PORT_PUBLIC);

        // ========================================================================
        // 2. SERVIDOR SEGURO (HTTPS/mTLS - Puerto 8082)
        //    Para los CPs. Requiere autenticación fuerte.
        // ========================================================================

        // --- Carga del Keystore ---
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
            System.out.println("Archivo Keystore cargado correctamente.");
            
            // Diagnóstico rápido
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

        // --- Configuración de Spark/Jetty Seguro ---
        port(PORT_MTLS); // Usamos la instancia estática de Spark para el puerto seguro

        JettyServerFactory jettyServerFactory = new JettyServerFactory() {
            @Override
            public Server create(ThreadPool threadPool) {
                Server server = threadPool != null ? new Server(threadPool) : new Server();

                @SuppressWarnings("deprecation")
                SslContextFactory sslContextFactory = new SslContextFactory();

                // Identidad del Servidor (HTTPS)
                sslContextFactory.setKeyStore(keyStoreFinal);
                sslContextFactory.setKeyStorePassword(KEYSTORE_PASS);
                
                // Confianza del Servidor (mTLS - Verificar CPs)
                sslContextFactory.setTrustStore(keyStoreFinal);
                sslContextFactory.setTrustStorePassword(KEYSTORE_PASS); 

                // CONFIGURACIÓN ESTRICTA PARA CPs (Puerto 8082)
                sslContextFactory.setWantClientAuth(true);
                sslContextFactory.setNeedClientAuth(true); // OBLIGATORIO: Solo CPs con certificado entran aquí
                
                sslContextFactory.setEndpointIdentificationAlgorithm(null);

                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.addCustomizer(new SecureRequestCustomizer()); 

                ServerConnector sslConnector = new ServerConnector(server,
                        new SslConnectionFactory(sslContextFactory, "http/1.1"), 
                        new HttpConnectionFactory(httpConfig));                
                sslConnector.setPort(PORT_MTLS);
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

        // CORS para la parte segura (por si acaso)
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
        
        // Endpoint de Autenticación (Solo en puerto 8082 seguro)
        configurarEndpointAuteticacion(GSON);
        
        // Iniciamos el servidor seguro
        Spark.init();
        System.out.println("🔒 [Central] API Segura (mTLS) lista en: https://localhost:" + PORT_MTLS);
    }
    
  

    private void configurarEndpointAuteticacion(Gson gson) {
        put("/api/auth", (request, response) -> {
            response.type("application/json");

            X509Certificate cpCert = getCertificateFromRequest(request); 
            
            if (cpCert == null) {
                System.out.println("Autenticación rechazada: No hay certificado.");
                response.status(401); 
                return gson.toJson(Map.of("status", "ERROR", "message", "Certificado requerido."));
            }

            String cpIdFromCert = parseCNFromSubject(cpCert.getSubjectX500Principal().getName());
            String jsonBody = request.body();
            Peticion registroData = gson.fromJson(jsonBody, Peticion.class);

            if (cpIdFromCert == null || !cpIdFromCert.equals(registroData.id)) {
                response.status(403); 
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