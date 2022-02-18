package com.ibm.lozperf.mb.modeladapter;

import java.util.List;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.batching.BatchCollector;
import com.ibm.lozperf.mb.batching.Job;

public abstract class AbstractBatchingAdapter implements ModelAdapter {

	private BatchCollector<Inputs> batchCollector = new BatchCollector<>((batch)-> batchPredict(batch));

	protected abstract void batchPredict(List<Job<Inputs>> batch);

	public AbstractBatchingAdapter() {
		super();
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		return batchCollector.predict(inputs);
	}

	@Override
	public void close() throws Exception {
		batchCollector.close();
	}

}