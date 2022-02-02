package com.ibm.lozperf.mb;

public interface ModelAdapter extends AutoCloseable{
	public boolean checkFraud(Inputs inputs);
	
	public default int numberTimesteps() {
		return 7;
	}
}
