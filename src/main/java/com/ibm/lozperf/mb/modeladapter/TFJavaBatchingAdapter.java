package com.ibm.lozperf.mb.modeladapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static Map<String, Tensor> tensorfyBatchInputs(List<Job<Inputs>> batch){
		int nTS = batch.get(0).getInput().timeSteps;
		Shape shape = Shape.of(batch.size(), nTS);
		TFloat32 amount = TFloat32.tensorOf(shape);
		DataBuffer<String> useChip = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> merchantName = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> merchantCity = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> merchantState = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> zip = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> mcc = DataBuffers.ofObjects(String.class, shape.size());
		DataBuffer<String> errors = DataBuffers.ofObjects(String.class, shape.size());
		// DataBuffer<Long> date = DataBuffers.ofLongs(shape.size());
		TInt64 date = TInt64.tensorOf(shape);

		for (int i = 0; i < batch.size(); i++) {
			Inputs inputs = batch.get(i).getInput();
			for (int j = 0; j < nTS; j++) {
				amount.setFloat(inputs.Amount[0][j].floatValue(), i, j);
				date.setLong(inputs.YearMonthDayTime[0][j], i, j);

				useChip.setObject(inputs.UseChip[0][j], i * nTS + j);
				merchantName.setObject(inputs.MerchantName[0][j], i * nTS + j);
				merchantCity.setObject(inputs.MerchantCity[0][j], i * nTS + j);
				merchantState.setObject(inputs.MerchantState[0][j], i * nTS + j);
				zip.setObject(inputs.Zip[0][j], i * nTS + j);
				mcc.setObject(inputs.MCC[0][j], i * nTS + j);
				errors.setObject(inputs.Errors[0][j], i * nTS + j);
			}

		}
		Map<String, Tensor> inputMap = new HashMap<>();
		inputMap.put("Amount", amount);
		inputMap.put("UseChip", TString.tensorOf(shape, useChip));
		inputMap.put("MerchantName", TString.tensorOf(shape, merchantName));
		inputMap.put("MerchantCity", TString.tensorOf(shape, merchantCity));
		inputMap.put("MerchantState", TString.tensorOf(shape, merchantState));
		inputMap.put("Zip", TString.tensorOf(shape, zip));
		inputMap.put("MCC", TString.tensorOf(shape, mcc));
		inputMap.put("Errors", TString.tensorOf(shape, errors));
		inputMap.put("YearMonthDayTime", date);
		
		return inputMap;
	}
	protected void setResult(List<Job<Inputs>> batch, TFloat32 result) {
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = result.getFloat(numberTimesteps() - 1, i, 0) > 0.5;
			batch.get(i).setResult(fraud);
		}
	}
	
	protected void batchPredict(List<Job<Inputs>> batch) {
		Map<String, Tensor> inputMap = tensorfyBatchInputs(batch);
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
