package com.dipcoin.partner.toll.commons;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ResourceLoader;

import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.commons.LogFormatter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Component("tollSignatureGenerationServices")
public class TollSignatureGenerationServices {

	private static String KEYSTORETYPE = "PKCS12";
	private static final Logger LOG = LogManager.getLogger(TollSignatureGenerationServices.class);

	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private TollProperties tollProperties;

	public ByteArrayOutputStream signatureGenerationServices(ByteArrayInputStream byteArrayInputStream, String traceId,
			String alias) {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {

			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			Transform envTransform = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);

			List<Transform> transformList = new ArrayList<>();
			transformList.add(envTransform);

			Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null), transformList, null,
					null);
			SignatureMethod signatureMethod = fac
					.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
			SignedInfo si = fac.newSignedInfo(
					fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
					signatureMethod, Collections.singletonList(ref));

			LOG.debug(LogFormatter.instance(traceId).message("Loading Keystore")
					.data("keystore path", tollProperties.getKeystorePath()).format());

			KeyStore ks = KeyStore.getInstance(KEYSTORETYPE);

			ks.load(FileUtils.openInputStream(new File(tollProperties.getKeystorePath())),
					tollProperties.getKeystorePassword().toCharArray());

			Iterator<String> certificateAliases = ks.aliases().asIterator();
			while (certificateAliases.hasNext()) {
				String certificateAlias = certificateAliases.next();
				if (alias.equalsIgnoreCase(certificateAlias)) {
					alias = certificateAlias;
				}
			}

			KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias,
					new KeyStore.PasswordProtection(tollProperties.getKeystorePassword().toCharArray()));

			X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

			// Create the KeyInfo containing the X509Data.
			KeyInfoFactory kif = fac.getKeyInfoFactory();
			List x509Content = new ArrayList();

			x509Content.add(cert.getSubjectX500Principal().getName());
			x509Content.add(cert);
			X509Data xd = kif.newX509Data(x509Content);
			KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);

			Document doc = dbf.newDocumentBuilder().parse(byteArrayInputStream);
			DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());
			XMLSignature signature = fac.newXMLSignature(si, ki);
			signature.sign(dsc);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer trans = tf.newTransformer();
			trans.transform(new DOMSource(doc), new StreamResult(byteArrayOutputStream));

			String str = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
			str = str.replace("&#13;", "");

			byteArrayOutputStream.reset();

			for (int i = 0; i < str.length(); ++i)
				byteArrayOutputStream.write(str.charAt(i));

		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(traceId).message(
					"Exception Caught While Generating signature or  fetching certificate stored at bankkeyStore path")
					.format(), e);
		}

		return byteArrayOutputStream;
	}

}
