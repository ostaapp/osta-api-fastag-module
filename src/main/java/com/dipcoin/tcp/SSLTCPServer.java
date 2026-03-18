package com.dipcoin.tcp;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;

public class SSLTCPServer implements Runnable {
  private static final Logger LOG = LogManager.getLogger(SSLTCPServer.class);

  private static String KEYSTORE_PASSWORD;
  private static KeyStore KEYSTORE = null;
  private static String KEYSTORETYPE = "PKCS12";

  @Autowired
  private ResourceLoader resourceLoader;

  public static void keystorePassword(String keystorePassword) {
    KEYSTORE_PASSWORD = keystorePassword;
  }

  private synchronized boolean initKeyStore(List<String> envs) {
    try {
      if (KEYSTORE == null) {
        KEYSTORE = KeyStore.getInstance(KEYSTORETYPE);
        KEYSTORE.load(null);
      }

      if (!CollectionUtils.isEmpty(envs)) {
        for (String env : envs) {
          try {
            String path = "classpath:dipcoin-" + env + ".p12";
            LOG.info("Loading Keystore " + path);
            KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);
            // @TODO - move password to config and per env
            ks.load(resourceLoader.getResource(path).getInputStream(),
                KEYSTORE_PASSWORD.toCharArray());

            Enumeration<String> enumeration = ks.aliases();
            while (enumeration.hasMoreElements()) {
              String alias = enumeration.nextElement();
              LOG.info("Reading alias " + alias);

              // Get Key & Certificates
              Key key = ks.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
              Certificate[] certs = ks.getCertificateChain(alias);

              // Put them altogether in the new keystore
              alias += "_" + env;
              LOG.info("Saving alias " + alias);
              KEYSTORE.setKeyEntry(alias, key, KEYSTORE_PASSWORD.toCharArray(), certs);
            }
          } catch (Exception e) {
            LOG.error("Exception caught", e);
          }

        }

        if (KEYSTORE.size() > 0) {
          return true;
        }
      }
    } catch (Exception e) {
      LOG.error("Exception caught", e);
    }

    return false;
  }

  @Autowired
  private Environment environment;

  @Autowired
  private AsyncRabbitTemplate tcpMessageAsyncAmqpTemplate;

  private ServerSocket serverSocket;
  private int port;
  private boolean shutdown = false;
  private boolean secure = true;
  private boolean keepSocketAlive = false;
  private String partnerReferenceId;

  private ExecutorService engineExecutor = Executors.newSingleThreadExecutor();
  private ExecutorService requestPool = Executors.newFixedThreadPool(100); // @TODO - Make this
                                                                           // configurable

  @PostConstruct
  public void construct() throws Exception {
    String[] profiles =
        ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
            : environment.getDefaultProfiles();
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ..." + Arrays.asList(profiles)
        + ", Port: " + port);
    // temp commented since this is not working on prod
    /*
     * if (!initKeyStore(Arrays.asList(profiles))) { throw new
     * Exception("Failed to initialize KeyStore"); } engineExecutor.execute(this);
     */
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");

    shutdown();

    if (!serverSocket.isClosed()) {
      serverSocket.close();
    }
    engineExecutor.shutdown();
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void shutdown() {
    this.shutdown = true;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public void setKeepSocketAlive(boolean keepSocketAlive) {
    this.keepSocketAlive = keepSocketAlive;
  }

  public void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  private ServerSocket initServerSocket(int port) throws Exception {
    LOG.info("Initializing Un-Secure TCP ...");
    return new ServerSocket(port);
  }

  private SSLServerSocket initSSLServerSocket(int port) throws Exception {
    LOG.info("Initializing TCP SSL....");

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(KEYSTORE, KEYSTORE_PASSWORD.toCharArray());

    TrustManagerFactory trustManager =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManager.init(KEYSTORE);

    SSLContext sc = SSLContext.getInstance("TLS");
    sc.init(kmf.getKeyManagers(), trustManager.getTrustManagers(), null);

    SSLServerSocketFactory ssf = sc.getServerSocketFactory();
    SSLServerSocket ssc = (SSLServerSocket) ssf.createServerSocket(port);
    ssc.setEnabledProtocols(new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "SSLv3"});

    return ssc;
  }

  @Override
  public void run() {
    try {
      LOG.info("Starting TCP Server on port: " + port + " for Client: " + partnerReferenceId);

      try {
        serverSocket = this.secure ? initSSLServerSocket(port) : initServerSocket(port);
      } catch (Exception e) {
        LOG.error("Failed to initialize TCP Server", e);
        return;
      }

      while (!shutdown) {
        if (serverSocket.isClosed()) {
          break;
        }

        try {
          Socket socket = serverSocket.accept();

          // @TODO - apply IP whitelisting

          ClientSocketHandler handler = new ClientSocketHandler(partnerReferenceId, socket);
          handler.setKeepSocketAlive(keepSocketAlive);
          handler.setTcpMessageAsyncAmqpTemplate(tcpMessageAsyncAmqpTemplate);

          // spawn thread to handle request
          this.requestPool.execute(handler);
        } catch (Exception e) {
          LOG.error("Exception caught ", e);
        }
      }

      LOG.info("Stopping TCP Server for Client: " + partnerReferenceId);
      serverSocket.close();

      this.requestPool.shutdown();
    } catch (Exception e) {
      LOG.error("Exception caught " + e);
    }

  }
}
