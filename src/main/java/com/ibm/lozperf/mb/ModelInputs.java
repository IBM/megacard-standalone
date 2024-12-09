package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ModelInputs {

	public int timeSteps;
	
	@XmlElement
	public BigDecimal[][] Amount;
	@XmlElement
	public long[][] UseChip;
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
	public int[][] Minute;
	@XmlElement
	public int[][] Hour;
	@XmlElement
	public int[][] Day;
	@XmlElement
	public int[][] Month;
	@XmlElement
	public long[][] TimeDelta;
	@XmlElement
	public int[][] YearDiff;
	
	public ModelInputs() {
		
	}
	
	public ModelInputs(int timeSteps) {
		this.timeSteps = timeSteps;
		Amount = new BigDecimal[1][timeSteps];
		UseChip = new long[1][timeSteps];
		MerchantName = new String[1][timeSteps];
		MerchantCity = new String[1][timeSteps];
		MerchantState = new String[1][timeSteps];
		Zip = new String[1][timeSteps];
		MCC = new String[1][timeSteps];
		Errors = new String[1][timeSteps];
		Minute = new int[1][timeSteps];
		Minute = new int[1][timeSteps];
		Hour = new int[1][timeSteps];
		Day = new int[1][timeSteps];
		Month = new int[1][timeSteps];
		TimeDelta = new long[1][timeSteps];
		YearDiff = new int[1][timeSteps];
	}
}