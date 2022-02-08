package com.ibm.lozperf.mb.modeladapter;

import java.util.HashMap;
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

	
	private class BufferStruct{
		
		final long[] shape;
		final FloatDataBuffer preprocessedDB;
		final float[] preprocessedArr;
		
		public BufferStruct(int bs, int size) {
			final int nTS = numberTimesteps();
			preprocessedDB = DataBuffers.ofFloats(size);
			preprocessedArr = new float[size];
			shape = new long[]{ nTS, bs, preprocessedArr.length / (nTS * bs) };

		}
		
	}
	
	ThreadLocal<HashMap<Integer,BufferStruct>> threadBuffers = ThreadLocal.withInitial(() -> new HashMap<Integer,BufferStruct>());
	
	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
		final int nTS = numberTimesteps();
		Map<String, Tensor> map = tensorfyInputs(batch);
		map = smb.call(map);
		TFloat32 preprocessedTF32 = (TFloat32) map.get("tf.compat.v1.transpose");
		
		BufferStruct buffers = threadBuffers.get().computeIfAbsent(batch.size(), bs -> new BufferStruct(bs, (int) preprocessedTF32.size()));
		
		preprocessedTF32.read(buffers.preprocessedDB);
		buffers.preprocessedDB.read(buffers.preprocessedArr);
		OMTensor tctensor = new OMTensor(buffers.preprocessedArr, buffers.shape);
		OMTensorList tensorList = new OMTensorList(new OMTensor[] { tctensor });
		tensorList = OMModel.mainGraph(tensorList);
		float prediction[] = tensorList.getOmtByIndex(0).getFloatData();
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = prediction[(nTS - 1) * batch.size() + i] > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
}
