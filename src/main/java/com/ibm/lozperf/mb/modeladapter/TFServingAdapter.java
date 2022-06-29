package com.ibm.lozperf.mb.modeladapter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.ibm.lozperf.mb.ModelInputs;
import com.ibm.lozperf.mb.ModelOutputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class TFServingAdapter implements ModelAdapter {

	private final static String TF_URL = System.getenv("TF_URL");

	private Client httpClient;
	protected WebTarget modelServer;

	public TFServingAdapter() {
		ClientBuilder cb = ClientBuilder.newBuilder();
		cb.property("com.ibm.ws.jaxrs.client.keepalive.connection", "keep-alive");
		cb.property("com.ibm.ws.jaxrs.client.connection.timeout", "10000");
		httpClient = cb.build();
		// httpClient = ClientBuilder.newClient();
		modelServer = httpClient.target(TF_URL);
	}

	protected float[][] doRequest(ModelInputs modelInputs) {
		ServingInputWrapper tfInputs = new ServingInputWrapper(modelInputs);
		Entity<ServingInputWrapper> entity = Entity.json(tfInputs);
		try (Response resp = modelServer.request().post(entity)) {
			int httpStatus = resp.getStatus();
			if (httpStatus != 200) {
				System.err.println("Got " + httpStatus + " from TF Server\n" + resp.readEntity(String.class));
				System.err.println(tfInputs.toString());
				return null;
			}
			// System.out.println(resp.readEntity(String.class));
			return resp.readEntity(ModelOutputs.class).outputs;
		}
	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		float[][] outputs = doRequest(modelInputs);
		if(outputs == null)
			return false;
		float fraud = outputs[outputs[0].length - 1][0];
		// System.out.println("Fraud Propability: " + frBoolean.parseBoolean(aud);
		boolean isFraud = fraud > 0.5;
		if (isFraud) {
			// System.out.println("FRAUD FRAUD FRAUD: " + fraud);
		}

		return isFraud;

	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}

}
