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

	public static float[] callDLC(TFloat32 preprocessed, int nTS, int batchsize) {
		FloatDataBuffer preprocessedDB = DataBuffers.ofFloats(preprocessed.size());
		preprocessed.read(preprocessedDB);
		preprocessed = null;
		float[] preprocessedArr = new float[(int) preprocessedDB.size()];
		preprocessedDB.read(preprocessedArr);
		preprocessedDB = null;
		long[] shape = { nTS, batchsize, preprocessedArr.length / (nTS * batchsize) };
		OMTensor tctensor = new OMTensor(preprocessedArr, shape);
		preprocessedArr = null;
		OMTensorList tensorList = new OMTensorList(new OMTensor[] { tctensor });
		tensorList = OMModel.mainGraph(tensorList);
		return tensorList.getOmtByIndex(0).getFloatData();
	}
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		final int nTS = numberTimesteps();
		Map<String, Tensor> map = tensorfyBatchInputs(batch);
		map = smb.call(map);
		TFloat32 preprocessedTF32 = (TFloat32) map.get("tf.compat.v1.transpose");
		map = null;
		float[] prediction = callDLC(preprocessedTF32, nTS, batch.size());
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = prediction[(nTS - 1) * batch.size() + i] > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
}