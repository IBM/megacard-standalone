package com.ibm.lozperf.mb.modeladapter.triton;

import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.modeladapter.stringlookup.ModelStringLookup;

import inference.GrpcService.ModelInferRequest.InferInputTensor;

public class LstmTritonAdapter extends TritonAdapter {
	
	public final static String MODEL_NAME = "lstm";
	public final static String MODEL_VERSION = "";
	
	private final static String STRING_LOOKUP_DIR = System.getenv("STRING_MAP_DIR");
	
	protected ModelStringLookup maps;
	
	public LstmTritonAdapter() {
		super(TRITON_HOST, TRITON_PORT, MODEL_NAME, MODEL_VERSION);
		maps = new ModelStringLookup(STRING_LOOKUP_DIR);
	}

	@Override
	protected List<InferInputTensor> makeInputTensors(ModelInputs modelInputs) {
		return List.of(
//				new OMTensor(amounts, shape),
//				new OMTensor(modelInputs.Day[0], shape),
//				new OMTensor(modelInputs.Hour[0], shape),
//				new OMTensor(mccs, shape),
//				new OMTensor(cities, shape),
//				new OMTensor(names, shape), 
//				new OMTensor(states, shape),
//				new OMTensor(modelInputs.Minute[0], shape),
//				new OMTensor(modelInputs.Month[0], shape),
//				new OMTensor(modelInputs.TimeDelta[0], shape),
//				new OMTensor(modelInputs.UseChip[0], shape),
//				new OMTensor(zips, shape) });
				
			toGrpcInput(modelInputs.Amount[0], "Amount"),
			toGrpcInput(modelInputs.Day[0], "Day"),
			toGrpcInput(modelInputs.Hour[0], "Hour"),
			lookup(maps.mcc, modelInputs.MCC[0], "MCC"),
			lookup(maps.city, modelInputs.MerchantCity[0], "MerchantCity"),
			lookup(maps.name, modelInputs.MerchantName[0], "MerchantName"),
			lookup(maps.state, modelInputs.MerchantState[0], "MerchantState"),
			toGrpcInput(modelInputs.Minute[0], "Minute"),
			toGrpcInput(modelInputs.Month[0], "Month"),
			toGrpcInput(modelInputs.UseChip[0], "UseChip"),
			toGrpcInput(modelInputs.TimeDelta[0], "TimeDelta"),
			//toGrpcInput(modelInputs.DayofWeek[0], "DayOfWeek"));
			lookup(maps.zip, modelInputs.Zip[0], "Zip")
			
		);
	}
}
