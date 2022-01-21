package com.ibm.lozperf.mb;

public interface ModelAdapter extends AutoCloseable{
	public boolean checkFraud(Inputs inputs);
	public int numberTimesteps();
}
