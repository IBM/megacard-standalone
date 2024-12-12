package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.ModelInputs;

public class RandomEnsembleAdapter extends EnsembleAdapter {
	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		if(totalCount++ % 100000 == 0) {
			System.out.println("ENSEMBLE: Total: " + totalCount + ", Model2: " + model2Count + ", Ratio: " + ((double) model2Count / totalCount) );
		}

		float res = model1.checkFraudProbability(modelInputs);
		if( Math.random() > uncertainInterval) {
			return res > 0.5;
		}
		model2Count++;
		//System.out.println("Uncertain... using Model 2");
		return model2.checkFraud(modelInputs);
	}
}
