/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client;

import client.data.*;
import client.network.AuthConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class N26Client {
	private AuthConnection authConnection;

	public N26Client(AuthConnection authConnection) {
		this.authConnection = authConnection;
	}

	public interface ResponseListener{
		void error(AuthConnection.ErrorType errorType);
	}

	private static abstract class InternalResponseListener implements AuthConnection.ResponseListener{
		ResponseListener externalResponseListener;

		InternalResponseListener(ResponseListener responseListener){
			this.externalResponseListener = responseListener;
		}

		@Override
		public void error(AuthConnection.ErrorType errorType) {
			externalResponseListener.error(errorType);
		}
	}

	public void getUserDetails(final UserResponseListener responseListener){
		authConnection.makeGetRequest("/api/me", new InternalResponseListener(responseListener) {
			@Override
			public void success(int responseCode, JSONObject response) {
				responseListener.success(new User(
						response.getString("id"),
						response.getString("email"),
						response.getString("firstName"),
						response.getString("lastName")
				));
			}
		});
	}
	public interface UserResponseListener extends ResponseListener{
		void success(User user);
	}

	public void getAccountDetails(final AccountResponseListener responseListener){
		authConnection.makeGetRequest("/api/accounts", new InternalResponseListener(responseListener) {
			@Override
			public void success(int responseCode, JSONObject response) {
				responseListener.success(new Account(
						response.getString("id"),
						response.getString("iban"),
						response.getString("bic"),
						response.getString("bankName"),
						response.getDouble("availableBalance")
				));
			}
		});
	}
	public interface AccountResponseListener extends ResponseListener{
		void success(Account account);
	}

	public void getContacts(final ContactsResponseListener responseListener){
		authConnection.makeGetRequestArray("/api/smrt/contacts", new AuthConnection.ArrayResponseListener() {
			@Override
			public void success(int responseCode, JSONArray response) {
				LinkedList<Contact> contacts = new LinkedList<>();
				Iterator iter = response.iterator();
				while(iter.hasNext()){
					JSONObject jsonContact = (JSONObject) iter.next();
					String userId = jsonContact.getString("userId");
					String id = jsonContact.getString("id");
					String name = jsonContact.getString("name");
					String subtitle = jsonContact.getString("subtitle");
					JSONObject jsonAccount = jsonContact.getJSONObject("account");
					String accountType = jsonAccount.getString("accountType");
					String iban = jsonAccount.getString("iban");
					String bic = jsonAccount.getString("bic");
					contacts.add(new Contact(userId, id, name, subtitle, accountType, iban, bic));
				}
				responseListener.success(contacts);
			}

			@Override
			public void error(AuthConnection.ErrorType errorType) {
				responseListener.error(errorType);
			}
		});
	}
	public interface ContactsResponseListener extends ResponseListener{
		void success(List<Contact> contactList);
	}

	public void checkIban(String iban, final CheckIbanResponseListener responseListener){
		JSONObject ibanPost = new JSONObject();
		ibanPost.put("IBAN", iban);
		authConnection.makePostRequest("/api/actions/iban", ibanPost, new InternalResponseListener(responseListener) {
			@Override
			public void success(int responseCode, JSONObject response) {
				responseListener.success(new IbanCheck(
						response.getBoolean("isIBANValid"),
						response.getString("IBAN"),
						response.getString("bic"),
						response.getString("bankName")
				));
			}
		});
	}
	public interface CheckIbanResponseListener extends ResponseListener{
		void success(IbanCheck ibanCheck);
	}

	public void postTransfer(TransferRequest transferRequest, final TransferResponseListener responseListener){
		JSONObject transferPost = new JSONObject();
		transferPost.put("pin", transferRequest.getTransferCode());
		JSONObject transaction = new JSONObject();
		transaction.put("partnerBic", transferRequest.getBic());
		transaction.put("amount", transferRequest.getAmount());
		transaction.put("type", "DT");
		transaction.put("partnerIban", transferRequest.getIban());
		transaction.put("partnerName", transferRequest.getRecipientName());
		transaction.put("referenceText", transferRequest.getReference());
		transferPost.put("transaction", transaction);

		authConnection.makePostRequest("/api/transactions", transferPost, new InternalResponseListener(responseListener) {
			@Override
			public void success(int responseCode, JSONObject response) {
				if(responseCode==200)
					responseListener.success();
				else
					responseListener.error(AuthConnection.ErrorType.OTHER);
			}
		});
	}
	public interface TransferResponseListener extends ResponseListener{
		void success();
	}
}
