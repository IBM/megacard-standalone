package com.ibm.lozperf.mb.modeladapter;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.ibm.lozperf.mb.Inputs;

@XmlRootElement
public class ServingInputWrapper {

	@XmlElement
	public Inputs inputs;
	
	public ServingInputWrapper() {
		
	}
	
	public ServingInputWrapper(Inputs inputs) {
		this.inputs = inputs;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		try {
			Marshaller marshaller = createMarshaller();
			marshaller.marshal(this, sw);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	private static Marshaller createMarshaller() {

		Marshaller jaxbMarshaller = null;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ServingInputWrapper.class);
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
