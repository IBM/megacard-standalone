package com.ibm.lozperf.mb.modeladapter;

import ai.onnxruntime.*;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.batching.Job;

public class OnnxModelBatchingAdapter extends AbstractBatchingAdapter {

	public final static Path STRING_MAP_DIR = Paths.get(System.getenv("STRING_MAP_DIR"));
	public final static StringLookup mccMap = loadMap("MCC.csv");
	public final static StringLookup cityMap = loadMap("MerchantCity.csv");
	public final static StringLookup nameMap = loadMap("MerchantName.csv");
	public final static StringLookup stateMap = loadMap("MerchantState.csv");
	public final static StringLookup zipMap = loadMap("Zip.csv");

	private final Map<String, OrtSession> sessionCache = new HashMap<>();
	private final OrtEnvironment env;

	// Constructor: Initialize OrtEnvironment
	public OnnxModelBatchingAdapter() throws OrtException {
		env = OrtEnvironment.getEnvironment();
	}

	// Get cached OrtSession or create a new one if not found
	public OrtSession getSession(String modelPath) throws OrtException {
		// Check if the session for the given model path is already cached
		if (sessionCache.containsKey(modelPath)) {
			return sessionCache.get(modelPath);
		} else {
			// Create a new OrtSession if not found in the cache
			OrtSession session = env.createSession(modelPath, new OrtSession.SessionOptions());
			// Cache the newly created session
			sessionCache.put(modelPath, session);
			return session;
		}
	}

	// Close all cached sessions (optional cleanup method)
	public void close() {
		for (OrtSession session : sessionCache.values()) {
			try {
				session.close();
			} catch (OrtException e) {
				e.printStackTrace();
			}
		}
		sessionCache.clear();
	}

	private static StringLookup loadMap(String name) {
		File f = STRING_MAP_DIR.resolve(Paths.get(name)).toFile();
		System.out.println("loading " + f);
		try {
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

		// Prepare input arrays
		final int inpLength = batch.size() * nTS;
		float[] amounts = new float[inpLength];
		int[] days = new int[inpLength];
		int[] hours = new int[inpLength];
		long[] mccs = new long[inpLength];
		long[] cities = new long[inpLength];
		long[] names = new long[inpLength];
		long[] states = new long[inpLength];
		int[] minutes = new int[inpLength];
		int[] months = new int[inpLength];
		long[] timeDeltas = new long[inpLength];
		long[] useChip = new long[inpLength];
		long[] zips = new long[inpLength];

		// ModelInputs modelInputs = null;

		// Process the batch
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

		// Prepare tensors to input into the ONNX model
		try (OrtEnvironment env = OrtEnvironment.getEnvironment()) {

			try {
				OrtSession session = getSession(
						"/opt/ibm/wlp/usr/shared/resources/onnx/ccf_new-noerror-7timedelta-1024_x86-inf.onnx");

				// Define the shape for the tensors
				long[] shape = { batch.size(), nTS };

				// Create the tensors
				OnnxTensor amountTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(amounts), shape);
				OnnxTensor dayTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(days), shape);
				OnnxTensor hourTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(hours), shape);
				OnnxTensor mccTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(mccs), shape);
				OnnxTensor cityTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(cities), shape);
				OnnxTensor nameTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(names), shape);
				OnnxTensor stateTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(states), shape);
				OnnxTensor zipTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(zips), shape);
				OnnxTensor minuteTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(minutes), shape);
				OnnxTensor monthTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(months), shape);
				OnnxTensor timeDeltaTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(timeDeltas), shape);
				OnnxTensor useChipTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(useChip), shape);

				// Create an input map
				Map<String, OnnxTensor> inputs = new HashMap<>();
				inputs.put("Amount", amountTensor);
				inputs.put("Day", dayTensor);
				inputs.put("Hour", hourTensor);
				inputs.put("MCC", mccTensor);
				inputs.put("MerchantCity", cityTensor);
				inputs.put("MerchantName", nameTensor);
				inputs.put("MerchantState", stateTensor);
				inputs.put("Zip", zipTensor);
				inputs.put("Minute", minuteTensor);
				inputs.put("Month", monthTensor);
				inputs.put("TimeDelta", timeDeltaTensor);
				inputs.put("UseChip", useChipTensor);

				// Run the model
				try (OrtSession.Result result = session.run(inputs)) {
					// Process the output
					float[][] results = (float[][]) result.get(0).getValue();
					assert (results.length == batch.size());

					for (int i = 0; i < batch.size(); i++) {
						batch.get(i).setResult(results[i][0]);
					}

				}

			} catch (OrtException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public int numberTimesteps() {
		return 7;
	}
}
