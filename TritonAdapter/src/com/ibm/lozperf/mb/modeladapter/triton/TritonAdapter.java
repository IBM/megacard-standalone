package com.ibm.lozperf.mb.modeladapter.triton;

import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.modeladapter.FraudProbability;
import com.ibm.lozperf.mb.modeladapter.stringlookup.StringLookup;

import inference.GRPCInferenceServiceGrpc;
import inference.GRPCInferenceServiceGrpc.GRPCInferenceServiceBlockingStub;
import inference.GrpcService.InferTensorContents;
import inference.GrpcService.ModelInferRequest;
import inference.GrpcService.ModelInferRequest.InferInputTensor;
import inference.GrpcService.ModelInferResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public abstract class TritonAdapter implements FraudProbability {
	

	protected GRPCInferenceServiceBlockingStub grpc_stub;
	private ManagedChannel channel;
	private String modelName;
	private String modelVersion;
	
	public TritonAdapter(String host, int port, String modelName, String modelVersion) {
		this.modelName = modelName;
		this.modelVersion = modelVersion;
		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		grpc_stub = GRPCInferenceServiceGrpc.newBlockingStub(channel);
	}

	@Override
	public void close() throws Exception {
		channel.shutdownNow();	
	}
	
	public static InferInputTensor toGrpcInput(long[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT64");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllInt64Contents(LongStream.of(data).boxed().toList());
		input.setContents(content);
		return input.build();
	}
	
	public static InferInputTensor toGrpcInput(int[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT32");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllIntContents(IntStream.of(data).boxed().toList());
		input.setContents(content);
		return input.build();
	}
	
	public static InferInputTensor toGrpcInputInt32(long[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT32");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllIntContents(LongStream.of(data).mapToInt(x -> (int) x).boxed().toList());
		input.setContents(content);
		return input.build();
	}
	
	public static InferInputTensor toGrpcInput(BigDecimal[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("FP32");
		input.addShape(1);
		input.addShape(data.length);

		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		content.addAllFp32Contents(Arrays.stream(data).map(x->x.floatValue()).toList());
		input.setContents(content);
		return input.build();
	}
	
	public static InferInputTensor lookup(StringLookup map, String[] data, String name) {
		InferInputTensor.Builder input = ModelInferRequest.InferInputTensor
				.newBuilder();
		input.setName(name);
		input.setDatatype("INT64");
		input.addShape(1);
		input.addShape(data.length);
		
		InferTensorContents.Builder content = InferTensorContents.newBuilder();
		for (String s: data) {
			int val = map.lookup(s);
			content.addInt64Contents(val);
		}
		input.setContents(content);
		return input.build();
	}
	
	abstract protected List<InferInputTensor> makeInputTensors(ModelInputs modelInputs);

	@Override
	public float checkFraudProbability(ModelInputs modelInputs) {
		ModelInferRequest.Builder request = ModelInferRequest.newBuilder();
		request.setModelName(modelName);
		request.setModelVersion(modelVersion);
		
		var inputs = makeInputTensors(modelInputs);
		request.addAllInputs(inputs);

		// Populate the outputs in the inference request
		ModelInferRequest.InferRequestedOutputTensor.Builder output0 = ModelInferRequest.InferRequestedOutputTensor
				.newBuilder();
		output0.setName("sequential_1");

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
		float res = output_data.get(0);
		System.out.println(res);
		return res;
	}

}
