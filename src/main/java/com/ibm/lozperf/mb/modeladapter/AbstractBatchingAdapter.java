package com.ibm.lozperf.mb.modeladapter;

import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.batching.BatchCollector;
import com.ibm.lozperf.mb.batching.Job;

public abstract class AbstractBatchingAdapter implements ModelAdapter, FraudProbability {

	private BatchCollector<ModelInputs> batchCollector = new BatchCollector<>((batch)-> batchPredict(batch));

	protected abstract void batchPredict(List<Job<ModelInputs>> batch);

	public AbstractBatchingAdapter() {
		super();
	}

	@Override
	public float checkFraudProbability(ModelInputs modelInputs) {
		return batchCollector.predict(modelInputs);
	}

	@Override
	public void close() throws Exception {
		batchCollector.close();
	}

}