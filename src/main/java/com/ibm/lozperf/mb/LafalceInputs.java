package com.ibm.lozperf.mb;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LafalceInputs {
	public static final int TIMESTEPS = 7;

	private static final Marshaller marshaller = createMarshaller();

	@XmlElement
	public Inputs inputs = new Inputs();

	@XmlRootElement
	public static class Inputs {

		@XmlElement
		public BigDecimal[] Amount = new BigDecimal[TIMESTEPS];
		@XmlElement
		public String[] UseChip = new String[TIMESTEPS];
		@XmlElement
		public String[] MerchantName = new String[TIMESTEPS];
		@XmlElement
		public String[] MerchantCity = new String[TIMESTEPS];
		@XmlElement
		public String[] MerchantState = new String[TIMESTEPS];
		@XmlElement
		public String[] Zip = new String[TIMESTEPS];
		@XmlElement
		public String[] MCC = new String[TIMESTEPS];
		@XmlElement
		public String[] Errors = new String[TIMESTEPS];
		@XmlElement
		public long[] YearMonthDayTime = new long[TIMESTEPS];
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		try {
			marshaller.marshal(this, sw);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	private static Marshaller createMarshaller() {

		Marshaller jaxbMarshaller = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(LafalceInputs.class);
			jaxbMarshaller = jaxbContext.createMarshaller();

			// To format JSON
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jaxbMarshaller;
	}
}
