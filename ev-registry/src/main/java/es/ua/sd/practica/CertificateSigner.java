package es.ua.sd.practica;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.*;

public class CertificateSigner {
    private static final String PASS = "Practica2";

    public static X509Certificate firmarCertificadoCP(String csrPem, int dias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try(FileInputStream fis = new FileInputStream("ca_registry.p12")) { ks.load(fis, PASS.toCharArray()); }
        
        PrivateKey caKey = (PrivateKey) ks.getKey("evregistry_ca", PASS.toCharArray());
        X509Certificate caCert = (X509Certificate) ks.getCertificate("evregistry_ca");

        PemReader reader = new PemReader(new StringReader(csrPem));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(reader.readPemObject().getContent());
        
        PublicKey cpKey = new JcaPKCS10CertificationRequest(csr).setProvider("BC").getPublicKey();
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(cpKey.getEncoded());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
            .setProvider("BC").build(caKey);

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded()),
            BigInteger.valueOf(System.currentTimeMillis()),
            new Date(), new Date(System.currentTimeMillis() + (dias * 86400000L)),
            csr.getSubject(), keyInfo
        );

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
    }

    public static String codificarCertificadoAPEM(X509Certificate cert) throws Exception {
        StringWriter sw = new StringWriter();
        try(PemWriter pw = new PemWriter(sw)) { pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded())); }
        return sw.toString();
    }
}