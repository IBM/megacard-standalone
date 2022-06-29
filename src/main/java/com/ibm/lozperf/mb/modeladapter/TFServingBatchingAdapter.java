package com.ibm.lozperf.mb.modeladapter;

import java.math.BigDecimal;
import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.batching.BatchCollector;
import com.ibm.lozperf.mb.batching.Job;

public class TFServingBatchingAdapter extends TFServingAdapter {

	
	private BatchCollector<ModelInputs> batchCollector = new BatchCollector<>((batch)-> batchPredict(batch));
	


	public TFServingBatchingAdapter() {

	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		return batchCollector.predict(modelInputs);
	}
	
	
	public void batchPredict(List<Job<ModelInputs>> batch) {
		int bs = batch.size();
		ModelInputs bmi = new ModelInputs();
		bmi.Amount = new BigDecimal[bs][];
		bmi.Day = new int[bs][];
		bmi.Errors = new String[bs][];
		bmi.Hour = new int[bs][];
		bmi.MCC = new String[bs][];
		bmi.MerchantCity = new String[bs][];
		bmi.MerchantName = new String[bs][];
		bmi.MerchantState = new String[bs][];
		bmi.Minute = new int[bs][];
		bmi.Month = new int[bs][];
		bmi.TimeDelta = new long[bs][];
		bmi.UseChip = new long[bs][];
		bmi.Zip = new String[bs][];
		
		for(int i=0; i<batch.size(); i++) {
			ModelInputs mi = batch.get(i).getInput();
			bmi.Amount[i] = mi.Amount[0];
			bmi.Day[i] = mi.Day[0];
			bmi.Errors[i] = mi.Errors[0];
			bmi.Hour[i] = mi.Hour[0];
			bmi.MCC[i] = mi.MCC[0];
			bmi.MerchantCity[i] = mi.MerchantCity[0];
			bmi.MerchantName[i] = mi.MerchantName[0];
			bmi.MerchantState[i] = mi.MerchantState[0];
			bmi.Minute[i] = mi.Minute[0];
			bmi.Month[i] = mi.Month[0];
			bmi.TimeDelta[i] = mi.TimeDelta[0];
			bmi.UseChip[i] = mi.UseChip[0];
			bmi.Zip[i] = mi.Zip[0];			
		}
		
		float[][] result = doRequest(bmi);
		
		for(int i=0; i<batch.size(); i++) {
			batch.get(i).setResult(result[i][result[i].length-1] > 0.5);
		}
		
	}

	@Override
	public void close() throws Exception {
		batchCollector.close();
		super.close();
	}

}
