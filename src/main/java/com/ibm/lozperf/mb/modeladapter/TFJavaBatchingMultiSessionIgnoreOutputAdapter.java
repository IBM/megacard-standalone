package com.ibm.lozperf.mb.modeladapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.SavedModelBundle.Loader;
import org.tensorflow.types.TFloat32;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;

public class TFJavaBatchingMultiSessionIgnoreOutputAdapter extends AbstractBatchingAdapter {

	public static final String TF_MODEL = System.getenv("TF_MODEL");
	Loader loader = SavedModelBundle.loader(TF_MODEL);
	private Collection<SavedModelBundle> smbs = new ArrayList<>();
	protected ThreadLocal<SavedModelBundle> threadSmb = new ThreadLocal<SavedModelBundle>() {
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
			for (int i = 0; i < batch.size(); i++) {
				batch.get(i).setResult(false);
			}
		}

	@Override
	public void close() throws Exception {
		smbs.forEach(SavedModelBundle::close);
		super.close();
	}

}