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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
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

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;

/**
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=geoserver-enterprise-master/src/web/app/src/test/java/org/geoserver/web/Start.java
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=LittleProxy-mitm-master/src/main/java/org/littleshoot/proxy/mitm/CertificateHelper.java
 */
public class KeystoreTool {
	
	private static Logger log = Logger.getLogger(KeystoreTool.class);
	private final KeyStore keystore;
	private final KeyManagerFactory key_manager_factory;
	private final TrustManagerFactory trust_manager_factory;
	
	static {
		MyDMAM.checkJVM();
		Security.setProperty("crypto.policy", "unlimited");
		// Security.addProvider(new BouncyCastleProvider());
	}
	
	private static final long SELF_GEN_CERTIFICATE_VALIDITY_DURATION = TimeUnit.DAYS.toMillis(360l * 10l);
	private static final long WARN_BEFORE_TIME_EXPIRATION_CERTIFICATES = TimeUnit.DAYS.toMillis(360);
	
	private final int private_key_size;
	private final String keygen_algorithm;
	private final String[] cipher_suite;
	private final String protocol;
	private final String signature_algorithm;
	private final String secure_random_algorithm;
	private final String kmf_algorithm;
	
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
		
		protocol = Configuration.global.getValue("tls_security", "protocol", "TLSv1.2");
		private_key_size = Configuration.global.getValue("tls_security", "private_key_size", 256);
		keygen_algorithm = Configuration.global.getValue("tls_security", "keygen_algorithm", "EC");
		secure_random_algorithm = Configuration.global.getValue("tls_security", "secure_random_algorithm", "SHA1PRNG");
		kmf_algorithm = Configuration.global.getValue("tls_security", "kmf_algorithm", "SunX509");
		
		/**
		 * signature_algorithms: SHA512withECDSA, SHA512withRSA, SHA384withECDSA, SHA384withRSA, SHA256withECDSA, SHA256withRSA, SHA256withDSA, SHA224withECDSA, SHA224withRSA, SHA224withDSA, SHA1withECDSA, SHA1withRSA, SHA1withDSA
		 */
		signature_algorithm = Configuration.global.getValue("tls_security", "signature_algorithm", "SHA512withECDSA");
		
		ArrayList<String> configured_cipher_suite = Configuration.global.getValues("tls_security", "cipher_suite", null);
		if (configured_cipher_suite == null) {
			/**
			 * https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices
			 */
			cipher_suite = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384" };
			/*
			"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
			"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"
			"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384"
			"TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"
			
			"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"
			"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
			"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
			"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
			"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256"
			
			"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA"
			"TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA"
			"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
			"TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"
			"TLS_DHE_RSA_WITH_AES_128_CBC_SHA"
			"TLS_DHE_RSA_WITH_AES_256_CBC_SHA"
			*/
		} else {
			cipher_suite = new String[configured_cipher_suite.size()];
			for (int pos = 0; pos < cipher_suite.length; pos++) {
				cipher_suite[pos] = configured_cipher_suite.get(pos);
			}
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
			
			KeyPairGenerator kp_generator = KeyPairGenerator.getInstance(keygen_algorithm);
			SecureRandom secureRandom = SecureRandom.getInstance(secure_random_algorithm);
			kp_generator.initialize(private_key_size, secureRandom);
			KeyPair keyPair = kp_generator.generateKeyPair();
			
			int random = new SecureRandom().nextInt();
			if (random < 0) random *= -1;
			BigInteger serial = BigInteger.valueOf(random);
			long expiration_date = System.currentTimeMillis() + SELF_GEN_CERTIFICATE_VALIDITY_DURATION;
			
			log.info("Generate keystore for \"" + x590_principal_hostname + "\", with a self-signed certificate and a key size " + private_key_size + " bits, a validity up to the " + Loggers.dateLog(expiration_date) + " and a S/N " + serial.toString(16));
			
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
			
			ContentSigner signer = new JcaContentSignerBuilder(signature_algorithm).build(keyPair.getPrivate());
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
		key_manager_factory = KeyManagerFactory.getInstance(kmf_algorithm);
		key_manager_factory.init(keystore, keystore_password.toCharArray());
		
		/**
		 * TrustManager's decide whether to allow connections.
		 */
		trust_manager_factory = TrustManagerFactory.getInstance(kmf_algorithm);
		trust_manager_factory.init(keystore);
	}
	
	SSLContext createTLSContext() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ssl_context = SSLContext.getInstance(protocol);
		ssl_context.init(key_manager_factory.getKeyManagers(), trust_manager_factory.getTrustManagers(), null);
		
		SSLParameters default_param = ssl_context.getDefaultSSLParameters();
		default_param.setProtocols(new String[] { protocol });
		default_param.setCipherSuites(cipher_suite);
		return ssl_context;
	}
	
	public void checkSecurity(SSLEngine engine) throws SSLException {
		String engine_protocol = engine.getSession().getProtocol();
		String engine_cipher_suite = engine.getSession().getCipherSuite();
		
		if (protocol.equalsIgnoreCase(engine_protocol) == false) {
			throw new SSLException("Invalid protocol: " + engine_protocol + ", only " + protocol + " is avaliable");
		}
		
		List<String> all_cipher_suite = Arrays.asList(cipher_suite);
		if (all_cipher_suite.stream().anyMatch(c -> c.equalsIgnoreCase(engine_cipher_suite)) == false) {
			throw new SSLException("Invalid cipher_suite: " + engine_cipher_suite + ", only " + all_cipher_suite + " are avaliable");
		}
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
