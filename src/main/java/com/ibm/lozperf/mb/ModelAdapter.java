package com.ibm.lozperf.mb;

public interface ModelAdapter extends AutoCloseable{
	public boolean checkFraud(ModelInputs modelInputs);
	
	public default int numberTimesteps() {
		return 7;
	}
}
