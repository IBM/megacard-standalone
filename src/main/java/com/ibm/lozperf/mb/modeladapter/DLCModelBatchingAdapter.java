package com.ibm.lozperf.mb.modeladapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.batching.Job;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

public class DLCModelBatchingAdapter extends AbstractBatchingAdapter {

	private final static Path STRING_MAP_DIR = Paths.get(System.getenv("STRING_MAP_DIR"));
	private final static Map<String, Integer> mccMap = loadMap("MCC.csv");
	private final static Map<String, Integer> cityMap = loadMap("MerchantCity.csv");
	private final static Map<String, Integer> nameMap = loadMap("MerchantName.csv");
	private final static Map<String, Integer> stateMap = loadMap("MerchantState.csv");
	private final static Map<String, Integer> zipMap = loadMap("Zip.csv");

	private static Map<String, Integer> loadMap(String name) {
		File f = STRING_MAP_DIR.resolve(Paths.get(name)).toFile();
		System.out.println("loading " + f);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			return br.lines().map(s -> s.split("\\|")).collect(Collectors.toMap(a -> a[0], a -> Integer.parseInt(a[1])));
		} catch (Exception e) {
			System.err.println("Error loading " + f);
			e.printStackTrace();
			return null;
		}
	}

	private void map(String[] inp, Map<String, Integer> map, int[] target, int targetbase) {
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
			int[] mccs = new int[inpLenght];
			int[] cities = new int[inpLenght];
			int[] names = new int[inpLenght];
			int[] states = new int[inpLenght];
			int[] minutes = new int[inpLenght];
			int[] months = new int[inpLenght];
			long[] timeDeltas = new long[inpLenght];
			int[] useChip = new int[inpLenght];
			int[] zips = new int[inpLenght];

			Calendar calendar = Calendar.getInstance();

			for (int i = 0, base = 0; i < batch.size(); i++, base += nTS) {
				Inputs inputs = batch.get(i).getInput();
				long lastTime = 0;
				for (int j = 0, idx = base; j < inputs.YearMonthDayTime[0].length; j++, idx++) {
					long time = inputs.YearMonthDayTime[0][j];
					timeDeltas[idx] = time - lastTime;
					lastTime = time;
					calendar.setTimeInMillis(time);
					months[idx] = calendar.get(Calendar.MONTH);
					days[idx] = calendar.get(Calendar.DAY_OF_MONTH);
					hours[idx] = calendar.get(Calendar.HOUR_OF_DAY);
					minutes[idx] = calendar.get(Calendar.MINUTE);
					useChip[idx] = inputs.UseChip[0][j].ordinal();
					amounts[idx] = inputs.Amount[0][j].floatValue();
				}
				timeDeltas[base] = 0; // lastTime is invalid for the first timeDelta
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
