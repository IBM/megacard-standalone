package com.ibm.lozperf.mb.modeladapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

public class DLCModelBatchingAdapter extends AbstractBatchingAdapter {

	public final static Path STRING_MAP_DIR = Paths.get(System.getenv("STRING_MAP_DIR"));
	public final static Map<String, Integer> mccMap = loadMap("MCC.csv");
	public final static Map<String, Integer> cityMap = loadMap("MerchantCity.csv");
	public final static Map<String, Integer> nameMap = loadMap("MerchantName.csv");
	public final static Map<String, Integer> stateMap = loadMap("MerchantState.csv");
	public final static Map<String, Integer> zipMap = loadMap("Zip.csv");

	private static Map<String, Integer> loadMap(String name) {
		File f = STRING_MAP_DIR.resolve(Paths.get(name)).toFile();
		System.out.println("loading " + f);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			return br.lines().map(s -> s.split("\\|"))
					.collect(Collectors.toMap(a -> a[0], a -> Integer.parseInt(a[1])));
		} catch (Exception e) {
			System.err.println("Error loading " + f);
			e.printStackTrace();
			return null;
		}
	}

	public static void map(String[] inp, Map<String, Integer> map, long[] target, int targetbase) {
		for (int j = 0; j < inp.length; j++) {
			target[targetbase + j] = map.getOrDefault(inp[j], 0);
		}
	}

	@Override
	protected void batchPredict(List<Job<Inputs>> batch) {
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
				Inputs inputs = batch.get(i).getInput();
				for (int j = 0, idx = base; j < inputs.Amount[0].length; j++, idx++) {
					amounts[idx] = inputs.Amount[0][j].floatValue();
				}
				System.arraycopy(inputs.Day[0], 0, days, base, inputs.Day[0].length);
				System.arraycopy(inputs.Hour[0], 0, hours, base, inputs.Hour[0].length);
				System.arraycopy(inputs.Minute[0], 0, minutes, base, inputs.Minute[0].length);
				System.arraycopy(inputs.Month[0], 0, months, base, inputs.Month[0].length);
				System.arraycopy(inputs.TimeDelta[0], 0, timeDeltas, base, inputs.TimeDelta[0].length);
				System.arraycopy(inputs.UseChip[0], 0, useChip, base, inputs.UseChip[0].length);
				map(inputs.MCC[0], mccMap, mccs, base);
				map(inputs.MerchantCity[0], cityMap, cities, base);
				map(inputs.MerchantName[0], nameMap, names, base);
				map(inputs.MerchantState[0], stateMap, states, base);
				map(inputs.Zip[0], zipMap, zips, base);
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
		for (int i = 0; i < batch.size(); i++) {
			batch.get(i).setResult(results[i] > 0.5);
		}
	}

	@Override
	public int numberTimesteps() {
		return 7;
	}
}
