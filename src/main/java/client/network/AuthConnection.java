/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class AuthConnection {
	private String accessToken;
	private String tokenType;
	private String refreshToken;
	private Date expiration;
	private static final long EXPIRATION_MARGIN_MILLIS = 500;

	public enum ErrorType{
		INTERNAL,
		NETWORK,
		PARSE,
		AUTH,
		OTHER
	}
	public interface ResponseListener{
		void success(int responseCode, JSONObject response);
		void error(ErrorType errorType);
	}
	public interface ArrayResponseListener{
		void success(int responseCode, JSONArray response);
		void error(ErrorType errorType);
	}
	public interface StringResponseListener{
		void success(int responseCode, String response);
		void error(ErrorType errorType);
	}

	public interface LoginResponseListener{
		void success();
		void error(ErrorType errorType);
	}

	public void login(String username, String password, final LoginResponseListener responseListener){
		Map<String, String> header = new TreeMap<>();
		header.put("Authorization", "Basic bXktdHJ1c3RlZC13ZHBDbGllbnQ6c2VjcmV0");
		header.put("Content-Type", "application/x-www-form-urlencoded");
		String body = null;
		try {
			body = String.format("grant_type=password&username=%s&password=%s",
					URLEncoder.encode(username, "UTF-8"), URLEncoder.encode(password, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		final Date requestedTime = new Date();
		Connection.makePostRequest("/oauth/token", header, body, new Connection.ResponseListener() {
			@Override
			public void success(int responseCode, String response) {
				try {
					JSONObject jsonResponse = new JSONObject(response);
					accessToken = jsonResponse.getString("access_token");
					tokenType = jsonResponse.getString("token_type");
					refreshToken = jsonResponse.getString("refresh_token");
					expiration = new Date(requestedTime.getTime() + jsonResponse.getInt("expires_in") * 100);
					responseListener.success();
				}catch(JSONException e){
					responseListener.error(ErrorType.AUTH);
				}
			}

			@Override
			public void error(Connection.ErrorType errorType) {
				responseListener.error(ErrorType.valueOf(errorType.name()));
			}
		});
	}

	private void renewToken(final LoginResponseListener responseListener) {
		Map<String, String> header = new TreeMap<>();
		header.put("Authorization", "Basic bXktdHJ1c3RlZC13ZHBDbGllbnQ6c2VjcmV0");
		header.put("Content-Type", "application/x-www-form-urlencoded");
		String body = null;
		try {
			body = String.format("grant_type=refresh_token&refresh_token=%s",
					URLEncoder.encode(refreshToken, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		final Date requestedTime = new Date();
		Connection.makePostRequest("/oauth/token", header, body, new Connection.ResponseListener() {
			@Override
			public void success(int responseCode, String response) {
				try {
					JSONObject jsonResponse = new JSONObject(response);
					accessToken = jsonResponse.getString("access_token");
					tokenType = jsonResponse.getString("token_type");
					refreshToken = jsonResponse.getString("refresh_token");
					expiration = new Date(requestedTime.getTime() + jsonResponse.getInt("expires_in") * 1000);
					responseListener.success();
				}catch(JSONException e){
					responseListener.error(ErrorType.AUTH);
				}
			}

			@Override
			public void error(Connection.ErrorType errorType) {
				responseListener.error(ErrorType.valueOf(errorType.name()));
			}
		});
	}

	public void makeRequest(final String method, final String endpoint, final Map<String, String> headers, final String data,
							final StringResponseListener responseListener){
		if(new Date().getTime() + EXPIRATION_MARGIN_MILLIS > expiration.getTime()){
			renewToken(new LoginResponseListener(){
				@Override
				public void success() {
					makeRequest(method, endpoint, headers, data, responseListener);
				}

				@Override
				public void error(ErrorType errorType) {
					responseListener.error(ErrorType.AUTH);
				}
			});
		}
		headers.put("Authorization", String.format("%s %s", tokenType, accessToken));
		Connection.makeRequest(method, endpoint, headers, data, new Connection.ResponseListener() {
			@Override
			public void success(int responseCode, String response) {
				if(responseCode>=401 && responseCode<=403){
					responseListener.error(ErrorType.AUTH);
				}else{
					responseListener.success(responseCode, response);
				}
			}

			@Override
			public void error(Connection.ErrorType errorType) {
				responseListener.error(ErrorType.valueOf(errorType.name()));
			}
		});
	}

	private static class StringToJsonResponseListener implements StringResponseListener{
		private ResponseListener externalResponseListener;
		StringToJsonResponseListener(ResponseListener externalResponseListener){
			this.externalResponseListener = externalResponseListener;
		}

		@Override
		public void success(int responseCode, String response) {
			try{
				externalResponseListener.success(responseCode, new JSONObject(response));
			}catch(JSONException e){
				externalResponseListener.error(ErrorType.PARSE);
			}
		}

		@Override
		public void error(ErrorType errorType) {
			externalResponseListener.error(errorType);
		}
	}

	public void makePostRequest(String endpoint, JSONObject data, ResponseListener responseListener){
		makePostRequest(endpoint, new TreeMap<String, String>(), data, responseListener);
	}

	public void makePostRequest(String endpoint, Map<String, String> headers, JSONObject data, ResponseListener responseListener){
		headers.put("Content-Type", "application/json");
		makeRequest("POST", endpoint, headers, data.toString(), new StringToJsonResponseListener(responseListener));
	}

	public void makeGetRequest(String endpoint, Map<String, String> headers, ResponseListener responseListener){
		makeRequest("GET", endpoint, headers, null, new StringToJsonResponseListener(responseListener));
	}

	public void makeGetRequest(String endpoint, ResponseListener responseListener){
		makeRequest("GET", endpoint, new TreeMap<String, String>(), null, new StringToJsonResponseListener(responseListener));
	}

	public void makeGetRequestArray(String endpoint, final ArrayResponseListener responseListener){
		makeRequest("GET", endpoint, new TreeMap<String, String>(), null, new StringResponseListener() {
			@Override
			public void success(int responseCode, String response) {
				try{
					responseListener.success(responseCode, new JSONArray(response));
				}catch(JSONException e){
					responseListener.error(ErrorType.PARSE);
				}
			}

			@Override
			public void error(ErrorType errorType) {
				responseListener.error(errorType);
			}
		});
	}
}
