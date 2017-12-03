/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 29 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;

/**
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=geoserver-enterprise-master/src/web/app/src/test/java/org/geoserver/web/Start.java
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=LittleProxy-mitm-master/src/main/java/org/littleshoot/proxy/mitm/CertificateHelper.java
 */
public class KeystoreTool {
	
	private static Logger log = Logger.getLogger(KeystoreTool.class);
	
	private static final int PRIVATE_KEY_SIZE = 256;
	private static final String KEYGEN_ALGORITHM = "EC";
	private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
	private static final String KMF_ALGORITHM = "SunX509";
	private static final long SELF_GEN_CERTIFICATE_VALIDITY_DURATION = TimeUnit.DAYS.toMillis(360l * 10l);
	private static final long WARN_BEFORE_TIME_EXPIRATION_CERTIFICATES = TimeUnit.DAYS.toMillis(360);
	
	private final KeyStore keystore;
	private final KeyManagerFactory key_manager_factory;
	private final TrustManagerFactory trust_manager_factory;
	
	/*
	 * signature_algorithms: SHA512withECDSA, SHA512withRSA, SHA384withECDSA, SHA384withRSA, SHA256withECDSA, SHA256withRSA, SHA256withDSA, SHA224withECDSA, SHA224withRSA, SHA224withDSA, SHA1withECDSA, SHA1withRSA, SHA1withDSA
	 */
	private static final String SIGNATURE_ALGORITHM = "SHA512withECDSA";
	
	static {
		MyDMAM.checkJVM(true);
		Security.setProperty("crypto.policy", "unlimited");
		// Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * @param keystore_file file.jks
	 */
	public KeystoreTool(File keystore_file, String keystore_password, String x590_principal_hostname) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, SecurityException, SignatureException, OperatorCreationException, NoSuchProviderException, UnrecoverableKeyException {
		if (keystore_file == null) {
			throw new NullPointerException("\"keystore_file\" can't to be null");
		}
		if (keystore_password == null) {
			throw new NullPointerException("\"keystore_password\" can't to be null");
		}
		
		keystore = KeyStore.getInstance("JKS");
		
		if (keystore_file.exists()) {
			FileInputStream fis = new FileInputStream(keystore_file);
			log.info("Load keystore file: " + keystore_file.getPath());
			keystore.load(fis, keystore_password.toCharArray());
			fis.close();
			if (isContainsCertificateHostname(keystore, x590_principal_hostname) == false) {
				throw new CertificateException("Invalid pks file " + keystore_file.getName() + ": missing \"" + x590_principal_hostname + "\" in x590 principal. You must regenerate it or change expected x590_principal_hostname");
			}
			checkValidityDates(keystore);
		} else {
			keystore.load(null);
			
			KeyPairGenerator kp_generator = KeyPairGenerator.getInstance(KEYGEN_ALGORITHM);
			SecureRandom secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
			kp_generator.initialize(PRIVATE_KEY_SIZE, secureRandom);
			KeyPair keyPair = kp_generator.generateKeyPair();
			
			int random = new SecureRandom().nextInt();
			if (random < 0) random *= -1;
			BigInteger serial = BigInteger.valueOf(random);
			long expiration_date = System.currentTimeMillis() + SELF_GEN_CERTIFICATE_VALIDITY_DURATION;
			
			log.info("Generate keystore for \"" + x590_principal_hostname + "\", with a self-signed certificate and a key size " + PRIVATE_KEY_SIZE + " bits, a validity up to the " + Loggers.dateLog(expiration_date) + " and a S/N " + serial.toString(16));
			
			X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
			nameBuilder.addRDN(BCStyle.CN, x590_principal_hostname);
			
			nameBuilder.addRDN(BCStyle.O, "None"); // organizationName
			nameBuilder.addRDN(BCStyle.OU, "MyDMAM"); // organizationalUnitName
			String timezone = System.getProperty("user.timezone", "");
			if (timezone.equals("") == false) {
				nameBuilder.addRDN(BCStyle.ST, timezone.split("/")[0]);
				nameBuilder.addRDN(BCStyle.L, timezone.split("/")[1]);
			}
			nameBuilder.addRDN(BCStyle.C, System.getProperty("user.country", Locale.getDefault().getCountry()));
			
			X500Name issuer = nameBuilder.build();
			X500Name subject = issuer;
			PublicKey pubKey = keyPair.getPublic();
			Date notBefore = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));
			Date notAfter = new Date(expiration_date);
			
			X509v3CertificateBuilder generator = new JcaX509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, pubKey);
			
			ByteArrayInputStream bIn = new ByteArrayInputStream(pubKey.getEncoded());
			ASN1InputStream is = null;
			try {
				is = new ASN1InputStream(bIn);
				ASN1Sequence seq = (ASN1Sequence) is.readObject();
				SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
				generator.addExtension(Extension.subjectKeyIdentifier, false, new BcX509ExtensionUtils().createSubjectKeyIdentifier(info));
			} finally {
				IOUtils.closeQuietly(is);
			}
			
			generator.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
			
			KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
			generator.addExtension(Extension.keyUsage, false, usage);
			
			ASN1EncodableVector purposes = new ASN1EncodableVector();
			purposes.add(KeyPurposeId.id_kp_serverAuth);
			purposes.add(KeyPurposeId.id_kp_clientAuth);
			purposes.add(KeyPurposeId.anyExtendedKeyUsage);
			generator.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));
			
			ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
			X509Certificate PKCertificate = new JcaX509CertificateConverter().getCertificate(generator.build(signer));
			
			/*File certFile = new File(keystore_file.getParentFile(), hostname + ".cert");
			FileOutputStream fos = new FileOutputStream(certFile.getAbsoluteFile());
			fos.write(PKCertificate.getEncoded());
			fos.close();*/
			
			keystore.setKeyEntry(x590_principal_hostname + ".key", keyPair.getPrivate(), keystore_password.toCharArray(), new java.security.cert.Certificate[] { PKCertificate });
			keystore.setCertificateEntry(x590_principal_hostname + ".cert", PKCertificate);
			
			log.info("Save keystore file to " + keystore_file.getCanonicalPath());
			keystore.store(new FileOutputStream(keystore_file), keystore_password.toCharArray());
			CopyMove.setUserOnlyPermissionsToFile(keystore_file);
		}
		
		/**
		 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
		 */
		/**
		 * KeyManager's decide which key material to use.
		 */
		key_manager_factory = KeyManagerFactory.getInstance(KMF_ALGORITHM);
		key_manager_factory.init(keystore, keystore_password.toCharArray());
		
		/**
		 * TrustManager's decide whether to allow connections.
		 */
		trust_manager_factory = TrustManagerFactory.getInstance(KMF_ALGORITHM);
		trust_manager_factory.init(keystore);
	}
	
	SSLContext createTLSContext(String context_protocol) throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance(context_protocol);
		sslContext.init(key_manager_factory.getKeyManagers(), trust_manager_factory.getTrustManagers(), null);
		return sslContext;
	}
	
	private static boolean isContainsCertificateHostname(KeyStore ks, String x590_principal_hostname) throws KeyStoreException {
		Enumeration<String> e = ks.aliases();
		while (e.hasMoreElements()) {
			String alias = e.nextElement();
			if (ks.isCertificateEntry(alias)) {
				Certificate c = ks.getCertificate(alias);
				if (c instanceof X509Certificate) {
					X500Principal p = (X500Principal) ((X509Certificate) c).getSubjectX500Principal();
					if (p.getName().contains(x590_principal_hostname)) return true;
				}
			}
		}
		return false;
	}
	
	private static void checkValidityDates(KeyStore ks) throws CertificateExpiredException, CertificateNotYetValidException, KeyStoreException {
		Enumeration<String> e = ks.aliases();
		while (e.hasMoreElements()) {
			String alias = e.nextElement();
			if (ks.isCertificateEntry(alias) == false) {
				continue;
			}
			Certificate c = ks.getCertificate(alias);
			if (c instanceof X509Certificate) {
				X509Certificate certificate = (X509Certificate) c;
				certificate.checkValidity();
				
				long max_date = certificate.getNotAfter().getTime();
				long threshold_date = System.currentTimeMillis() + WARN_BEFORE_TIME_EXPIRATION_CERTIFICATES;
				
				if (threshold_date > max_date) {
					log.warn("Certificate \"" + alias + "\" will expire soon (expiration date: " + Loggers.dateLog(max_date) + ") you should regeneate it (and re-deploy it)");
				} else if (log.isDebugEnabled()) {
					log.debug("Certificate \"" + alias + "\" expiration date: " + Loggers.dateLog(max_date));
				}
			}
		}
	}
	
}
