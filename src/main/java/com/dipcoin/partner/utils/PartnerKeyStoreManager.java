package com.dipcoin.partner.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.dipcoin.commons.HttpsUtils;
import com.dipcoin.commons.HttpsUtils.Proxy;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.commons.KeyStoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.PartnerSecret;
import com.dipcoin.commons.PartnerSecret.Key;
import com.dipcoin.commons.ResourceManager;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.core.CryptoUtil.AlgoScheme;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.dao.PartnerServerDao;
import com.dipcoin.db.services.model.PartnerServer;
import com.dipcoin.partner.utils.PartnerClient.Protocol;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component("partnerKeyStoreManager")
public class PartnerKeyStoreManager implements ResourceManager.CacheEnabled {

  private static final Logger LOG = LogManager.getLogger(PartnerKeyStoreManager.class);
  private static ObjectMapper objectMapper = new ObjectMapper();

  public static class PartnerSecretLookupKey {
    private String partnerReferenceId;
    private Protocol protocol;

    private PartnerSecretLookupKey(String partnerReferenceId, Protocol protocol) {
      this.partnerReferenceId = partnerReferenceId;
      this.protocol = protocol;
    }

    public String getPartnerReferenceId() {
      return partnerReferenceId;
    }

    public Protocol getProtocol() {
      return protocol;
    }

    public static PartnerSecretLookupKey of(String partnerReferenceId, Protocol protocol) {
      return new PartnerSecretLookupKey(partnerReferenceId, protocol);
    }

    @Override
    public String toString() {
      return partnerReferenceId + protocol.type();
    }

    @Override
    public int hashCode() {
      return Objects.hash(partnerReferenceId, protocol.type());
    }
  }

  @Autowired
  private CryptoUtil cryptoUtil;

  @Autowired
  private PartnerServerDao partnerServerDao;

  @Autowired
  private Environment environment;

  @Autowired
  private ResourceLoader resourceLoader;

  private List<String> activeProfiles = null;

  private final AtomicReference<KeyStore> secretKeyStore = new AtomicReference<>();

  private String secretKeyStorePath, secretKeyStorePass, secretKeyStoreKeyPass,
      secretKeyStoreType = "PKCS12";

  private static final Proxy proxy = new Proxy();

  // @TODO - move this to environment variable if profile doesnt stay in tune with
  // env
  public String getActiveEnvironment() {
    if (!CollectionUtils.isEmpty(activeProfiles)) {
      return activeProfiles.get(0);
    }

    return null;
  }

  public void setSecretKeyStorePath(String secretKeyStorePath) {
    this.secretKeyStorePath = secretKeyStorePath;
  }

  public void setSecretKeyStorePass(String secretKeyStorePass) {
    this.secretKeyStorePass = secretKeyStorePass;
  }

  public void setSecretKeyStoreKeyPass(String secretKeyStoreKeyPass) {
    this.secretKeyStoreKeyPass = secretKeyStoreKeyPass;
  }

  public static void initProxy(String host, int port, boolean enabled) {
    LOG.info(LogFormatter.instance().message("Proxy Setup: " + host + ":" + port + ", " + enabled)
        .format());
    proxy.setHost(host);
    proxy.setPort(port);
    proxy.setEnabled(enabled);
  }

  public void init() throws Exception {
    activeProfiles = Arrays.asList(
        ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
            : environment.getDefaultProfiles());

    LOG.info(LogFormatter.instance().data("Active Profiles", activeProfiles)
        .data("secretKeyStorePass", Mask.PASSWORD_MASKED.value())
        .data("secretKeyStoreKeyPass", Mask.PASSWORD_MASKED.value())
        .data("secretKeyStoreType", secretKeyStoreType).format());
    KeyStore store = KeyStore.getInstance(secretKeyStoreType);
    LOG.info(LogFormatter.instance().message("Loading KeyStore: " + secretKeyStorePath).format());
    store.load(resourceLoader.getResource(secretKeyStorePath).getInputStream(),
        secretKeyStorePass.toCharArray());
    secretKeyStore.set(store);
  }

  public static X509Certificate fetchPublicCert(String host, int port, boolean selfSigned)
      throws Exception {

    SSLSocket socket = null;
    Socket tunnel = null;
    try {
      LOG.debug(LogFormatter.instance().message("Fetching Public Cert").data("host", host)
          .data("port", port).data("selfSigned", selfSigned).format());
      if (selfSigned) {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, HttpsUtils.TRUST_ALL_CERTS, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      }

      LOG.debug(LogFormatter.instance().message("Initiating SSLSocketFactory...")
          .data("proxy.isEnabled()", proxy.isEnabled()).format());
      SSLSocketFactory sslfactory = HttpsURLConnection.getDefaultSSLSocketFactory();
      if (proxy.isEnabled() && !StringUtils.isEmpty(proxy.getHost()) && proxy.getPort() > 0) {
        LOG.debug(LogFormatter.instance().message("Initiating Tunnel...").format());
        tunnel = new Socket(proxy.getHost(), proxy.getPort());

        LOG.debug(LogFormatter.instance().data("tunnel details", tunnel.toString()).format());

        HttpsUtils.doTunnelHandshake(tunnel, proxy.getHost(), proxy.getPort(), host, port);

        LOG.debug(LogFormatter.instance().message("Initiating SSLSocket...").format());
        socket = (SSLSocket) sslfactory.createSocket(tunnel, host, port, true);

      } else {
        LOG.debug(
            LogFormatter.instance().message("Initiating SSLSocket without proxy...").format());

        socket = (SSLSocket) sslfactory.createSocket();
        socket.connect(new InetSocketAddress(host, port), 5000);
      }

      LOG.debug(LogFormatter.instance().message("Initiate handshake...").format());
      socket.startHandshake();

      Certificate[] certs = socket.getSession().getPeerCertificates();
      if (ArrayUtils.isEmpty(certs)) {
        return null;
      }
      return (X509Certificate) certs[0];
      /*
       * Certificate cert = certs[0]; RSAPublicKey key = (RSAPublicKey) cert.getPublicKey();
       * 
       * PemObject pemObject = new PemObject("CERTIFICATE", key.getEncoded()).generate(); final
       * StringWriter stringWriter = new StringWriter(); final PemWriter pemWriter = new
       * PemWriter(stringWriter); pemWriter.writeObject(pemObject); pemWriter.flush();
       * pemWriter.close();
       * 
       * return stringWriter.toString();
       */
    } finally {

      // closing tunnel
      if (tunnel != null && !tunnel.isClosed())
        tunnel.close();

      // closing socket
      if (socket != null && !socket.isClosed())
        socket.close();

    }
  }

  /*
   * Encryption
   */
  // @TODO - Move the secret store to a centralized location OR configure each
  // API system to load only certain banks
  private LoadingCache<String, PartnerSecret> partnerSecretsLookup =
      CacheBuilder.newBuilder().maximumSize(2000).expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new CacheLoader<String, PartnerSecret>() {

            @Override
            public PartnerSecret load(String partnerReferenceId) throws Exception {
              String originalPartnerReferenceId = partnerReferenceId;
              partnerReferenceId =
                  partnerReferenceId.contains("_") ? partnerReferenceId.split("_")[0]
                      : partnerReferenceId;
              List<PartnerServer> list = partnerServerDao
                  .findServerByReferenceId(partnerReferenceId, getActiveEnvironment());
              LOG.debug(LogFormatter.instance().data("partnerReferenceId", partnerReferenceId)
                  .data("activeEnvironment", getActiveEnvironment())
                  .data("store", secretKeyStore.get().aliases()).format());
              if (CollectionUtils.isEmpty(list)) {
                throw new Exception("Failed to fetch PartnerServer for Bank: " + partnerReferenceId
                    + " and environment: " + getActiveEnvironment());
              }

              List<Key> keys = new LinkedList<>();
              for (PartnerServer server : list) {
                try {
                  Protocol protocol = Protocol.lookup(server.getProtocol());
                  if (protocol == null) {
                    LOG.debug(LogFormatter.instance().message("Invalid/Missing Protocol in DB")
                        .data("protocol", server.getProtocol()).format());
                    continue;
                  }

                  // @TODO - move to function.
                  // @NOTE lowercase as its the key standard

                  String alias = (originalPartnerReferenceId + "_" + protocol.type()).toLowerCase();

                  LOG.debug(LogFormatter.instance().data("alias", alias).format());

                  if (!secretKeyStore.get().containsAlias(alias)) {
                    LOG.debug(LogFormatter.instance()
                        .message("KeyStore doesnt contain alias " + alias).format());
                    continue;
                  }

                  String decryptedKey = KeyStoreUtils.readPasswordFromKeyStore(secretKeyStore.get(),
                      secretKeyStoreKeyPass, alias);
                  LOG.debug(LogFormatter.instance().data("partnerReferenceId", partnerReferenceId)
                      .data("SecretKey", decryptedKey).format());

                  // @TODO - For now using secret key as init vector too. Need to store in DB
                  String initVector = null;
                  if (server.getIVLength() > 0) {
                    LOG.debug(LogFormatter.instance()
                        .message("Initializing IV..." + server.getIVLength()).format());
                    initVector = decryptedKey.substring(0, server.getIVLength());
                  } else {
                    LOG.debug(LogFormatter.instance()
                        .message("Initializing IV..." + server.getIVLength()).format());
                    initVector = decryptedKey;
                  }

                  AlgoScheme algoScheme = AlgoScheme.scheme(server.getEncryptionAlgo(),
                      server.getEncryptionAlgoScheme());

                  X509Certificate sslCert =
                      protocol.isSecure()
                          ? fetchPublicCert(server.getHost(), Integer.parseInt(server.getPort()),
                              server.getSelfSignedCert() == DBConstants.BooleanStatus.YES.value()
                                  ? true
                                  : false)
                          : null;

                  Key key = new Key.Builder().setHost(server.getHost())
                      .setProtocol(server.getProtocol()).setSecretKey(decryptedKey)
                      .setInitVector(initVector).setAlgoScheme(algoScheme)
                      .setPublicCertificate(sslCert).setClientEncoding(server.getClientEncoding())
                      .setClientDecoding(server.getClientDecoding())
                      .setKeyDecoding(server.getKeyDecoding())
                      .setPartnerEncoding(server.getPartnerEncoding())
                      .setPartnerDecoding(server.getPartnerDecoding()).build();

                  LOG.debug("Added Key: " + objectMapper.writeValueAsString(key));
                  keys.add(key);
                } catch (Exception e) {
                  LOG.error("Exception Caught", e);
                }
              }

              return new PartnerSecret(partnerReferenceId, keys);
            }
          });

  // Note this is just to use encrypt data send in your request it could whole request or just a
  // parameter

  private LoadingCache<String, PartnerSecret> partnerSecretsLookupForDataEncryption =
      CacheBuilder.newBuilder().maximumSize(2000).expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new CacheLoader<String, PartnerSecret>() {

            @Override
            public PartnerSecret load(String partnerReferenceId) throws Exception {
              String originalPartnerReferenceId = partnerReferenceId;
              partnerReferenceId =
                  partnerReferenceId.contains("_") ? partnerReferenceId.split("_")[0]
                      : partnerReferenceId;
              List<PartnerServer> list = partnerServerDao
                  .findServerByReferenceId(partnerReferenceId, getActiveEnvironment());
              LOG.debug(LogFormatter.instance().data("partnerReferenceId", partnerReferenceId)
                  .data("activeEnvironment", getActiveEnvironment())
                  .data("store", secretKeyStore.get().aliases()).format());
              if (CollectionUtils.isEmpty(list)) {
                throw new Exception("Failed to fetch PartnerServer for Bank: " + partnerReferenceId
                    + " and environment: " + getActiveEnvironment());
              }

              List<Key> keys = new LinkedList<>();
              for (PartnerServer server : list) {
                try {
                  Protocol protocol = Protocol.lookup(server.getProtocol());
                  if (protocol == null) {
                    LOG.debug(LogFormatter.instance().message("Invalid/Missing Protocol in DB")
                        .data("protocol", server.getProtocol()).format());
                    continue;
                  }

                  // @TODO - move to function.
                  // @NOTE lowercase as its the key standard

                  String alias = (originalPartnerReferenceId + "_" + protocol.type()).toLowerCase();

                  LOG.debug(LogFormatter.instance().data("alias", alias).format());

                  if (!secretKeyStore.get().containsAlias(alias)) {
                    LOG.debug(LogFormatter.instance()
                        .message("KeyStore doesnt contain alias " + alias).format());
                    continue;
                  }

                  String decryptedKey = KeyStoreUtils.readPasswordFromKeyStore(secretKeyStore.get(),
                      secretKeyStoreKeyPass, alias);
                  LOG.debug(LogFormatter.instance().data("partnerReferenceId", partnerReferenceId)
                      .data("SecretKey", decryptedKey).format());

                  // @TODO - For now using secret key as init vector too. Need to store in DB
                  String initVector = null;
                  if (server.getIVLength() > 0) {
                    LOG.debug(LogFormatter.instance()
                        .message("Initializing IV..." + server.getIVLength()).format());
                    initVector = decryptedKey.substring(0, server.getIVLength());
                  } else {
                    LOG.debug(LogFormatter.instance()
                        .message("Initializing IV..." + server.getIVLength()).format());
                    initVector = decryptedKey;
                  }

                  AlgoScheme algoScheme = AlgoScheme.scheme(server.getEncryptionAlgo(),
                      server.getEncryptionAlgoScheme());

                  Key key =
                      new Key.Builder().setHost(server.getHost()).setProtocol(server.getProtocol())
                          .setSecretKey(decryptedKey).setInitVector(initVector)
                          .setAlgoScheme(algoScheme).setClientDecoding(server.getClientDecoding())
                          .setKeyDecoding(server.getKeyDecoding())
                          .setPartnerEncoding(server.getPartnerEncoding())
                          .setPartnerDecoding(server.getPartnerDecoding()).build();

                  LOG.debug("Added Key: " + objectMapper.writeValueAsString(key));
                  keys.add(key);
                } catch (Exception e) {
                  LOG.error("Exception Caught", e);
                }
              }

              return new PartnerSecret(partnerReferenceId, keys);
            }
          });



  public PartnerSecret getPartnerSecret(PartnerRequestContext requestContext,
      String partnerReferenceId) throws Exception {
    return partnerSecretsLookup.get(partnerReferenceId);
  }

  public PartnerSecret getPartnerSecretForDataEncryption(PartnerRequestContext requestContext,
      String partnerReferenceId) throws Exception {
    return partnerSecretsLookupForDataEncryption.get(partnerReferenceId);
  }

  public byte[] encryptData(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, byte[] data) throws Exception {
    try {
      PartnerSecret secret = getPartnerSecretForDataEncryption(requestContext, partnerReferenceId);
      if (secret != null) {
        Key partnerKey = secret.getKeyByProtocol(protocol.name());
        if (partnerKey == null) {
          throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
              + " and protocol " + protocol.name());
        }

        LOG.debug(LogFormatter.instance().data("AlgoScheme", partnerKey.getAlgoScheme())
            .data("KeyEncoding", partnerKey.getKeyDecoding()).format());
        return cryptoUtil.encrypt(data, partnerKey.getAlgoScheme(),
            CryptoUtil.decodeKeyBytes(partnerKey.getSecretKey(), partnerKey.getKeyDecoding()),
            CryptoUtil.decodeKeyBytes(partnerKey.getInitVector(), partnerKey.getKeyDecoding()));
      }
    } catch (Exception e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }
    throw new Exception("Failed to encrypt data for partnerReferenceId " + partnerReferenceId);
  }

  /*
   * Encrypt/Decrypt payload as needed
   */
  public byte[] encrypt(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, byte[] data) throws Exception {
    try {
      PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
      if (secret != null) {
        Key partnerKey = secret.getKeyByProtocol(protocol.name());
        if (partnerKey == null) {
          throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
              + " and protocol " + protocol.name());
        }

        LOG.debug(LogFormatter.instance().data("AlgoScheme", partnerKey.getAlgoScheme())
            .data("KeyEncoding", partnerKey.getKeyDecoding()).format());
        return cryptoUtil.encrypt(data, partnerKey.getAlgoScheme(),
            CryptoUtil.decodeKeyBytes(partnerKey.getSecretKey(), partnerKey.getKeyDecoding()),
            CryptoUtil.decodeKeyBytes(partnerKey.getInitVector(), partnerKey.getKeyDecoding()));
      }
    } catch (Exception e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }
    throw new Exception("Failed to encrypt data for partnerReferenceId " + partnerReferenceId);
  }

  public byte[] decrypt(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, byte[] data) throws Exception {
    try {
      PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
      if (secret != null) {
        // @TODO - use client IV too besides secret key
        Key partnerKey = secret.getKeyByProtocol(protocol.name());
        if (partnerKey == null) {
          throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
              + " and protocol " + protocol.name());
        }

        LOG.debug(LogFormatter.instance().data("AlgoScheme", partnerKey.getAlgoScheme())
            .data("SecretKey", partnerKey.getSecretKey())
            .data("KeyEncoding", partnerKey.getKeyDecoding()).format());
        return cryptoUtil.decrypt(data, partnerKey.getAlgoScheme(),
            CryptoUtil.decodeKeyBytes(partnerKey.getSecretKey(), partnerKey.getKeyDecoding()),
            CryptoUtil.decodeKeyBytes(partnerKey.getInitVector(), partnerKey.getKeyDecoding()));
      }
    } catch (Exception e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }
    throw new Exception(
        "Failed to decrypt data " + data + " for partnerReferenceId " + partnerReferenceId);
  }

  public String clientEncode(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, byte[] data) throws Exception {
    PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
    if (secret != null) {
      Key partnerKey = secret.getKeyByProtocol(protocol.name());
      if (partnerKey == null) {
        throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
            + " and protocol " + protocol.name());
      }

      return CryptoUtil.encode(data, partnerKey.getClientEncoding());
    } else {
      throw new Exception(
          "Failed to encode system data for partnerReferenceId " + partnerReferenceId);
    }
  }

  public byte[] clientDecode(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, String data) throws Exception {
    PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
    if (secret != null) {
      Key partnerKey = secret.getKeyByProtocol(protocol.name());
      if (partnerKey == null) {
        throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
            + " and protocol " + protocol.name());
      }

      return CryptoUtil.decode(data, partnerKey.getClientDecoding());
    } else {
      throw new Exception(
          "Failed to decode system data for partnerReferenceId " + partnerReferenceId);
    }
  }

  public String partnerEncode(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, byte[] data) throws Exception {
    PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
    if (secret != null) {
      Key partnerKey = secret.getKeyByProtocol(protocol.name());
      if (partnerKey == null) {
        throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
            + " and protocol " + protocol.name());
      }

      return CryptoUtil.encode(data, partnerKey.getPartnerEncoding());
    } else {
      throw new Exception(
          "Failed to encode partner data for partnerReferenceId " + partnerReferenceId);
    }
  }

  public byte[] partnerDecode(PartnerRequestContext requestContext, Protocol protocol,
      String partnerReferenceId, String data) throws Exception {
    PartnerSecret secret = getPartnerSecret(requestContext, partnerReferenceId);
    if (secret != null) {
      Key partnerKey = secret.getKeyByProtocol(protocol.name());
      if (partnerKey == null) {
        throw new Exception("Secret not configured for partnerReferenceId " + partnerReferenceId
            + " and protocol " + protocol.name());
      }

      return CryptoUtil.decode(data, partnerKey.getPartnerDecoding());
    } else {
      throw new Exception(
          "Failed to decode partner data for partnerReferenceId " + partnerReferenceId);
    }
  }

  @PostConstruct
  public void construct() throws Exception {
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ... ");
    ResourceManager.register(this);
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }

  @Override
  public void nukeCache() {
    LOG.info("Nuking " + this.getClass().getSimpleName() + " caches");
    partnerSecretsLookup.invalidateAll();
  }

  public AtomicReference<KeyStore> getSecretKeyStore() {
    return secretKeyStore;
  }

  public String getSecretKeyStorePass() {
    return secretKeyStorePass;
  }

  public String getSecretKeyStoreKeyPass() {
    return secretKeyStoreKeyPass;
  }
}
