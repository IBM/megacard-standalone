package com.ibm.lozperf.mb;
public enum CreditcardTransactionType{
	ONLINE("Online Transaction"),
	STRIP("Swipe Transaction"),
	CHIP("Chip Transaction");
	
	public final String stringValue;
	
	private CreditcardTransactionType(String stringValue) {
		this.stringValue = stringValue;
	}
	
	
	private final static CreditcardTransactionType[] values = values();
			
	public static CreditcardTransactionType getType(String stringValue) {
		for(CreditcardTransactionType type: values) {
			if(type.toString().equals(stringValue))
					return type;
		}		
		throw new IllegalArgumentException();
	}
	
	@Override
	public String toString() {
		return stringValue;
	}
}