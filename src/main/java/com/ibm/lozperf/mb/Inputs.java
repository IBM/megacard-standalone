package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Inputs {

	public final int timeSteps;
	
	@XmlElement
	public BigDecimal[] Amount;
	@XmlElement
	public String[] UseChip;
	@XmlElement
	public String[] MerchantName;
	@XmlElement
	public String[] MerchantCity;
	@XmlElement
	public String[] MerchantState;
	@XmlElement
	public String[] Zip;
	@XmlElement
	public String[] MCC;
	@XmlElement
	public String[] Errors;
	@XmlElement
	public long[] YearMonthDayTime;
	
	public Inputs(int timeSteps) {
		this.timeSteps = timeSteps;
		Amount = new BigDecimal[timeSteps];
		UseChip = new String[timeSteps];
		MerchantName = new String[timeSteps];
		MerchantCity = new String[timeSteps];
		MerchantState = new String[timeSteps];
		Zip = new String[timeSteps];
		MCC = new String[timeSteps];
		Errors = new String[timeSteps];
		YearMonthDayTime = new long[timeSteps];
	}
}