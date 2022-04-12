package com.ibm.lozperf.mb.modeladapter;

import java.util.HashMap;
import java.util.Map;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.SavedModelBundle.Loader;
import org.tensorflow.Tensor;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TString;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class TFJavaAdapter implements ModelAdapter {

	private final static String TF_MODEL = System.getenv("TF_MODEL");

	protected SavedModelBundle smb;

	public TFJavaAdapter() {
		Loader loader = SavedModelBundle.loader(TF_MODEL);
		smb = loader.load();
	}

	@Override
	public void close() throws Exception {
		smb.close();
	}

	
	public Map<String, Tensor> tensorfyInputs(Inputs inputs){
		Map<String, Tensor> inputMap = new HashMap<>();
		float[] amounts = new float[inputs.Amount[0].length];
		for (int i = 0; i < amounts.length; i++) {
			amounts[i] = inputs.Amount[0][i].floatValue();
		}
		inputMap.put("Amount", TFloat32.vectorOf(amounts));
		inputMap.put("UseChip", TString.vectorOf(inputs.UseChip[0].toString()));
		inputMap.put("MerchantName", TString.vectorOf(inputs.MerchantName[0]));
		inputMap.put("MerchantCity", TString.vectorOf(inputs.MerchantCity[0]));
		inputMap.put("MerchantState", TString.vectorOf(inputs.MerchantState[0]));
		inputMap.put("Zip", TString.vectorOf(inputs.Zip[0]));
		inputMap.put("MCC", TString.vectorOf(inputs.MCC[0]));
		inputMap.put("Errors", TString.vectorOf(inputs.Errors[0]));
		inputMap.put("YearMonthDayTime", TInt64.vectorOf(inputs.YearMonthDayTime[0]));
		return inputMap;
	}
	
	@Override
	public boolean checkFraud(Inputs inputs) {

		Map<String,Tensor> map = tensorfyInputs(inputs);
		map = smb.call(map);
		TFloat32 tf32 = (TFloat32) map.get("sequential_12");
		return tf32.getFloat(tf32.size() - 1, 0, 0) > 0.5;
	}

	@Override
	public int numberTimesteps() {
		return 7;
	}

}
