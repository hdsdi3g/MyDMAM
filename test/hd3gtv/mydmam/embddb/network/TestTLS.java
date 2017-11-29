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
 * Copyright (C) hdsdi3g for hd3g.tv 28 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class TestTLS /*extends TestCase*/ {
	
	private static final Logger log = Logger.getLogger(TestTLS.class);
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/*Arrays.asList(Security.getProviders()).forEach(p -> {
	System.out.println(p.getName());
	p.entrySet().forEach(entry -> {
		System.out.println("\t" + entry.getKey() + "\t\t" + entry.getValue().toString());
	});
	});*/
	
	/*SSLContext context = SSLContext.getInstance("TLSv1.2");
	System.out.println(context.getProtocol()); // TLSv1.2
	System.out.println(context.getProvider());// SunJSSE version 1.8
	
	*/
	
	private static boolean keyStoreContainsCertificate(KeyStore ks, String hostname) throws KeyStoreException {
		Enumeration<String> e = ks.aliases();
		while (e.hasMoreElements()) {
			String alias = e.nextElement();
			if (ks.isCertificateEntry(alias)) {
				Certificate c = ks.getCertificate(alias);
				if (c instanceof X509Certificate) {
					X500Principal p = (X500Principal) ((X509Certificate) c).getSubjectX500Principal();
					if (p.getName().contains(hostname)) return true;
				}
			}
		}
		return false;
	}
	
	public static final String KEY_PAIR_GENERATOR_NAME = "RSA";// ECDSA is not avaliable
	public static final int KEY_PAIR_GENERATOR_SIZE = 2048;
	public static final String PROTOCOL = "TLSv1.2";
	public static final String ALGORITHM = "SunX509";
	
	public static final String[] CIPHER_SUITE;
	
	static {
		/**
		 * https://github.com/ssllabs/research/wiki/SSL-and-TLS-Deployment-Best-Practices
		 */
		ArrayList<String> la_CIPHER_SUITE = new ArrayList<>();
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
		// la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256");
		la_CIPHER_SUITE.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
		// la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
		la_CIPHER_SUITE.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");
		// la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
		// la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_CBC_SHA");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256");
		la_CIPHER_SUITE.add("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
		
		CIPHER_SUITE = new String[la_CIPHER_SUITE.size()];
		for (int pos = 0; pos < CIPHER_SUITE.length; pos++) {
			CIPHER_SUITE[pos] = la_CIPHER_SUITE.get(pos);
		}
	}
	
	/**
	 * signature_algorithms: SHA512withECDSA, SHA512withRSA, SHA384withECDSA, SHA384withRSA, SHA256withECDSA, SHA256withRSA, SHA256withDSA, SHA224withECDSA, SHA224withRSA, SHA224withDSA, SHA1withECDSA, SHA1withRSA, SHA1withDSA
	 */
	public static final String SIGNATURE_ALGORITHM = "sha256WithRSAEncryption";
	
	static SSLContext createContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException, InvalidKeyException, SecurityException, SignatureException {
		/**
		 * Generate a JKS
		 * https://www.programcreek.com/java-api-examples/index.php?source_dir=geoserver-enterprise-master/src/web/app/src/test/java/org/geoserver/web/Start.java
		 */
		String hostname = "me";
		File keyStoreFile = new File("ks.pks");
		String password = "test";
		
		KeyStore privateKS = KeyStore.getInstance("JKS");
		if (keyStoreFile.exists()) {
			FileInputStream fis = new FileInputStream(keyStoreFile);
			privateKS.load(fis, password.toCharArray());
			
			if (keyStoreContainsCertificate(privateKS, hostname) == false) {
				throw new IOException("Oh No!");
			}
		} else {
			privateKS.load(null);
			
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_NAME);
			keyPairGenerator.initialize(KEY_PAIR_GENERATOR_SIZE);
			KeyPair KPair = keyPairGenerator.generateKeyPair();
			
			// cerate a X509 certifacte generator
			X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
			
			// set validity to 10 years, issuer and subject are equal --> self singed certificate
			int random = new SecureRandom().nextInt();
			if (random < 0) random *= -1;
			v3CertGen.setSerialNumber(BigInteger.valueOf(random));
			v3CertGen.setIssuerDN(new X509Principal("CN=" + hostname + ", OU=None, O=None L=None, C=None"));
			v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
			v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)));
			v3CertGen.setSubjectDN(new X509Principal("CN=" + hostname + ", OU=None, O=None L=None, C=None"));
			
			v3CertGen.setPublicKey(KPair.getPublic());
			v3CertGen.setSignatureAlgorithm(SIGNATURE_ALGORITHM);
			
			X509Certificate pk_certificate = v3CertGen.generateX509Certificate(KPair.getPrivate());
			
			// store the certificate containing the public key,this file is needed
			// to import the public key in other key store.
			// File certFile = new File(keyStoreFile.getParentFile(), hostname + ".cert");
			// FileOutputStream fos = new FileOutputStream(certFile.getAbsoluteFile());
			// fos.write(PKCertificate.getEncoded());
			// fos.close();
			
			privateKS.setKeyEntry(hostname + ".key", KPair.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[] { pk_certificate });
			privateKS.setCertificateEntry(hostname + ".cert", pk_certificate);
			privateKS.store(new FileOutputStream(keyStoreFile), password.toCharArray());
		}
		
		/**
		 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
		 */
		// KeyManager's decide which key material to use.
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(ALGORITHM);
		kmf.init(privateKS, password.toCharArray());
		
		// TrustManager's decide whether to allow connections.
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(ALGORITHM);
		tmf.init(privateKS);
		
		SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		
		return sslContext;
	}
	
	public static void main(String[] args) throws Exception {
		
		new TLSEngineSimpleDemo(createContext());
		
		System.exit(0);
		
		/**
		 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
		 */
		SSLEngine engine_client = createContext().createSSLEngine();
		engine_client.setUseClientMode(true);
		
		SSLSession session = engine_client.getSession();
		/**
		 * http://www.onjava.com/2004/11/03/ssl-nio.html
		 * inNetData: Stores data received directly from the network. This consists of encrypted data and handshake information. This buffer is filled with data read from the socket and emptied by SSLEngine.unwrap().
		 * inAppData: Stores decrypted data received from the peer. This buffer is filled by SSLEngine.unwrap() with decrypted application data and emptied by the application.
		 * outAppData: Stores decrypted application data that is to be sent to the other peer. The application fills this buffer, which is then emptied by SSLEngine.wrap().
		 * outNetData: Stores data that is to be sent to the network, including handshake and encrypted application data. This buffer is filled by SSLEngine.wrap() and emptied by writing it to the network.
		 */
		
		ByteBuffer myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		ByteBuffer myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		ByteBuffer peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		ByteBuffer peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		
		SocketChannel socketChannel = new SocketChannel();
		// Do initial handshake
		doHandshake(socketChannel, engine_client, myNetData, peerNetData);
		
		myAppData.put("hello".getBytes());
		myAppData.flip();
		
		while (myAppData.hasRemaining()) {
			// Generate SSL/TLS encoded data (handshake or application data)
			SSLEngineResult res = engine_client.wrap(myAppData, myNetData);
			
			switch (res.getStatus()) {
			case BUFFER_OVERFLOW:
				// Maybe need to enlarge the peer application data buffer.
				if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
					// enlarge the peer application data buffer
				} else {
					// compact or clear the buffer
				}
				// retry the operation
				break;
			
			case BUFFER_UNDERFLOW:
				// Maybe need to enlarge the peer network packet buffer
				if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					// enlarge the peer network packet buffer
				} else {
					// compact or clear the buffer
				}
				// obtain more inbound network data and then retry the operation
				break;
			case CLOSED:
				return;
			case OK:
				myAppData.compact();
				
				// Send SSL/TLS encoded data to peer
				while (myNetData.hasRemaining()) {
					int num = socketChannel.write(myNetData);
					if (num == -1) {
						// handle closed channel
					} else if (num == 0) {
						// no bytes written; try again later
					}
				}
				break;
			}
		}
		
		// Read TLS encoded data from peer
		int num = socketChannel.read(peerNetData);
		if (num == -1) {
			// Handle closed channel
		} else if (num == 0) {
			// No bytes read; try again ...
		} else {
			// Process incoming data
			peerNetData.flip();
			SSLEngineResult res = engine_client.unwrap(peerNetData, peerAppData);
			
			switch (res.getStatus()) {
			case BUFFER_OVERFLOW:
				// Maybe need to enlarge the peer application data buffer.
				if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
					// enlarge the peer application data buffer
				} else {
					// compact or clear the buffer
				}
				// retry the operation
				break;
			
			case BUFFER_UNDERFLOW:
				// Maybe need to enlarge the peer network packet buffer
				if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					// enlarge the peer network packet buffer
				} else {
					// compact or clear the buffer
				}
				// obtain more inbound network data and then retry the operation
				break;
			case CLOSED:
				return;
			case OK:
				peerNetData.compact();
				
				if (peerAppData.hasRemaining()) {
					// Use peerAppData
				}
				break;
			}
		}
	}
	
	private static class SocketChannel {
		
		private ByteBuffer internal = null;
		
		int read(ByteBuffer buffer) {
			return 0;// XXX
		}
		
		public int write(ByteBuffer buffer) {
			return 0;// XXX
		}
	}
	
	/**
	 * https://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLENG
	 */
	static void doHandshake(SocketChannel socketChannel, SSLEngine engine_client, ByteBuffer myNetData, ByteBuffer peerNetData) throws Exception {
		int appBufferSize = engine_client.getSession().getApplicationBufferSize();
		ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
		ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
		
		engine_client.beginHandshake();
		SSLEngineResult.HandshakeStatus hs = engine_client.getHandshakeStatus();
		
		while (true) {
			switch (hs) {
			case NEED_UNWRAP:
				if (socketChannel.read(peerNetData) < 0) {
					log.info("Channel wil be closed");
					System.exit(0);
				}
				
				// Process incoming handshaking data
				peerNetData.flip();
				SSLEngineResult res = engine_client.unwrap(peerNetData, peerAppData);
				peerNetData.compact();
				hs = res.getHandshakeStatus();
				
				// Check status
				switch (res.getStatus()) {
				case OK:
					// Handle OK status
					break;
				case BUFFER_OVERFLOW:
					// Maybe need to enlarge the peer application data buffer.
					if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
						// enlarge the peer application data buffer
					} else {
						// compact or clear the buffer
					}
					// retry the operation
					break;
				
				case BUFFER_UNDERFLOW:
					// Maybe need to enlarge the peer network packet buffer
					if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
						// enlarge the peer network packet buffer
					} else {
						// compact or clear the buffer
					}
					// obtain more inbound network data and then retry the operation
					break;
				case CLOSED:
					break;
				}
				break;
			case NEED_WRAP:
				// Empty the local network packet buffer.
				myNetData.clear();
				
				// Generate handshaking data
				res = engine_client.wrap(myAppData, myNetData);
				hs = res.getHandshakeStatus();
				
				// Check status
				switch (res.getStatus()) {
				case OK:
					myNetData.flip();
					
					// Send the handshaking data to peer
					while (myNetData.hasRemaining()) {
						if (socketChannel.write(myNetData) < 0) {
							// Handle closed channel
						}
					}
					break;
				case BUFFER_OVERFLOW:
					// Maybe need to enlarge the peer application data buffer.
					if (engine_client.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
						// enlarge the peer application data buffer
					} else {
						// compact or clear the buffer
					}
					// retry the operation
					break;
				
				case BUFFER_UNDERFLOW:
					// Maybe need to enlarge the peer network packet buffer
					if (engine_client.getSession().getPacketBufferSize() > peerNetData.capacity()) {
						// enlarge the peer network packet buffer
					} else {
						// compact or clear the buffer
					}
					// obtain more inbound network data and then retry the operation
					break;
				case CLOSED:
					break;
				}
				break;
			case NEED_TASK:
				Runnable task;
				while ((task = engine_client.getDelegatedTask()) != null) {
					// new Thread(task).start();
					task.run();
				}
				break;
			case FINISHED:
				return;
			case NOT_HANDSHAKING:
				return;
			}
		}
	}
}
