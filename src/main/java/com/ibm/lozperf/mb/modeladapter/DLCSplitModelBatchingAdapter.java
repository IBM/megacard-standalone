package com.ibm.lozperf.mb.modeladapter;

import java.util.List;
import java.util.Map;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.types.TFloat32;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

public class DLCSplitModelBatchingAdapter extends TFJavaBatchingAdapter {

	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		final int nTS = numberTimesteps();
		Map<String, Tensor> map = tensorfyInputs(batch);
		map = smb.call(map);
		TFloat32 preprocessedTF32 = (TFloat32) map.get("tf.compat.v1.transpose");
		FloatDataBuffer preprocessedDB = DataBuffers.ofFloats(preprocessedTF32.size());
		preprocessedTF32.read(preprocessedDB);
		float[] preproccedArr = new float[(int) preprocessedDB.size()];
		preprocessedDB.read(preproccedArr);
		long[] shape = { nTS, batch.size(), preprocessedDB.size() / (nTS * batch.size()) };
		OMTensor tctensor = new OMTensor(preproccedArr, shape);
		OMTensorList tensorListIn = new OMTensorList(new OMTensor[] { tctensor });
		OMTensorList tensorListOut = OMModel.mainGraph(tensorListIn);
		OMTensor output = tensorListOut.getOmtByIndex(0);
		float prediction[] = output.getFloatData();
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = prediction[(nTS - 1)*batch.size() + i] > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
}
