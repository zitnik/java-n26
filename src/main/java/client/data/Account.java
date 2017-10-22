/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.data;

public class Account {
	private String id, iban, bic, bankName;
	private double availableBalance;

	public Account(String id, String iban, String bic, String bankName, double availableBalance) {
		this.id = id;
		this.iban = iban;
		this.bic = bic;
		this.bankName = bankName;
		this.availableBalance = availableBalance;
	}

	public String getId() {
		return id;
	}

	public String getIban() {
		return iban;
	}

	public String getBic() {
		return bic;
	}

	public String getBankName() {
		return bankName;
	}

	public double getAvailableBalance() {
		return availableBalance;
	}
}
