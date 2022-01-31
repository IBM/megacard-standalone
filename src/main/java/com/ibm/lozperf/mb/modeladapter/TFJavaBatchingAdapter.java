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

	private final static int nPredictThreads = Integer.parseInt(System.getenv("PREDICT_THREADS"));
	
	private BatchCollector<Inputs> batchCollector = new BatchCollector<>();
	private volatile boolean shutdown = false;
	private Thread predictThreads[] = new Thread[nPredictThreads];

	public TFJavaBatchingAdapter() {
		super();
		for (int i = 0; i < predictThreads.length; i++) {
			final int thNum = i;
			predictThreads[i] = new Thread() {
				private long nBatches = 0;
				private long nElements = 0;
				@Override
				public void run() {
					while (!shutdown) {
						try {
							List<Job<Inputs>> batch = batchCollector.removeBatch();
							batchPredict(batch);
							
							nBatches++;
							nElements += batch.size();
							if (nElements % 4000 == 0) {
								System.out.println(thNum + ": Avg Batch Size: " + ((float) nElements / nBatches));
								nBatches = 0;
								nElements = 0;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			predictThreads[i].start();
		}
	}

	protected void batchPredict(List<Job<Inputs>> batch) {
		int nTS = numberTimesteps();
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
				amount.setFloat(inputs.Amount[j].floatValue(), i, j);
				date.setLong(inputs.YearMonthDayTime[j], i, j);

				useChip.setObject(inputs.UseChip[j], i * nTS + j);
				merchantName.setObject(inputs.MerchantName[j], i * nTS + j);
				merchantCity.setObject(inputs.MerchantCity[j], i * nTS + j);
				merchantState.setObject(inputs.MerchantState[j], i * nTS + j);
				zip.setObject(inputs.Zip[j], i * nTS + j);
				mcc.setObject(inputs.MCC[j], i * nTS + j);
				errors.setObject(inputs.Errors[j], i * nTS + j);
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

		Map<String, Tensor> output = smb.call(inputMap);
		TFloat32 tf32 = (TFloat32) output.get("sequential_12");
		// System.out.println("Shape: " + tf32.shape());
		for (int i = 0; i < batch.size(); i++) {
			boolean fraud = tf32.getFloat(nTS - 1, i, 0) > 0.5;
			batch.get(i).setResult(fraud);
		}
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		return batchCollector.predict(inputs);
	}

	@Override
	public void close() throws Exception {
		shutdown = true;
		for (Thread predictThread : predictThreads)
			predictThread.interrupt();
		super.close();
	}
}
