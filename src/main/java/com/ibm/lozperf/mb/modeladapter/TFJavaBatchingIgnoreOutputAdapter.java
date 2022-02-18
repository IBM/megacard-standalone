package com.ibm.lozperf.mb.modeladapter;

import java.util.List;
import java.util.Map;

import org.tensorflow.Tensor;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class TFJavaBatchingIgnoreOutputAdapter extends TFJavaBatchingAdapter {
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		Map<String, Tensor> inputMap = tensorfyBatchInputs(batch);
		Map<String, Tensor> output = smb.call(inputMap);
		for (int i = 0; i < batch.size(); i++) {
			batch.get(i).setResult(false);
		}
	}
}
