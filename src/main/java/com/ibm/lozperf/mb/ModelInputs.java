package com.ibm.lozperf.mb;

import java.math.BigDecimal;

public class ModelInputs {

	public int timeSteps;
	
	public BigDecimal[][] Amount;
	public long[][] UseChip;
	public String[][] MerchantName;
	public String[][] MerchantCity;
	public String[][] MerchantState;
	public String[][] Zip;
	public String[][] MCC;
	public String[][] Errors;
	public int[][] Minute;
	public int[][] Hour;
	public int[][] DayofWeek;
	public int[][] Day;
	public int[][] Month;
	public long[][] TimeDelta;
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
		DayofWeek = new int[1][timeSteps];
		Day = new int[1][timeSteps];
		Month = new int[1][timeSteps];
		TimeDelta = new long[1][timeSteps];
		YearDiff = new int[1][timeSteps];
	}
}