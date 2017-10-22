/*
 * Copyright © 2017 Anze Zitnik
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package paymentreferencecheck;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SIReferenceChecker {
	/**
	 * Checks validity of SI (Slovenian standard) payment references.
	 *
	 * @return true if paymentreferencecheck is valid,
	 * false if paymentreferencecheck is invalid,
	 * null if validity could not be verified (unknown format)
	 */
	public static Boolean checkReference(String reference){
		if(reference==null || reference.length()<2)
			return null;
		reference = reference.replaceAll("[ \\.-]+","").toUpperCase();
		if(reference.substring(0,2).equals("SI")){
			int model;
			try{
				model = Integer.valueOf(reference.substring(2,4));
			}catch (NumberFormatException e){
				return null;
			}
			String testdata = reference.substring(4);
			switch (model){
				case 0:
					return new SIReferenceControl("p-p-p", 1, 3).check(testdata);
				case 1:
					return new SIReferenceControl("(p-p-p)k", 1, 3).check(testdata);
				case 2:
					return new SIReferenceControl("p-(p)k-(p)k", 3, 3).check(testdata);
				case 3:
					return new SIReferenceControl("(p)k-(p)k-(p)k", 3, 3).check(testdata);
				case 4:
					return new SIReferenceControl("(p)k-p-(p)k", 3, 3).check(testdata);
				case 5:
					return new SIReferenceControl("(p)k-p-p", 1, 3).check(testdata);
				case 6:
					return new SIReferenceControl("p-(p-p)k", 2, 3).check(testdata);
				case 7:
					return new SIReferenceControl("p-(p)k-p", 2, 3).check(testdata);
				case 8:
					return new SIReferenceControl("(p-p)k-(p)k", 3, 3).check(testdata);
				case 9:
					return new SIReferenceControl("(p-p)k-p", 1, 3).check(testdata);
				case 10:
					return new SIReferenceControl("(p)k-(p-p)k", 2, 3).check(testdata);
				case 11:
				case 18:
				case 19:
				case 28:
				case 38:
				case 40:
				case 41:
				case 48:
				case 49:
				case 51:
				case 58:
					return new SIReferenceControl("(p)k-(p)k-p", 2, 3).check(testdata);
				case 12:
					return new SIReferenceControl("(p)k", 1, 1).check(testdata);
				case 21:
				case 31:
					return new SIReferenceControl("(p)k-p", 1, 3).check(testdata);
				case 55:
					return new SIReferenceControl("(p)k-p-p", 1, 3).check(testdata);
				case 99:
					return new SIReferenceControl("", 0, 0).check(testdata);
			}
		}

		return null;
	}

	private static class SIReferenceControl{
		String definition;
		int min, max;

		SIReferenceControl(String definition, int min, int max) {
			this.definition = definition;
			this.min = min;
			this.max = max;
		}

		boolean check(String data){
			//check general constraints
			if(max==0)
				return data.isEmpty();
			if(data.length()>22 || data.length()<1)
				return false;
			String test;
			if(max==1)
				test = "[0-9]{1,20}-";
			else
				test = String.format("([0-9]{1,20}-){%d,%d}", min, max);
			if(!(data+"-").matches(test))
				return false;
			if(data.replaceAll("-", "").length() > 20)
				return false;

			//check control numbers
			String[] c_fields = definition.split("\\)k");
			int position = 0;
			for(String c_field : c_fields){
				if(!c_field.startsWith("("))
					continue;
				int p_count = count(c_field, "p");
				Pattern pattern = Pattern.compile(String.format("([0-9]{1,20}-){%d}[0-9]{1,20}", p_count-1));
				Matcher matcher = pattern.matcher(data);
				boolean b = matcher.find(position);
				if(!b)
					return false;
				if(!checkControlNum(data.substring(position, matcher.end()).replaceAll("-", "")))
					return false;
				position = matcher.end();
			}
			return true;
		}
	}

	static boolean checkControlNum(String data){
		return controlNum(data.substring(0, data.length()-1)) == Integer.valueOf(""+data.charAt(data.length()-1));
	}

	static int controlNum(String data){
		int r = 0;
		int p = 2;
		data = new StringBuilder(data).reverse().toString();
		for(int index=0; index<data.length(); index++) {
			int i = Integer.valueOf(""+data.charAt(index));
			r += i*p;
			p++;
		}
		int x = 11 - (r % 11);
		if(x>9)
			return 0;
		return x;
	}

	static int count(String data, String match){
		if(match.isEmpty()) return 0;
		int r=0;
		for(int i=0; i<data.length()-match.length()+1; i++){
			if(data.substring(i).startsWith(match))
				r++;
		}
		return r;
	}
}
