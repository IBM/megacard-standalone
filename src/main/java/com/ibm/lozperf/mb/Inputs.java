package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Inputs {

	public int timeSteps;
	
	@XmlElement
	public BigDecimal[][] Amount;
	@XmlElement
	public CreditcardTransactionType[][] UseChip;
	@XmlElement
	public String[][] MerchantName;
	@XmlElement
	public String[][] MerchantCity;
	@XmlElement
	public String[][] MerchantState;
	@XmlElement
	public String[][] Zip;
	@XmlElement
	public String[][] MCC;
	@XmlElement
	public String[][] Errors;
	@XmlElement
	public long[][] YearMonthDayTime;
	
	public Inputs() {
		
	}
	
	public Inputs(int timeSteps) {
		this.timeSteps = timeSteps;
		Amount = new BigDecimal[1][timeSteps];
		UseChip = new CreditcardTransactionType[1][timeSteps];
		MerchantName = new String[1][timeSteps];
		MerchantCity = new String[1][timeSteps];
		MerchantState = new String[1][timeSteps];
		Zip = new String[1][timeSteps];
		MCC = new String[1][timeSteps];
		Errors = new String[1][timeSteps];
		YearMonthDayTime = new long[1][timeSteps];
	}
}