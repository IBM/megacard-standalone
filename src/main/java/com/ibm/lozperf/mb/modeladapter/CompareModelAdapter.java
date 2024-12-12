package com.ibm.lozperf.mb.modeladapter;

import java.util.Arrays;

import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;

public class CompareModelAdapter implements ModelAdapter {

	ModelAdapter[] modelAdapters = { new DLCModelBatchingAdapter(), new DLCModelAdapter() };

	public CompareModelAdapter() {
	}

	@Override
	public void close() throws Exception {
		for (ModelAdapter ma : modelAdapters)
			ma.close();
	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		boolean result = modelAdapters[0].checkFraud(modelInputs);
		for (int i = 1; i < modelAdapters.length; i++) {
			boolean r = modelAdapters[i].checkFraud(modelInputs);
			if (r != result) {
				ServingInputWrapper tfInputs = new ServingInputWrapper(modelInputs);
				System.out.println(
						"Model Missmatch " + result + " != " + r + " for inputs:\n" + tfInputs.toString() + "\n" + 
								Arrays.toString(modelInputs.MerchantName[0]));
			}
		}
		return result;
	}

}
