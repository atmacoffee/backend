package com.atma.sensor_service.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MqttBrokerProperties {

    private final String brokerUrl;
    private final String host;
    private final int port;
    private final boolean useTls;
    private final String tlsCaFile;

    public MqttBrokerProperties(
            @Value("${mqtt.broker.url:}") String brokerUrl,
            @Value("${mqtt.host}") String host,
            @Value("${mqtt.port}") int port,
            @Value("${mqtt.use-tls}") boolean useTls,
            @Value("${mqtt.tls.ca-file:}") String tlsCaFile
    ) {
        this.brokerUrl = brokerUrl;
        this.host = host;
        this.port = port;
        this.useTls = useTls;
        this.tlsCaFile = tlsCaFile;
    }

    public String brokerUrl() {
        if (brokerUrl != null && !brokerUrl.isBlank()) {
            return brokerUrl;
        }
        String scheme = useTls ? "ssl" : "tcp";
        return scheme + "://" + host + ":" + port;
    }

    public boolean useTls() {
        return useTls;
    }

    public SSLSocketFactory sslSocketFactory() {
        if (!useTls || tlsCaFile == null || tlsCaFile.isBlank()) {
            return null;
        }
        try (InputStream inputStream = new FileInputStream(tlsCaFile)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate caCertificate = certificateFactory.generateCertificate(inputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("mqtt-ca", caCertificate);

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize MQTT TLS trust store", ex);
        }
    }
}
