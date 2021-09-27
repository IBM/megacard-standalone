package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CreditCardTransaction {
	
	@XmlElement
	public String cardNumber;
	@XmlElement
	public String cvv;
	@XmlElement
	public String expirationDate;
	@XmlElement
	public int merchantAcc;
	@XmlElement
	public String merchantToken;
	@XmlElement
	public String transactionUuid;
	@XmlElement
	public BigDecimal amount;
}