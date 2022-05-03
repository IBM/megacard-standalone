package com.ibm.lozperf.mb;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ModelOutputs {
	@XmlElement public float[][][] outputs;
}
