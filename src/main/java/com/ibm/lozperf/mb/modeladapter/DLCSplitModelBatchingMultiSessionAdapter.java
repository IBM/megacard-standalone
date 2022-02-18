package com.ibm.lozperf.mb.modeladapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.SavedModelBundle.Loader;
import org.tensorflow.types.TFloat32;
import org.tensorflow.Tensor;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class DLCSplitModelBatchingMultiSessionAdapter extends AbstractBatchingAdapter {
	
	public final static String TF_MODEL = System.getenv("TF_MODEL");
	Loader loader = SavedModelBundle.loader(TF_MODEL);
	
	private Collection<SavedModelBundle> smbs = new ArrayList<>();
	ThreadLocal<SavedModelBundle> threadSmb = new ThreadLocal<SavedModelBundle>() {
		@Override
		protected SavedModelBundle initialValue() {		
			SavedModelBundle smb = loader.load();
			smbs.add(smb);
			return smb;
		}
	};
	
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
	
	@Override
	public void close() throws Exception {
		smbs.forEach(SavedModelBundle::close);
		super.close();
	}
}
