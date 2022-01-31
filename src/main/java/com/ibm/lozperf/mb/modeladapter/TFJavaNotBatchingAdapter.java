package com.ibm.lozperf.mb.modeladapter;

import java.util.Arrays;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class TFJavaNotBatchingAdapter extends TFJavaBatchingAdapter {
	
	@Override
	public boolean checkFraud(Inputs inputs) {
		Job<Inputs> job = new Job<>(inputs);
		batchPredict(Arrays.asList(job));
		return job.getResult();
	}
}
