package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class SleepModelAdapter implements ModelAdapter {

	private final static long NULL_SLEEP = Integer.parseInt(System.getenv("NULL_SLEEP"));
	
	@Override
	public void close() throws Exception {
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		try {
			Thread.sleep(NULL_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}

}
