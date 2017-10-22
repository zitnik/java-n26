/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.data;

public class TransferRequest {
	private String transferCode, iban, bic, recipientName, reference;
	private double amount;

	public TransferRequest(String transferCode, String iban, String bic, String recipientName, String reference, double amount) {
		this.transferCode = transferCode;
		this.iban = iban;
		this.bic = bic;
		this.recipientName = recipientName;
		this.reference = reference;
		this.amount = amount;
	}

	public String getTransferCode() {
		return transferCode;
	}

	public String getIban() {
		return iban;
	}

	public String getBic() {
		return bic;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getReference() {
		return reference;
	}

	public double getAmount() {
		return amount;
	}
}
