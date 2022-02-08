package com.ibm.lozperf.mb.modeladapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.ndarray.buffer.DataBuffer;
import org.tensorflow.ndarray.buffer.DataBuffers;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TString;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.BatchCollector;
import com.ibm.lozperf.mb.batching.Job;

public class TFJavaBatchingAdapter extends TFJavaAdapter {

	
	
	private BatchCollector<Inputs> batchCollector = new BatchCollector<>((batch)-> batchPredict(batch));

	
	private class BufferStruct{
		
		final Shape shape;
		final TFloat32 amount;
		final DataBuffer<String> useChip;
		final DataBuffer<String> merchantName;
		final DataBuffer<String> merchantCity;
		final DataBuffer<String> merchantState;
		final DataBuffer<String> zip;
		final DataBuffer<String> mcc;
		final DataBuffer<String> errors;
		final TInt64 date;
		
		public BufferStruct(int batchsize) {
			int nTS = numberTimesteps();
			shape = Shape.of(batchsize, nTS);
			amount = TFloat32.tensorOf(shape);
			useChip = DataBuffers.ofObjects(String.class, shape.size());
			merchantName = DataBuffers.ofObjects(String.class, shape.size());
			merchantCity = DataBuffers.ofObjects(String.class, shape.size());
			merchantState = DataBuffers.ofObjects(String.class, shape.size());
			zip = DataBuffers.ofObjects(String.class, shape.size());
			mcc = DataBuffers.ofObjects(String.class, shape.size());
			errors = DataBuffers.ofObjects(String.class, shape.size());
			date = TInt64.tensorOf(shape);
		}
		
	}
	
	ThreadLocal<HashMap<Integer,BufferStruct>> threadBuffers = ThreadLocal.withInitial(() -> new HashMap<Integer,BufferStruct>());
	
	protected Map<String, Tensor> tensorfyInputs(List<Job<Inputs>> batch){
		BufferStruct buffers = threadBuffers.get().computeIfAbsent(batch.size(), (bs)->new BufferStruct(bs));
		int nTS = numberTimesteps();	
		for (int i = 0; i < batch.size(); i++) {
			Inputs inputs = batch.get(i).getInput();
			for (int j = 0; j < nTS; j++) {
				buffers.amount.setFloat(inputs.Amount[j].floatValue(), i, j);
				buffers.date.setLong(inputs.YearMonthDayTime[j], i, j);

				buffers.useChip.setObject(inputs.UseChip[j], i * nTS + j);
				buffers.merchantName.setObject(inputs.MerchantName[j], i * nTS + j);
				buffers.merchantCity.setObject(inputs.MerchantCity[j], i * nTS + j);
				buffers.merchantState.setObject(inputs.MerchantState[j], i * nTS + j);
				buffers.zip.setObject(inputs.Zip[j], i * nTS + j);
				buffers.mcc.setObject(inputs.MCC[j], i * nTS + j);
				buffers.errors.setObject(inputs.Errors[j], i * nTS + j);
			}

		}
		Map<String, Tensor> inputMap = new HashMap<>(20);
		inputMap.put("Amount", buffers.amount);
		inputMap.put("UseChip", TString.tensorOf(buffers.shape, buffers.useChip));
		inputMap.put("MerchantName", TString.tensorOf(buffers.shape, buffers.merchantName));
		inputMap.put("MerchantCity", TString.tensorOf(buffers.shape, buffers.merchantCity));
		inputMap.put("MerchantState", TString.tensorOf(buffers.shape, buffers.merchantState));
		inputMap.put("Zip", TString.tensorOf(buffers.shape, buffers.zip));
		inputMap.put("MCC", TString.tensorOf(buffers.shape, buffers.mcc));
		inputMap.put("Errors", TString.tensorOf(buffers.shape, buffers.errors));
		inputMap.put("YearMonthDayTime", buffers.date);
		
		return inputMap;
	}
	protected void setResult(List<Job<Inputs>> batch, TFloat32 result) {
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = result.getFloat(numberTimesteps() - 1, i, 0) > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
	
	protected void batchPredict(List<Job<Inputs>> batch) {
		Map<String, Tensor> inputMap = tensorfyInputs(batch);
		Map<String, Tensor> output = smb.call(inputMap);
		TFloat32 tf32 = (TFloat32) output.get("sequential_12");
		setResult(batch, tf32);
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		return batchCollector.predict(inputs);
	}

	@Override
	public void close() throws Exception {
		batchCollector.close();
		super.close();
	}
}
