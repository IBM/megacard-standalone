package com.ibm.lozperf.mb.modeladapter;

import java.util.List;
import java.util.Map;

import org.tensorflow.types.TFloat32;
import org.tensorflow.Tensor;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class DLCSplitModelBatchingMultiSessionAdapter extends TFJavaBatchingMultiSessionIgnoreOutputAdapter {
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		Map<String, Tensor> map = TFJavaBatchingAdapter.tensorfyBatchInputs(batch);
		final int nTS = numberTimesteps();
		map = threadSmb.get().call(map);
		TFloat32 preprocessedTF32 = (TFloat32) map.get("tf.compat.v1.transpose");
		map = null;
		float[] prediction = DLCSplitModelBatchingAdapter.callDLC(preprocessedTF32, nTS, batch.size());
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = prediction[(nTS - 1) * batch.size() + i] > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
}
