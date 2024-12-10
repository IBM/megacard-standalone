package com.ibm.lozperf.mb.modeladapter.triton;

import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.google.common.primitives.Floats;
import com.google.errorprone.annotations.ForOverride;
import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.modeladapter.FraudProbability;
import com.ibm.lozperf.mb.modeladapter.stringlookup.ModelStringLookup;
import com.ibm.lozperf.mb.modeladapter.stringlookup.StringLookup;

import inference.GRPCInferenceServiceGrpc;
import inference.GRPCInferenceServiceGrpc.GRPCInferenceServiceBlockingStub;
import inference.GrpcService.InferTensorContents;
import inference.GrpcService.ModelInferRequest;
import inference.GrpcService.ModelInferRequest.InferInputTensor;
import inference.GrpcService.ModelInferResponse;
import inference.GrpcService.ModelInferResponse.InferOutputTensor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class TritonAdapter implements ModelAdapter, FraudProbability {
	
	public final static String MODEL_NAME = "tabbert";
	public final static String MODEL_VERSION = "";
	
	private final static String TRITON_HOST = System.getenv("TRITON_HOST");
	private final static int TRITON_PORT = Integer.parseInt(System.getenv("TRITON_PORT"));
	private final static String TABBERT_STRING_LOOKUP_DIR = System.getenv("TABBERT_STRING_LOOKUP_DIR");
	
	protected ModelStringLookup maps;
	protected GRPCInferenceServiceBlockingStub grpc_stub;
	private ManagedChannel channel;
	
	public TritonAdapter() {
		channel = ManagedChannelBuilder.forAddress(TRITON_HOST, TRITON_PORT).usePlaintext().build();
		grpc_stub = GRPCInferenceServiceGrpc.newBlockingStub(channel);
		maps = new ModelStringLookup(TABBERT_STRING_LOOKUP_DIR);
	}

	@Override
	public void close() throws Exception {
		channel.shutdownNow();	
	}
	
	public static InferInputTensor.Builder toGrpcInput(long[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT64");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllInt64Contents(LongStream.of(data).boxed().toList());
		input.setContents(content);
		return input;
	}
	
	public static InferInputTensor.Builder toGrpcInput(int[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT32");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllIntContents(IntStream.of(data).boxed().toList());
		input.setContents(content);
		return input;
	}
	
	public static InferInputTensor.Builder toGrpcInputInt32(long[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT32");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllIntContents(LongStream.of(data).mapToInt(x -> (int) x).boxed().toList());
		input.setContents(content);
		return input;
	}
	
	public static InferInputTensor.Builder toGrpcInput(BigDecimal[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("FP32");
		input.addShape(1);
		input.addShape(data.length);

		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllFp32Contents(Arrays.stream(data).map(x->x.floatValue()).toList());
		input.setContents(content);
		return input;
	}
	
	public static InferInputTensor.Builder lookup(StringLookup map, String[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT32");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		for (String s: data) {
			int val = map.lookup(s);
			content.addIntContents(val);
		}
		input.setContents(content);
		return input;
	}

	@Override
	public float checkFraudProbability(ModelInputs modelInputs) {
		ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
		request.setModelName(MODEL_NAME);
		request.setModelVersion(MODEL_VERSION);
		
		request.addInputs(toGrpcInput(modelInputs.YearDiff[0], "YearDiff"));
		request.addInputs(toGrpcInput(modelInputs.Month[0], "Month"));
		request.addInputs(toGrpcInput(modelInputs.Day[0], "Day"));
		request.addInputs(toGrpcInput(modelInputs.Hour[0], "Hour"));
		request.addInputs(toGrpcInput(modelInputs.Minute[0], "Minute"));
		request.addInputs(toGrpcInput(modelInputs.DayofWeek[0], "DayOfWeek"));
		request.addInputs(toGrpcInput(modelInputs.Amount[0], "Amount"));
		request.addInputs(toGrpcInputInt32(modelInputs.UseChip[0], "UseChip"));
		request.addInputs(lookup(maps.name, modelInputs.MerchantName[0], "MerchantName"));
		request.addInputs(lookup(maps.city, modelInputs.MerchantCity[0], "MerchantCity"));
		request.addInputs(lookup(maps.state, modelInputs.MerchantState[0], "MerchantState"));
		request.addInputs(lookup(maps.zip, modelInputs.Zip[0], "Zip"));
		request.addInputs(lookup(maps.mcc, modelInputs.MCC[0], "MCC"));

		// Populate the outputs in the inference request
		ModelInferRequest.InferRequestedOutputTensor.Builder output0 = ModelInferRequest.InferRequestedOutputTensor
				.newBuilder();
		output0.setName("logits");

		request.addOutputs(0, output0);

		ModelInferResponse response = grpc_stub.modelInfer(request.build());
		System.out.println(response);

		// Get the response outputs
		FloatBuffer output_data = response.getRawOutputContentsList().get(0).asReadOnlyByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
		
//		InferOutputTensor output = response.getOutputs(0);
//		var shape = output.getShapeList();
//		int last = 1;
//		for(long d: shape) {
//			last*=(int)d;
//		}
//		last--;
		return output_data.get(13);
	}

}
