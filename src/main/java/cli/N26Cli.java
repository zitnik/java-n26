/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package cli;

import client.N26Client;
import client.data.*;
import client.network.AuthConnection;
import paymentreferencecheck.SIReferenceChecker;

import java.util.List;
import java.util.Scanner;

public class N26Cli {

	Scanner scanner;
	private N26Client client;

	public N26Cli(){
		scanner = new Scanner(System.in);
	}

	private void printError(AuthConnection.ErrorType errorType){
		printError(errorType.toString());
	}

	private void printError(String error){
		System.err.println(String.format("Error: %s", error));
		mainMenu();
	}

	private String readLine(){
		return scanner.nextLine();
	}

	private String readLine(String prompt, Object ... args){
		System.out.println(String.format(prompt, args));
		return readLine();
	}

	private String readPassword(String prompt, Object ... args){
		return new String(System.console().readPassword(prompt, args));
	}

	public void run(){
		loginMenu();
	}

	void loginMenu(){
		String username = readLine("Username?");
		String password = readPassword("Password?");
		System.out.println("connecting...");

		final AuthConnection authConnection = new AuthConnection();
		authConnection.login(username, password, new AuthConnection.LoginResponseListener() {
			@Override
			public void success() {
				client = new N26Client(authConnection);
				client.getUserDetails(new N26Client.UserResponseListener() {
					@Override
					public void success(User user) {
						System.out.println(String.format("Welcome, %s %s",
								user.getFirstName(), user.getLastName()));
						mainMenu();
					}

					@Override
					public void error(AuthConnection.ErrorType errorType) {
						System.err.println(String.format("Error: %s", errorType));
						loginMenu();
					}
				});
			}

			@Override
			public void error(AuthConnection.ErrorType errorType) {
				System.err.println(String.format("Error: %s", errorType));
				loginMenu();
			}
		});
	}

	void mainMenu(){
		int selection = Integer.valueOf(readLine("Available actions:\n" +
				"1: account info\n" +
				"2: make transfer\n" +
				"3: exit"));

		switch (selection){
			case 1:
				accountInfo();
				break;
			case 2:
				makeTransferMenu();
				break;
			case 3:
				System.exit(0);
			default:
				System.out.println("Invalid selection");
				mainMenu();
		}
	}

	void accountInfo(){
		client.getAccountDetails(new N26Client.AccountResponseListener() {
			@Override
			public void success(Account account) {
				System.out.format("%s / %s\nIBAN: %s\nBalance: %.2f EUR\n\n",
						account.getBankName(),
						account.getBic(),
						account.getIban(),
						account.getAvailableBalance());
				mainMenu();
			}

			@Override
			public void error(AuthConnection.ErrorType errorType) {
				printError(errorType);
			}
		});
	}

	void makeTransferMenu(){
		int selection = Integer.valueOf(readLine("1: Input recipient details\n" +
				"2: Choose recipient among contacts"));
		switch (selection){
			case 1:
				String name = readLine("Recipient name? (max 70 chars)");
				if(name.length() > 70)
					name = name.substring(0,70);

				String iban;
				do{
					iban = readLine("IBAN?");
					iban = iban.replaceAll("[ \\.-]+","");
				}while(!iban.matches("[a-zA-Z]{2}[0-9]{17}"));

				checkIbanAndTransfer(name, iban);
				break;
			case 2:
				client.getContacts(new N26Client.ContactsResponseListener() {
					@Override
					public void success(List<Contact> contactList) {
						StringBuilder prompt = new StringBuilder();
						prompt.append("Select contact:\n\n");
						int i=0;
						for(Contact contact : contactList){
							i++;
							prompt.append(i);
							prompt.append(".");
							prompt.append("\t");
							prompt.append(contact.getName());
							prompt.append("\n\t");
							prompt.append(contact.getSubtitle());
							prompt.append("\n");
						}
						System.out.println(prompt.toString());
						Contact contact = null;
						do{
							try {
								int selection = Integer.valueOf(readLine());
								contact = contactList.get(selection-1);
							}catch(NumberFormatException e){
								System.out.println("Invalid number format");
							}catch(IndexOutOfBoundsException e){
								System.out.println("Invalid selection");
							}
						}while(contact==null);

						checkIbanAndTransfer(contact.getName(), contact.getIban());
					}

					@Override
					public void error(AuthConnection.ErrorType errorType) {
						printError(errorType);
					}
				});
				break;
			default:
				System.out.println("Invalid selection");
				mainMenu();
		}
	}

	private void checkIbanAndTransfer(final String name, final String iban){
		System.out.println("Checking IBAN...");
		client.checkIban(iban, new N26Client.CheckIbanResponseListener() {
			@Override
			public void success(IbanCheck ibanCheck) {
				if(!ibanCheck.isIbanValid()){
					System.out.println("IBAN invalid!");
					makeTransferMenu();
					return;
				}
				System.out.format("IBAN OK. Bank info: %s / %s\n", ibanCheck.getBankName(), ibanCheck.getBic());
				transfer(name, iban, ibanCheck.getBic());
			}

			@Override
			public void error(AuthConnection.ErrorType errorType) {
				printError(errorType);
			}
		});
	}

	private void transfer(String name, String iban, String bic){
		Double amount = null;
		do{
			try{
				amount = Double.valueOf(readLine("Amount? (EUR)"));
			}catch(NumberFormatException e){
				System.out.println("Invalid number format");
			}
		}while(amount==null || amount<=0);

		String reference;
		do{
			reference = readLine("Reference? (max 135 chars)");
			if(reference.length() > 135)
				reference = reference.substring(0,135);
			Boolean check = SIReferenceChecker.checkReference(reference);
			if(check==null)
				System.out.println("Reference checksum not verified!");
			else if(check) {
				System.out.println("Reference checksum OK.");
				reference = reference.replaceAll(" \\.-","").toUpperCase();
			}
			else{
				System.out.println("Reference checksum invalid!");
				reference=null;
			}
		}while(reference==null);

		String c = readLine("Transfer %f EUR to\n%s\nIBAN: %s\nReference: %s\nConfirm [y/n]?",
				amount, name, iban, reference);
		if(!c.equalsIgnoreCase("y")) {
			System.out.println("Transfer cancelled");
			mainMenu();
		}

		String transferCode = readPassword("Transfer code?\n");
		TransferRequest transferRequest = new TransferRequest(
				transferCode,
				iban,
				bic,
				name,
				reference,
				amount
		);
		client.postTransfer(transferRequest, new N26Client.TransferResponseListener(){
			@Override
			public void success() {
				System.out.println("Transaction data sent successfully.\nConfirm the transaction on your phone.\n");
				mainMenu();
			}

			@Override
			public void error(AuthConnection.ErrorType errorType) {
				printError(errorType);
			}
		});
	}
}
