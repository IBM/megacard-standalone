package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;

public interface FraudProbability extends ModelAdapter {

	public float checkFraudProbability(ModelInputs modelInputs);
	
	public default boolean checkFraud(ModelInputs modelInputs) {
		return checkFraudProbability(modelInputs) > 0.5;
	}
}
