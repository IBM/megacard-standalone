package com.ibm.lozperf.mb.modeladapter;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.modeladapter.stringlookup.StringLookup;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtException;

public class OnnxModelAdapter implements ModelAdapter {

	public final static Path STRING_MAP_DIR = Paths.get(System.getenv("STRING_MAP_DIR"));
	public final static StringLookup mccMap = loadMap("MCC.csv");
	public final static StringLookup cityMap = loadMap("MerchantCity.csv");
	public final static StringLookup nameMap = loadMap("MerchantName.csv");
	public final static StringLookup stateMap = loadMap("MerchantState.csv");
	public final static StringLookup zipMap = loadMap("Zip.csv");

	private final Map<String, OrtSession> sessionCache = new HashMap<>();
	private final OrtEnvironment env = null;

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

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		final int nTS = numberTimesteps();

		// Prepare the input arrays
		float[] amounts = new float[nTS];
		for (int j = 0; j < modelInputs.Amount[0].length; j++) {
			amounts[j] = modelInputs.Amount[0][j].floatValue();
		}

		long[] mccs = new long[nTS];
		mccMap.lookup(modelInputs.MCC[0], mccs, 0);
		long[] cities = new long[nTS];
		cityMap.lookup(modelInputs.MerchantCity[0], cities, 0);
		long[] names = new long[nTS];
		nameMap.lookup(modelInputs.MerchantName[0], names, 0);
		long[] states = new long[nTS];
		stateMap.lookup(modelInputs.MerchantState[0], states, 0);
		long[] zips = new long[nTS];
		zipMap.lookup(modelInputs.Zip[0], zips, 0);

		// Shape for the tensors (1, numberTimesteps)
		long[] shape = { 1, nTS };

		// Create the ONNX Runtime environment and session
		try (OrtEnvironment env = OrtEnvironment.getEnvironment()) {

			try {
				OrtSession session = getSession(
						"/opt/ibm/wlp/usr/shared/resources/onnx/ccf_new-noerror-7timedelta-1024_x86-inf.onnx");

				// Prepare the input tensor objects

				OnnxTensor amountTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(amounts), shape);
				OnnxTensor dayTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(modelInputs.Day[0]), shape);
				OnnxTensor hourTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(modelInputs.Hour[0]), shape);
				OnnxTensor mccTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(mccs), shape);
				OnnxTensor cityTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(cities), shape);
				OnnxTensor nameTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(names), shape);
				OnnxTensor stateTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(states), shape);
				OnnxTensor zipTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(zips), shape);
				OnnxTensor minuteTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(modelInputs.Minute[0]), shape);
				OnnxTensor monthTensor = OnnxTensor.createTensor(env, IntBuffer.wrap(modelInputs.Month[0]), shape);
				OnnxTensor timeDeltaTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(modelInputs.TimeDelta[0]),
						shape);
				OnnxTensor useChipTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(modelInputs.UseChip[0]), shape);

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
					// Get the output tensor (assuming it's the first output)
					OnnxTensor outputTensor = (OnnxTensor) result.get(0);
					float[][] output = (float[][]) outputTensor.getValue();
					// Assuming the output is a single scalar value, check if it exceeds 0.5
					assert output.length == 1;
					return output[0][0] > 0.5;
				}

			} catch (OrtException e) {
				e.printStackTrace();
				return false;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

}
