package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class NullModelAdapter implements ModelAdapter {

	@Override
	public void close() throws Exception {
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		return false;
	}

}
