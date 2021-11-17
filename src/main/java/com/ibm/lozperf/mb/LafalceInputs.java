package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LafalceInputs {
	public static final int TIMESTEPS=7;
	
	@XmlElement public Inputs inputs = new Inputs();
	
	
	@XmlRootElement
	public class Inputs{
		
		@XmlElement public BigDecimal[] Amount = new BigDecimal[TIMESTEPS];
		@XmlElement public String[] UseChip = new String[TIMESTEPS];
		@XmlElement public String[] MerchantName = new String[TIMESTEPS];
		@XmlElement public String[] MerchantCity = new String[TIMESTEPS];
		@XmlElement public String[] MerchantState = new String[TIMESTEPS];
		@XmlElement public String[] Zip = new String[TIMESTEPS];
		@XmlElement public String[] MCC = new String[TIMESTEPS];
		@XmlElement public String[] Errors = new String[TIMESTEPS];
		@XmlElement public long[] YearMonthDayTime = new long[TIMESTEPS];
	}
}
