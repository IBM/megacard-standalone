package com.ibm.lozperf.mb;
public enum CreditcardTransactionType{
	ONLINE("Online Transaction"),
	STRIP("Swipe Transaction"),
	CHIP("Chip Transaction");
	
	public final String stringValue;
	
	private CreditcardTransactionType(String stringValue) {
		this.stringValue = stringValue;
	}
	
	@Override
	public String toString() {
		return stringValue;
	}
}