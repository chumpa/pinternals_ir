package com.pinternals.ir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.NTLMScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Utils {
	public static final Charset utf8 = NotesRetriever.utf8;
	
	public static void streamToFile(InputStream is, OutputStream fos, Charset from, Charset to) throws IOException {
    	BufferedReader br = new BufferedReader(new InputStreamReader(is, from));
    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, to));
    	
    	if (to.equals(utf8)) {	// add BOM utf-8
	    	fos.write(0xEF);
	    	fos.write(0xBB);
	    	fos.write(0xBF);
    	}
    	char[] cb = new char[65536];
    	int i = br.read(cb);
    	while (i>0) {
    		bw.write(cb, 0, i);
    		i = br.read(cb);
    		bw.flush();
    	}
    	bw.close();
    	fos.close();
    	br.close();
	}
	
	static sun.misc.BASE64Decoder base64decoder = new BASE64Decoder();

	/**
	 * retrieve passwd from ./uname.pswd 
	 * @param uname 
	 * @return
	 * @throws IOException
	 */
	public static String getPasswd(String uname) throws IOException {
		assert uname !=null && !uname.equals("");
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			DESKeySpec keySpec = new DESKeySpec(uname.getBytes(utf8)); 
			SecretKey key = keyFactory.generateSecret(keySpec);
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(uname + ".pwd"), utf8));
			byte[] encrypedPwdBytes = base64decoder.decodeBuffer(br.readLine());
			
			Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));
			
			br.close();
			br = null;
			return new String(plainTextPwdBytes, utf8);
		} catch ( InvalidKeyException | NoSuchAlgorithmException |
				InvalidKeySpecException | NoSuchPaddingException |
				IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException (e.getMessage());
		}
	}

	/**
	 * store encrypted password into ./uname.pwd 
	 * @param uname username
	 * @param passwd plain-text password
	 * @return
	 * @throws IOException
	 */
	static boolean putPasswd(String uname, String passwd) throws IOException {
		assert uname !=null && !uname.equals(""); 
		assert passwd !=null && !passwd.equals("");
		try {
			DESKeySpec keySpec = new DESKeySpec(uname.getBytes(utf8)); 
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
			SecretKey key = keyFactory.generateSecret(keySpec);
			sun.misc.BASE64Encoder base64encoder = new BASE64Encoder();
			byte[] cleartext = passwd.getBytes(utf8);
			Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
			cipher.init(Cipher.ENCRYPT_MODE, key);
			String encryptedPwd = base64encoder.encode(cipher.doFinal(cleartext));
			FileOutputStream fos = new FileOutputStream(uname + ".pwd");
			fos.write(encryptedPwd.getBytes(utf8));
			fos.flush();
			fos.close();
			return true;
		} catch ( InvalidKeyException | NoSuchAlgorithmException |
				InvalidKeySpecException | NoSuchPaddingException |
				IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException (e.getMessage());
		}
	}
	
	public static HttpHost createProxyHost(String v) throws IOException {
		URL u = new URL(v);
		return new HttpHost(u.getHost(), u.getPort());
	}
	public static Credentials createProxyCred(String v) throws IOException {
		URL u = new URL(v);
		String uname = u.getUserInfo();
		String domain = u.getPath();
		// TODO add local workstation here
		NTCredentials nt = new NTCredentials(uname, getPasswd(uname), "", domain);
		return nt;
	}
	
	public static CloseableHttpClient makeHttpClient(String suname, HttpHost prHost, Credentials cred) throws IOException {
        CredentialsProvider cp = new BasicCredentialsProvider();
        String passwd = getPasswd(suname);
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(suname, passwd));
        passwd = null;
        SSLContext sslctx;
		try {
			sslctx = SSLContexts.custom()
			        .useProtocol("TLS")
			        .loadTrustMaterial(null, new TrustStrategy() {
			            @Override
			            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException
			            {
			                return true;
			            }}).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IOException(e.getMessage());
		}
        CloseableHttpClient cl;
        HttpClientBuilder cb = HttpClients.custom()
        		.useSystemProperties()
        		.setDefaultCredentialsProvider(cp)
        		.setSSLContext(sslctx);
		if (prHost!=null) {
			cb.setProxy(prHost);
	        if (cred!=null) cp.setCredentials(new AuthScope(prHost), cred);
		}
		cl = cb.build();
		return cl;
	}
	
	static void authProxy(HttpContext htx, Credentials cred) throws IOException {
		AuthState authState = new AuthState();
		System.out.println(cred);
		authState.update(new NTLMScheme(), cred);
		htx.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);
	}
	
	public static void main(String args[]) throws Exception {
//		Cache.fs.getPath("")
	}
	@Deprecated
	public static void main_https(String args[]) throws Exception {
//		System.setProperty("java.net.useSystemProxies","true");
		URI lnp = new URI("https://launchpad.support.sap.com/");
		
		CloseableHttpClient cl;
		HttpClientContext htx;
        List<Proxy> l = ProxySelector.getDefault().select(lnp);
        HttpHost prHost = null;
        if (l.size()>0) {
            Proxy prx = l.get(0);
            if (prx.type().equals(Proxy.Type.HTTP)) {
            	InetSocketAddress ia = (InetSocketAddress)prx.address();
            	prHost = new HttpHost(ia.getHostString(), ia.getPort());
            }
        } 
        
        if (prHost==null) {
        	prHost = new HttpHost("....", 8080);
        }
		NTCredentials cred = new NTCredentials("", "", "", "");
		CredentialsProvider cp = new BasicCredentialsProvider();
    		cp.setCredentials(new AuthScope(prHost), cred);
//    		cp.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
    		
		cl = HttpClients.custom()
        		.useSystemProperties()
        		.setDefaultCredentialsProvider(cp)
        		.setProxy(prHost)
        		.build();
		htx = HttpClientContext.create();
		AuthState authState = new AuthState();
		authState.update(new NTLMScheme(), cred);
		htx.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);
		HttpGet get;
		CloseableHttpResponse rsp;
        get = new HttpGet("https://launchpad.support.sap.com/services/odata/svt/snogwscorr/");
		rsp = cl.execute(get, htx);
        rsp.getEntity().writeTo(System.out);
	}
	
//	public static void main2(String args[]) throws Exception {
//		System.out.println("Test: prxuname wks domain prx-host prx-port");
//		HttpHost prHost = new HttpHost(args[3], Integer.valueOf(args[4]));
////		d = new NTCredentials(args[0], Utils.getPasswd(args[0]), args[1], args[2]);
//		CredentialsProvider cp = new BasicCredentialsProvider();
////		cp.setCredentials(new AuthScope(prHost), cred);
//		CloseableHttpClient cl = HttpClients.custom()
//        		.useSystemProperties()
//        		.setDefaultCredentialsProvider(cp)
//        		.setProxy(prHost)
//        		.build();
//		HttpClientContext htx = HttpClientContext.create();
//		AuthState authState = new AuthState();
////		authState.update(new NTLMScheme(), cred);
//		htx.setAttribute(HttpClientContext.PROXY_AUTH_STATE, authState);
//		HttpGet g = new HttpGet("http://sap.com");
//		CloseableHttpResponse rsp = cl.execute(g, htx);
//    	StatusLine l = rsp.getStatusLine();
//    	System.out.println(l);
//	}
}