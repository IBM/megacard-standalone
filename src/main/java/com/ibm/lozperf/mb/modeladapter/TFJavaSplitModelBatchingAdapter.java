package com.ibm.lozperf.mb.modeladapter;

import java.util.List;
import java.util.Map;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.SavedModelBundle.Loader;
import org.tensorflow.types.TFloat32;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class TFJavaSplitModelBatchingAdapter extends TFJavaBatchingAdapter {
	private final static String TF_MODEL2 = System.getenv("TF_MODEL2");
	
	protected SavedModelBundle smb2;

	public TFJavaSplitModelBatchingAdapter() {
		Loader loader = SavedModelBundle.loader(TF_MODEL2);
		smb2 = loader.load();
	}
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		Map<String, Tensor> inputMap = tensorfyInputs(batch);
		Map<String, Tensor> output = smb.call(inputMap);
		inputMap.clear();
		inputMap.put("lstm_2_input", output.get("tf.compat.v1.transpose"));
		output = smb2.call(inputMap);
		TFloat32 tf32 = (TFloat32) output.get("dense_1");
		setResult(batch, tf32);
	}
}
