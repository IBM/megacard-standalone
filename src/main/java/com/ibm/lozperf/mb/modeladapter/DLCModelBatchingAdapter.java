package com.ibm.lozperf.mb.modeladapter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.batching.Job;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

public class DLCModelBatchingAdapter extends AbstractBatchingAdapter {

	public final static Path STRING_MAP_DIR = Paths.get(System.getenv("STRING_MAP_DIR"));
	public final static StringLookup mccMap = loadMap("MCC.csv");
	public final static StringLookup cityMap = loadMap("MerchantCity.csv");
	public final static StringLookup nameMap = loadMap("MerchantName.csv");
	public final static StringLookup stateMap = loadMap("MerchantState.csv");
	public final static StringLookup zipMap = loadMap("Zip.csv");

	private static StringLookup loadMap(String name) {
		File f = STRING_MAP_DIR.resolve(Paths.get(name)).toFile();
		System.out.println("loading " + f);
		try  {
			return new StringLookup(f);
		} catch (Exception e) {
			System.err.println("Error loading " + f);
			e.printStackTrace();
			return null;
		}
	}

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
				mccMap.lookup(modelInputs.MCC[0], mccs, base);
				cityMap.lookup(modelInputs.MerchantCity[0], cities, base);
				nameMap.lookup(modelInputs.MerchantName[0], names, base);
				stateMap.lookup(modelInputs.MerchantState[0], states, base);
				zipMap.lookup(modelInputs.Zip[0], zips, base);
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
			batch.get(i).setResult(results[i] > 0.5);
		}
	}

	@Override
	public int numberTimesteps() {
		return 7;
	}
}
