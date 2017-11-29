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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import hd3gtv.mydmam.Loggers;

/**
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=geoserver-enterprise-master/src/web/app/src/test/java/org/geoserver/web/Start.java
 * @see https://www.programcreek.com/java-api-examples/index.php?source_dir=LittleProxy-mitm-master/src/main/java/org/littleshoot/proxy/mitm/CertificateHelper.java
 */
public class KeystoreTool {// TODO tests
	
	private static Logger log = Logger.getLogger(KeystoreTool.class);
	
	private final KeyStore privateKS;
	private static final int PRIVATE_KEY_SIZE = 2048;
	
	private static final String KEYGEN_ALGORITHM = "RSA";
	private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";// XXX upgrade
	private static final String SIGNATURE_ALGORITHM = "SHA1WithRSAEncryption";// XXX upgrade
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	// TODO warn validity expiration at boot
	
	public KeystoreTool(File keystore_file, String keystore_password, String x590_principal_hostname) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, SecurityException, SignatureException, OperatorCreationException, NoSuchProviderException {
		if (keystore_file == null) {
			throw new NullPointerException("\"keystore_file\" can't to be null");
		}
		if (keystore_password == null) {
			throw new NullPointerException("\"keystore_password\" can't to be null");
		}
		
		privateKS = KeyStore.getInstance("JKS");
		
		if (keystore_file.exists()) {
			FileInputStream fis = new FileInputStream(keystore_file);
			log.info("Load keystore file: " + keystore_file.getPath());
			privateKS.load(fis, keystore_password.toCharArray());
			fis.close();
			if (isContainsCertificateHostname(privateKS, x590_principal_hostname) == false) {
				throw new CertificateException("Invalid pks file " + keystore_file.getName() + ": missing \"" + x590_principal_hostname + "\" in x590 principal. You must regenerate it or change expected x590_principal_hostname");
			}
		}
		
		privateKS.load(null);
		
		KeyPair keyPair = generateKeyPair(PRIVATE_KEY_SIZE);
		
		int random = new SecureRandom().nextInt();
		if (random < 0) random *= -1;
		BigInteger serial = BigInteger.valueOf(random);
		long expiration_date = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(360l * 10l);
		
		log.info("Generate keystore for \"" + x590_principal_hostname + "\", with a self-signed certificate and a key size " + PRIVATE_KEY_SIZE + " bits, a validity up to the " + Loggers.dateLog(expiration_date) + ", a S/N " + serial.toString(16));
		
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
		generator.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(pubKey));
		generator.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		
		KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
		generator.addExtension(Extension.keyUsage, false, usage);
		
		ASN1EncodableVector purposes = new ASN1EncodableVector();
		purposes.add(KeyPurposeId.id_kp_serverAuth);
		purposes.add(KeyPurposeId.id_kp_clientAuth);
		purposes.add(KeyPurposeId.anyExtendedKeyUsage);
		generator.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));
		
		X509Certificate PKCertificate = signCertificate(generator, keyPair.getPrivate());
		
		/*File certFile = new File(keystore_file.getParentFile(), hostname + ".cert");
		FileOutputStream fos = new FileOutputStream(certFile.getAbsoluteFile());
		fos.write(PKCertificate.getEncoded());
		fos.close();*/
		
		privateKS.setKeyEntry(x590_principal_hostname + ".key", keyPair.getPrivate(), keystore_password.toCharArray(), new java.security.cert.Certificate[] { PKCertificate });
		privateKS.setCertificateEntry(x590_principal_hostname + ".cert", PKCertificate);
		
		log.info("Save keystore file to " + keystore_file.getCanonicalPath());
		privateKS.store(new FileOutputStream(keystore_file), keystore_password.toCharArray());
	}
	
	private boolean isContainsCertificateHostname(KeyStore ks, String x590_principal_hostname) throws KeyStoreException {
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
	
	private static SubjectKeyIdentifier createSubjectKeyIdentifier(PublicKey key) throws IOException {
		ByteArrayInputStream bIn = new ByteArrayInputStream(key.getEncoded());
		ASN1InputStream is = null;
		try {
			is = new ASN1InputStream(bIn);
			ASN1Sequence seq = (ASN1Sequence) is.readObject();
			SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
			return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance(KEYGEN_ALGORITHM);
		SecureRandom secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
		generator.initialize(keySize, secureRandom);
		return generator.generateKeyPair();
	}
	
	private static X509Certificate signCertificate(X509v3CertificateBuilder certificateBuilder, PrivateKey signedWithPrivateKey) throws CertificateException, OperatorCreationException {
		ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider("BC").build(signedWithPrivateKey);
		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateBuilder.build(signer));
		return cert;
	}
	
}
