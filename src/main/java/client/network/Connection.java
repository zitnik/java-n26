/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.network;


import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;

class Connection {

	protected static final SSLSocketFactory SSL_SOCKET_FACTORY;
	static{
		try {
			InputStream is = null;
			try {
				is = Connection.class.getClassLoader().getResourceAsStream("n26apikeystore.jks");
				KeyStore trusted = KeyStore.getInstance("JKS");
				trusted.load(is, "287ews".toCharArray());
				String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
				TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
				tmf.init(trusted);
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(null, tmf.getTrustManagers(), null);
				SSL_SOCKET_FACTORY = context.getSocketFactory();
			}finally {
				is.close();
			}
		} catch (Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public enum ErrorType{
		INTERNAL,
		NETWORK,
		PARSE
	}

	public interface ResponseListener{
		void success(int responseCode, String response);
		void error(ErrorType errorType);
	}

	public static void makeGetRequest(String endpoint, Map<String, String> headers, ResponseListener responseListener){
		makeRequest("GET", endpoint, headers, null, responseListener);
	}

	public static void makePostRequest(String endpoint, Map<String, String> headers, String data, ResponseListener responseListener){
		makeRequest("POST", endpoint, headers, data.toString(), responseListener);
	}

	public static void makePostRequest(String endpoint, Map<String, String> headers, JSONObject data, ResponseListener responseListener){
		headers.put("Content-Type", "application/json");
		makeRequest("POST", endpoint, headers, data.toString(), responseListener);
	}

	public static void makeRequest(final String method, final String endpoint, final Map<String, String> headers,
								   final String data, final ResponseListener responseListener){
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Response r = makeSynchronousRequest(method, endpoint, headers, data);
				if (r.exception != null) {
					r.exception.printStackTrace();
					if (r.exception instanceof MalformedURLException ||
							r.exception instanceof ProtocolException ||
							r.exception instanceof UnsupportedEncodingException)
						responseListener.error(ErrorType.INTERNAL);
					else
						responseListener.error(ErrorType.NETWORK);
				} else {
					try {
						responseListener.success(r.code, r.body);
					} catch (JSONException e) {
						responseListener.error(ErrorType.PARSE);
					}
				}
			}
		};
		Thread requestThread = new Thread(runnable);
		requestThread.run();
	}

	protected static Response makeSynchronousRequest(String method, String endpoint, Map<String, String> headers, String data) {
		try {
			URL url = new URL("https", "api.tech26.de", endpoint);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
			if(headers!=null) {
				for (Map.Entry<String, String> i : headers.entrySet()) {
					connection.addRequestProperty(i.getKey(), i.getValue());
				}
			}
			connection.setRequestMethod(method);
			if(data!=null){
				connection.setDoOutput(true);
				byte[] body = data.toString().getBytes("UTF-8");
				connection.setRequestProperty("Content-Length", Integer.toString(body.length));
				OutputStream os = null;
				try {
					os = connection.getOutputStream();
					os.write(body);
				}finally {
					if (os!=null) os.close();
				}
			}

			int responseCode = connection.getResponseCode();
			boolean responseError = true;
			InputStream inputStream = connection.getErrorStream();
			if (inputStream == null) {
				inputStream = connection.getInputStream();
				responseError = false;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			while (true) {
				String a = reader.readLine();
				if (a == null) break;
				sb.append(a);
			}
			inputStream.close();
			String response = sb.length() > 0 ? sb.toString() : null;

			return new Response(response, responseCode, responseError, null);
		} catch (IOException e) {
			return new Response(null, -1, false, e);
		}
	}

	protected static class Response{
		String body;
		int code;
		boolean responseError;
		Exception exception;

		public Response(String body, int code, boolean responseError, Exception exception) {
			this.body = body;
			this.code = code;
			this.responseError = responseError;
			this.exception = exception;
		}
	}
}
