package com.ibm.lozperf.mb;

import java.math.BigDecimal;
import java.util.Arrays;

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
		Hour = new int[1][timeSteps];
		DayofWeek = new int[1][timeSteps];
		Day = new int[1][timeSteps];
		Month = new int[1][timeSteps];
		TimeDelta = new long[1][timeSteps];
		YearDiff = new int[1][timeSteps];
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ModelInputs: { ");
		sb.append("Amount: ");
		sb.append(Arrays.deepToString(Amount));
		sb.append(", MerchantName: ");
		sb.append(Arrays.deepToString(MerchantName));
		sb.append(", MerchantCity: ");
		sb.append(Arrays.deepToString(MerchantCity));
		sb.append(", MerchantState: ");
		sb.append(Arrays.deepToString(MerchantState));
		sb.append(", Zip: ");
		sb.append(Arrays.deepToString(Zip));
		sb.append(", MCC: ");
		sb.append(Arrays.deepToString(MCC));
		sb.append(", Errors: ");
		sb.append(Arrays.deepToString(Errors));
		sb.append(", Minute: ");
		sb.append(Arrays.deepToString(Minute));
		sb.append(", Hour: ");
		sb.append(Arrays.deepToString(Hour));
		sb.append(", DayofWeek: ");
		sb.append(Arrays.deepToString(DayofWeek));
		sb.append(", Day: ");
		sb.append(Arrays.deepToString(Day));
		sb.append(", Month: ");
		sb.append(Arrays.deepToString(Month));		
		sb.append(", TimeDelta: ");
		sb.append(Arrays.deepToString(TimeDelta));		
		sb.append(", YearDiff: ");
		sb.append(Arrays.deepToString(YearDiff));
		sb.append(" }");
		return sb.toString();
	}
}