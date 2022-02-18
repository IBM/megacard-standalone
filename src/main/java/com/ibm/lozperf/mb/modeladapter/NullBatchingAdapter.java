package com.ibm.lozperf.mb.modeladapter;

import java.util.List;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class NullBatchingAdapter extends AbstractBatchingAdapter{

	private final static long NULL_SLEEP = Integer.parseInt(System.getenv("NULL_SLEEP"));
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		try {
			Thread.sleep(NULL_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < batch.size(); i++) {
			batch.get(i).setResult(false);
		}
	}

}
