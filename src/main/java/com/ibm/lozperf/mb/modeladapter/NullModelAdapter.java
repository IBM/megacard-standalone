package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class NullModelAdapter implements ModelAdapter {
	
	@Override
	public void close() throws Exception {
	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		return false;
	}

}
