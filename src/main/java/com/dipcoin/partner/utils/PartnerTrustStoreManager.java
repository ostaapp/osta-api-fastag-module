package com.dipcoin.partner.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dipcoin.commons.HttpsUtils;
import com.dipcoin.commons.PartnerSecret;

@Service("partnerTrustStoreManager")
public class PartnerTrustStoreManager {

  private static final Logger LOG = LogManager.getLogger(PartnerTrustStoreManager.class);
  public static String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
  public static String END_CERTIFICATE = "-----END CERTIFICATE-----";

  @Autowired
  private PartnerKeyStoreManager partnerKeyStoreManager;

  private final AtomicReference<KeyStore> trustStore = new AtomicReference<>();
  private String TruststoreType = "PKCS12";

  public boolean healthcheck(PartnerRequestContext requestContext, String partnerReferenceId,
      String healthcheckApi) throws IOException {
    CloseableHttpClient httpClient = null;
    CloseableHttpResponse response = null;
    try {
      httpClient = HttpClients.custom()
          .setSSLSocketFactory(getSSLConnectionSocketFactory(requestContext, partnerReferenceId))
          .build();
      HttpGet request = new HttpGet(healthcheckApi);

      response = httpClient.execute(request);
      if (response.getStatusLine().getStatusCode() == HttpsURLConnection.HTTP_OK)
        return true;

    } catch (Exception e) {
      LOG.error("Exception caught", e);
    } finally {
      httpClient.close();
      response.close();
    }

    return false;
  }

  protected boolean setPublicCertificate(String alias, String publicCert) {
    try {
      LOG.debug("Setting certificate: alias " + alias + " => " + publicCert.toString());

      if (StringUtils.isEmpty(alias) || StringUtils.isEmpty(publicCert)) {
        return false;
      }
      if (trustStore.get() == null && !init()) {
        return false;
      }

      CertificateFactory factory = CertificateFactory.getInstance("X.509");

      StringBuffer certData = new StringBuffer();
      if (!publicCert.trim().startsWith(BEGIN_CERTIFICATE)) {
        certData.append(BEGIN_CERTIFICATE).append("\n").append(publicCert.trim()).append("\n")
            .append(END_CERTIFICATE).append("\n");
      } else {
        certData.append(publicCert.trim());
      }

      X509Certificate cert = (X509Certificate) factory.generateCertificate(
          new ByteArrayInputStream(certData.toString().getBytes(StandardCharsets.UTF_8)));

      if (trustStore.get().containsAlias(alias)) {
        LOG.debug("Removing existing cert for alias " + alias);
        trustStore.get().deleteEntry(alias);
      }

      LOG.debug("Setting new cert for alias " + alias);
      trustStore.get().setCertificateEntry(alias, cert);

      return true;
    } catch (Exception e) {
      LOG.error("Exception caught", e);
    }

    return false;

  }

  private boolean setPublicCertificate(String alias, X509Certificate publicCert) {
    try {
      if (trustStore.get().containsAlias(alias)) {
        LOG.debug("Removing existing cert for alias " + alias);
        trustStore.get().deleteEntry(alias);
      }

      LOG.debug("Setting new cert for alias " + alias);
      if (publicCert != null) {
        LOG.debug("Setting certificate: alias " + alias + " => " + publicCert.toString());
        trustStore.get().setCertificateEntry(alias, publicCert);
        return true;
      }
    } catch (Exception e) {
      LOG.error("Exception caught", e);
    }

    return false;

  }

  private boolean init() {
    try {
      LOG.info("Initializing in-memory Truststore");
      KeyStore store = KeyStore.getInstance(TruststoreType);
      store.load(null);

      trustStore.set(store);

      return true;
    } catch (Exception e) {
      LOG.error("Exception caught", e);
    }

    return false;
  }

  private boolean updateTrustStore(String partnerReferenceId, final PartnerSecret partnerSecret,
      boolean forceUpdate) {
    if (!CollectionUtils.isEmpty(partnerSecret.getKeys())) {

      for (PartnerSecret.Key key : partnerSecret.getKeys()) {
        try {
          String alias = (partnerReferenceId + "_" + key.getProtocol()).toLowerCase();
          if (!trustStore.get().containsAlias(alias) || forceUpdate) {
            if (!setPublicCertificate(alias, key.getPublicCertificate())) {
              LOG.error("Failed to set Certificate for alias " + alias);
            }
          }
        } catch (Exception e) {
          LOG.error("Exception caught", e);
        }
      }
      return true;
    }

    return false;
  }

  public void loadPartnerCertificates(PartnerRequestContext requestContext,
      String partnerReferenceId, boolean forceUpdate) throws Exception {
    updateTrustStore(partnerReferenceId,
        partnerKeyStoreManager.getPartnerSecret(requestContext, partnerReferenceId), forceUpdate);
  }

  @SuppressWarnings("deprecation")
  private HostnameVerifier getHostnameVerifier() {
    // @TODO - Make this constant and configurable
    return (!StringUtils.isEmpty(partnerKeyStoreManager.getActiveEnvironment())
        && partnerKeyStoreManager.getActiveEnvironment().equals("prod"))
            ? SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER
            : SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
  }

  public SSLConnectionSocketFactory getSSLConnectionSocketFactory(
      PartnerRequestContext requestContext, String partnerReferenceId) throws Exception {
    if (trustStore.get() == null) {
      throw new Exception("Bank TrustStore not loaded");
    }

    loadPartnerCertificates(requestContext, partnerReferenceId, false);

    LOG.debug("Creating sslContext");
    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(trustStore.get(),
            (!StringUtils.isEmpty(partnerKeyStoreManager.getActiveEnvironment())
                && partnerKeyStoreManager.getActiveEnvironment().equals("prod")) ? null
                    : new TrustSelfSignedStrategy())
        .build();
    sslContext.init(null, HttpsUtils.TRUST_ALL_CERTS, new SecureRandom());

    HostnameVerifier hostnameVerifier = getHostnameVerifier();
    return new SSLConnectionSocketFactory(sslContext, new String[] {"TLSv1.2"}, null,
        hostnameVerifier);
  }

  public SSLSocketFactory getSSLSocketFactory(PartnerRequestContext requestContext,
      String partnerReferenceId) throws Exception {
    if (trustStore == null) {
      throw new Exception("Bank TrustStore not loaded");
    }

    loadPartnerCertificates(requestContext, partnerReferenceId, false);

    SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(trustStore.get(),
            (!StringUtils.isEmpty(partnerKeyStoreManager.getActiveEnvironment())
                && partnerKeyStoreManager.getActiveEnvironment().equals("prod")) ? null
                    : new TrustSelfSignedStrategy())
        .build();

    return sslContext.getSocketFactory();
  }

  @PostConstruct
  public void construct() throws Exception {
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ...");
    init();
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }
}
