package com.ibm.lozperf.mb.modeladapter.triton;

import java.util.List;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.modeladapter.stringlookup.ModelStringLookup;

import inference.GrpcService.ModelInferRequest.InferInputTensor;

public class TabbertTritonAdapter extends TritonAdapter {
	
	public final static String MODEL_NAME = "tabbert";
	public final static String MODEL_VERSION = "";
	
	private final static String TRITON_HOST = System.getenv("TRITON_HOST");
	private final static int TRITON_PORT = Integer.parseInt(System.getenv("TRITON_PORT"));
	private final static String TABBERT_STRING_LOOKUP_DIR = System.getenv("TABBERT_STRING_LOOKUP_DIR");
	
	protected ModelStringLookup maps;
	
	public TabbertTritonAdapter() {
		super(TRITON_HOST, TRITON_PORT, MODEL_NAME, MODEL_VERSION);
		maps = new ModelStringLookup(TABBERT_STRING_LOOKUP_DIR);
	}

	@Override
	protected List<InferInputTensor> makeInputTensors(ModelInputs modelInputs) {
		return List.of(
			toGrpcInput(modelInputs.YearDiff[0], "YearDiff"),
			toGrpcInput(modelInputs.Month[0], "Month"),
			toGrpcInput(modelInputs.Day[0], "Day"),
			toGrpcInput(modelInputs.Hour[0], "Hour"),
			toGrpcInput(modelInputs.Minute[0], "Minute"),
			//toGrpcInput(modelInputs.DayofWeek[0], "DayOfWeek"));
			toGrpcInput(modelInputs.Amount[0], "Amount"),
			toGrpcInput(modelInputs.UseChip[0], "UseChip"),
			lookup(maps.name, modelInputs.MerchantName[0], "MerchantName"),
			lookup(maps.city, modelInputs.MerchantCity[0], "MerchantCity"),
			lookup(maps.state, modelInputs.MerchantState[0], "MerchantState"),
			lookup(maps.zip, modelInputs.Zip[0], "Zip"),
			lookup(maps.mcc, modelInputs.MCC[0], "MCC")
		);
	}
}
