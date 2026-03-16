package com.dipcoin.partner.toll.commons;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.api.commons.TollProperties;

@Component("tollSignatureVerificationServices")
public class TollSignatureVerificationServices {
  private static final Logger LOG = LogManager.getLogger(TollSignatureVerificationServices.class);


  @Autowired
  private ResourceLoader resourceLoader;
  
  @Autowired
  private TollProperties tollProperties;

  public static boolean verificationResult = true;
  private static String KEYSTORETYPE = "PKCS12";

  public ByteArrayOutputStream verifySignature(String traceId, String data, String alias) {
    
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      Document signedDocument =
          dbf.newDocumentBuilder().parse(new ByteArrayInputStream(data.getBytes()));

      NodeList nl = signedDocument.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

      if (nl.getLength() == 0) {
        verificationResult = false;
        return null;
      }

      XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

      DOMValidateContext valContext =
          new DOMValidateContext(getPublicKey(traceId, alias), nl.item(0));
      XMLSignature signature = fac.unmarshalXMLSignature(valContext);


      verificationResult = signature.validate(valContext);

      LOG.debug(LogFormatter.instance(traceId).message("Incoming Request body signature verification")
          .data("Verification Result", verificationResult).format());

      nl.item(0).getParentNode().removeChild(nl.item(0));

      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer trans = tf.newTransformer();
      trans.transform(new DOMSource(signedDocument), new StreamResult(byteArrayOutputStream));


    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(traceId)
          .message("Exception Caught While verifying digital siganature").format(), e);
    }

    return byteArrayOutputStream;
  }

  public PublicKey getPublicKey(String traceId, String alias) {
    PublicKey publicKey = null;

    try {
      
      LOG.debug(LogFormatter.instance(traceId).message("Loading Keystore").data("keystore path", tollProperties.getKeystorePath())
          .format());
      
      KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);

      ks.load(FileUtils.openInputStream(new File(tollProperties.getKeystorePath())), tollProperties.getKeystorePassword().toCharArray());

      Iterator<String> certificateAliases = ks.aliases().asIterator();
      while(certificateAliases.hasNext()){
        String certificateAlias = certificateAliases.next();
        if(alias.equalsIgnoreCase(certificateAlias)) {
          alias = certificateAlias;
        }
      }
      
      X509Certificate cert = (X509Certificate) ks.getCertificate(alias);


      publicKey = cert.getPublicKey();

    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(traceId).message(
          "Exception Caught While fetching public key from certificate stored at bankkeyStore path")
          .format(), e);
    }

    return publicKey;

  }

  public PrivateKey getPrivateKey(String traceId, String alias) {
    PrivateKey key = null;
    try {
      
      
      LOG.debug(LogFormatter.instance(traceId).message("Loading Keystore").data("keystore path", tollProperties.getKeystorePath())
          .format());
      
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

      ks.load(FileUtils.openInputStream(new File(tollProperties.getKeystorePath())), tollProperties.getKeystorePassword().toCharArray());

      Iterator<String> certificateAliases = ks.aliases().asIterator();
      while(certificateAliases.hasNext()){
        String certificateAlias = certificateAliases.next();
        if(alias.equalsIgnoreCase(certificateAlias)) {
          alias = certificateAlias;
        }
      }
      
      key = (PrivateKey) ks.getKey(alias, tollProperties.getKeystorePassword().toCharArray());


    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(traceId).message(
          "Exception Caught While fetching private key from certificate stored at bankkeyStore path")
          .format(), e);
    }

    return key;
  }
  
  
  public  boolean verifyEcdsaSignedData(String plainText, String signature, PublicKey publicKey)
      throws Exception {
    try {
      
      Security.addProvider(new BouncyCastleProvider());
      Signature publicSignature = Signature.getInstance("SHA256WITHPLAIN-ECDSA", "BC");
      publicSignature.initVerify(publicKey);
      publicSignature.update(plainText.getBytes("UTF8"));

      byte[] signatureBytes = Hex.decodeHex(signature);

      return publicSignature.verify(signatureBytes);    
    } catch (Exception e) {
      return false;
    }
  }
  
   
}
