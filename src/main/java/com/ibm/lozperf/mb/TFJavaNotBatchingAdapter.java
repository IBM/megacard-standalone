package com.ibm.lozperf.mb;

import java.util.Arrays;

public class TFJavaNotBatchingAdapter extends TFJavaBatchingAdapter {
	
	@Override
	public boolean checkFraud(Inputs inputs) {
		Job<Inputs> job = new Job<>(inputs);
		batchPredict(Arrays.asList(job));
		return job.getResult();
	}
}
