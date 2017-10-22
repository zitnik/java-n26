/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package client.data;

public class IbanCheck {
	private boolean ibanValid;
	private String iban, bic, bankName;

	public IbanCheck(boolean ibanValid, String iban, String bic, String bankName) {
		this.ibanValid = ibanValid;
		this.iban = iban;
		this.bic = bic;
		this.bankName = bankName;
	}

	public boolean isIbanValid() {
		return ibanValid;
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
}
