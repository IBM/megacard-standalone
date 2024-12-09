package com.ibm.lozperf.mb.modeladapter;

import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.onnxmlir.OMModel;
import com.ibm.onnxmlir.OMTensor;
import com.ibm.onnxmlir.OMTensorList;

import static com.ibm.lozperf.mb.modeladapter.DLCModelBatchingAdapter.maps;

public class DLCModelAdapter implements ModelAdapter, FraudProbability {

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public float checkFraudProbability(ModelInputs modelInputs) {
		final int nTS = numberTimesteps();
		float[] amounts = new float[nTS];
		for (int j = 0; j < modelInputs.Amount[0].length; j++) {
			amounts[j] = modelInputs.Amount[0][j].floatValue();
		}
		long[] mccs = new long[nTS];
		maps.mcc.lookup(modelInputs.MCC[0], mccs, 0);
		long[] cities = new long[nTS];
		maps.city.lookup(modelInputs.MerchantCity[0], cities, 0);
		long[] names = new long[nTS];
		maps.name.lookup(modelInputs.MerchantName[0], names, 0);
		long[] states = new long[nTS];
		maps.state.lookup(modelInputs.MerchantState[0], states, 0);
		long[] zips = new long[nTS];
		maps.zip.lookup(modelInputs.Zip[0], zips, 0);
		
		long[] shape = { 1, numberTimesteps() };
		OMTensorList tensorList = new OMTensorList(new OMTensor[] { 
				new OMTensor(amounts, shape),
				new OMTensor(modelInputs.Day[0], shape),
				new OMTensor(modelInputs.Hour[0], shape),
				new OMTensor(mccs, shape),
				new OMTensor(cities, shape),
				new OMTensor(names, shape), 
				new OMTensor(states, shape),
				new OMTensor(modelInputs.Minute[0], shape),
				new OMTensor(modelInputs.Month[0], shape),
				new OMTensor(modelInputs.TimeDelta[0], shape),
				new OMTensor(modelInputs.UseChip[0], shape),
				new OMTensor(zips, shape) });
		tensorList = OMModel.mainGraph(tensorList);
		float[] results = tensorList.getOmtByIndex(0).getFloatData();
		assert(results.length == 1);
		return results[0];
	}

}
