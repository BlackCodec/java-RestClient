/** @Release: 20230426.0900 */
package it.icapito.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RestClient {
	
	protected static class HttpMessage {
		private Map<String,String> headers = new HashMap<>();
		private String body = null;
		
		protected HttpMessage() {}
		public Map<String,String> getHeaders() { return this.headers; }
		public String getBody() { return this.body; }
		public String getKey(String key) { return (this.containsHeader(key)?this.headers.get(key):null); }
		public boolean hasBody() { return RestClient.stringValidate(this.body); }
		public boolean hasHeaders() { return !this.headers.isEmpty(); }
		public boolean containsHeader(String key) {  return (this.headers.containsKey(key) && RestClient.stringValidate(this.headers.get(key))); }
		public void setBody(String bodyString) { this.body = bodyString; }
		public void setHeaders(Map<String,String> headers) { this.headers = headers; }
		public void addHeader(String key, String value) { this.headers.put(key, value); }
		public void clearHeaders() { this.headers.clear(); }
	}
	
	public enum HttpMethod { GET, POST, PUT, DELETE, PATCH, HEAD, TRACE, CONNECT }
	
	public static class HttpRequest extends HttpMessage {
		private HttpMethod method = HttpMethod.GET;
		private String url = null;
		private HttpResponse response = null;
		private SSLSocketFactory defaultSocketFactory;
		private HostnameVerifier defaultHostnameVerifier;
		private Exception error = null;
		
		public HttpRequest() {
			super();
			this.defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
			this.defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
		}
		public void setMethod(HttpMethod method) { this.method = method; }
		public void setUrl(String url) { this.url = url.trim(); }
		public String getUrl() { return this.url; }
		public HttpMethod getMethod() { return this.method; }
		public void setResponse(HttpResponse response) { this.response = response; }
		public HttpResponse getResponse() { return this.response; }
		public void addBasicAuthentication(String username, String password) {
			String encodedAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
			this.addHeader("Authorization", encodedAuth);
		}
		public void setContentType(String type) { this.addHeader("Content-type", type); }
		public boolean isError() { return this.error != null; }
		public String getErrorMessage() { return (this.isError()?this.error.getMessage():""); }
		public boolean execute() {
			HttpsURLConnection connection = null;
			try {
				URL urlObj = new URL(this.url);
				connection = (HttpsURLConnection) urlObj.openConnection();
				if (this.isSupportedMethod(connection) connection.setRequestMethod(this.method.name());
				else this.setRequestMethod(connection);
				for (Entry<String, String> header: this.getHeaders().entrySet()) 
					connection.addRequestProperty(header.getKey(), header.getValue());
				if (this.hasBody()) {
					connection.setDoOutput(true);
					try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
						out.writeBytes(this.getBody()); out.flush();
					} catch (Exception e) { this.error = e; return false; }
				}
				String bodyString = this.getStringFromStream(connection.getInputStream());
				if (!RestClient.stringValidate(bodyString)) bodyString = this.getStringFromStream(connection.getErrorStream());
				this.response = new HttpResponse();
				this.response.setCode(connection.getResponseCode());
				this.response.setBody(bodyString);
				return true;
			} catch (IOException e) { this.error = e; return false;
			} finally { if (connection != null) connection.disconnect(); }
		}
		private String getStringFromStream(InputStream inputStream) throws IOException {
			StringBuilder contenuto = new StringBuilder();
			String line;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				while ((line = reader.readLine()) != null) 
					contenuto.append(line);
			} 
			return contenuto.toString();
		}
		public void enableCertificateValidation() {
			HttpsURLConnection.setDefaultSSLSocketFactory(this.defaultSocketFactory);
			HttpsURLConnection.setDefaultHostnameVerifier(this.defaultHostnameVerifier);
		}
		public boolean disableCertificateValidation() {
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, (new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
					@Override
					public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {}
					@Override
					public void checkServerTrusted(X509Certificate[] chanin, String authType) throws CertificateException {}
				}}), new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) { return true; }
				});
				return true;
			} catch (NoSuchAlgorithmException| KeyManagementException e) {
				this.error = e;
				return false;
			}
		}

		private boolean isSupportedMethod(HttpsURLConnection con) {
			try {
				Field methods = HttpURLConnection.class.getDeclaredField("methods");
				methods.setAccessible(true);
				Object value = methods.get(con);
				String[] values = (String[])value;
				for (String m: values)
					if (m.equalsIgnoreCase(this.method.name())) return true;
			} catch(Exception e) {
				this.error = e;
			}
			return false;
		}
	
		private boolean setRequestMethod(final HttpsURLConnection c) {
			try {
			    	final Object target;
	        		if (c instanceof sun.net.www.protocol.https.HttpsURLConnectionImpl) {
	        			final Field delegate = sun.net.www.protocol.https.HttpsURLConnectionImpl.class.getDeclaredField("delegate");
					delegate.setAccessible(true);
					target = delegate.get(c);
			        } else
	        		    target = c;
				final Field f = HttpURLConnection.class.getDeclaredField("method");
				f.setAccessible(true);
				f.set(target, this.method.name());
				return true;
			} catch (IllegalAccessException | NoSuchFieldException e) {
				this.error = e;
			}
			return false;
		}
	}
	
	public static class HttpResponse extends HttpMessage {
		private int code;
		private boolean isError = false;
		
		public HttpResponse() { super(); }
		public void setCode(int code) { this.code = code; }
		public void setSuccess() { this.isError = false; }
		public void setError() { this.isError = true; }
		public boolean isError() { return this.isError; }
		public boolean isSuccess() { return !this.isError(); }
		public int getResponseCode() { return this.code; }
	}
	
	public static HttpRequest createRequest() { return new HttpRequest(); }
	private static boolean stringValidate(String input) {  return (input != null && !input.trim().isEmpty() && !input.trim().equalsIgnoreCase("null") && !input.trim().equalsIgnoreCase("")); }
}
