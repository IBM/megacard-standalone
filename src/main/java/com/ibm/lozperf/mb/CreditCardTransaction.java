package com.ibm.lozperf.mb;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CreditCardTransaction {
	
	@XmlElement(required=true)
	public String cardNumber;
	@XmlElement(required=true)
	public String cvv;
	@XmlElement(required=true)
	public String expirationDate;
	@XmlElement(required=true)
	public int merchantAcc;
	@XmlElement(required=true)
	public String merchantToken;
	@XmlElement(required=true)
	public String transactionUuid;
	@XmlElement(required=true)
	public BigDecimal amount;
	@XmlElement(required=true)
	public String method;
	@XmlElement
	public long timestamp;
}