package com.ibm.lozperf.mb.modeladapter;

import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.batching.Job;
import com.ibm.lozperf.mb.modeladapter.stringlookup.ModelStringLookup;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

public class DLCModelBatchingAdapter extends AbstractBatchingAdapter {

	public final static String STRING_MAP_DIR = System.getenv("STRING_MAP_DIR");
	public final static ModelStringLookup maps = new ModelStringLookup(STRING_MAP_DIR);

	@Override
	protected void batchPredict(List<Job<ModelInputs>> batch) {
		final int nTS = numberTimesteps();
		OMTensorList tensorList;
		{
			final int inpLenght = batch.size() * nTS;
			float[] amounts = new float[inpLenght];
			int[] days = new int[inpLenght];
			int[] hours = new int[inpLenght];
			long[] mccs = new long[inpLenght];
			long[] cities = new long[inpLenght];
			long[] names = new long[inpLenght];
			long[] states = new long[inpLenght];
			int[] minutes = new int[inpLenght];
			int[] months = new int[inpLenght];
			long[] timeDeltas = new long[inpLenght];
			long[] useChip = new long[inpLenght];
			long[] zips = new long[inpLenght];

			for (int i = 0, base = 0; i < batch.size(); i++, base += nTS) {
				ModelInputs modelInputs = batch.get(i).getInput();
				for (int j = 0, idx = base; j < modelInputs.Amount[0].length; j++, idx++) {
					amounts[idx] = modelInputs.Amount[0][j].floatValue();
				}
				System.arraycopy(modelInputs.Day[0], 0, days, base, modelInputs.Day[0].length);
				System.arraycopy(modelInputs.Hour[0], 0, hours, base, modelInputs.Hour[0].length);
				System.arraycopy(modelInputs.Minute[0], 0, minutes, base, modelInputs.Minute[0].length);
				System.arraycopy(modelInputs.Month[0], 0, months, base, modelInputs.Month[0].length);
				System.arraycopy(modelInputs.TimeDelta[0], 0, timeDeltas, base, modelInputs.TimeDelta[0].length);
				System.arraycopy(modelInputs.UseChip[0], 0, useChip, base, modelInputs.UseChip[0].length);
				maps.mcc.lookup(modelInputs.MCC[0], mccs, base);
				maps.city.lookup(modelInputs.MerchantCity[0], cities, base);
				maps.name.lookup(modelInputs.MerchantName[0], names, base);
				maps.state.lookup(modelInputs.MerchantState[0], states, base);
				maps.zip.lookup(modelInputs.Zip[0], zips, base);
			}

			long[] shape = { batch.size(), nTS };
			tensorList = new OMTensorList(new OMTensor[] { new OMTensor(amounts, shape), new OMTensor(days, shape),
					new OMTensor(hours, shape), new OMTensor(mccs, shape), new OMTensor(cities, shape),
					new OMTensor(names, shape), new OMTensor(states, shape), new OMTensor(minutes, shape),
					new OMTensor(months, shape), new OMTensor(timeDeltas, shape), new OMTensor(useChip, shape),
					new OMTensor(zips, shape) });
		}
		tensorList = OMModel.mainGraph(tensorList);
		float[] results = tensorList.getOmtByIndex(0).getFloatData();
		assert(results.length == batch.size());
		for (int i = 0; i < batch.size(); i++) {
			batch.get(i).setResult(results[i]);
		}
	}

	@Override
	public int numberTimesteps() {
		return 7;
	}
}
