/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.data;

public class Contact {
	/** {
	 * 		"userId" : "45f56011-a554-4582-aafd-db68843e5d0c",
	 * 		"id" : "67306d69-a681-44eb-982e-044537906d9d",
	 * 		"name" : "A1 Slovenija, d. d.",
	 * 		"subtitle" : "SI56 2900 0015 9800 373",
	 * 		"account" :
	 * 			{
	 * 			"accountType" : "sepa",
	 * 			"iban" : "SI56290000159800373",
	 * 			"bic" : "BACXSI22XXX"
	 * 			}
	 * 	}
	 */
	private String userId, id, name, subtitle;
	private String accountType, iban, bic;

	public Contact(String userId, String id, String name, String subtitle, String accountType, String iban, String bic) {
		this.userId = userId;
		this.id = id;
		this.name = name;
		this.subtitle = subtitle;
		this.accountType = accountType;
		this.iban = iban;
		this.bic = bic;
	}

	public String getUserId() {
		return userId;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public String getAccountType() {
		return accountType;
	}

	public String getIban() {
		return iban;
	}

	public String getBic() {
		return bic;
	}
}
