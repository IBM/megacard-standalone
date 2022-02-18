package com.ibm.lozperf.mb.modeladapter;

import java.util.Map;

import org.tensorflow.Tensor;

import com.ibm.lozperf.mb.Inputs;

public class TFJavaIgnoreOuputAdapter extends TFJavaAdapter {
	
	@Override
	public boolean checkFraud(Inputs inputs) {
		Map<String,Tensor> map = tensorfyInputs(inputs);
		map = smb.call(map);
		return false;
	}
}
