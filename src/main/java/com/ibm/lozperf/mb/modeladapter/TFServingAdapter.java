package com.ibm.lozperf.mb.modeladapter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.ibm.lozperf.mb.Inputs;
import com.ibm.lozperf.mb.LafalceOutputs;
import com.ibm.lozperf.mb.ModelAdapter;

public class TFServingAdapter implements ModelAdapter {

	private final static String TF_URL = System.getenv("TF_URL");

	private Client httpClient;
	private WebTarget modelServer;

	public TFServingAdapter() {
		ClientBuilder cb = ClientBuilder.newBuilder();
		cb.property("com.ibm.ws.jaxrs.client.keepalive.connection", "keep-alive");
		cb.property("com.ibm.ws.jaxrs.client.connection.timeout", "1000");
		httpClient = cb.build();
		// httpClient = ClientBuilder.newClient();
		modelServer = httpClient.target(TF_URL);
	}

	@Override
	public boolean checkFraud(Inputs inputs) {
		ServingInputWrapper tfInputs = new ServingInputWrapper(inputs);
		Entity<ServingInputWrapper> entity = Entity.json(tfInputs);
		try (Response resp = modelServer.request().post(entity)) {
			int httpStatus = resp.getStatus();
			if (httpStatus != 200) {
				System.err.println("Got " + httpStatus + " from TF Server\n" + resp.readEntity(String.class));
				System.err.println(tfInputs.toString());
				return false;
			}
			// System.out.println(resp.readEntity(String.class));
			float[][][] outputs = resp.readEntity(LafalceOutputs.class).outputs;
			float fraud = outputs[outputs.length - 1][0][0];
			// System.out.println("Fraud Propability: " + frBoolean.parseBoolean(aud);
			boolean isFraud = fraud > 0.5;
			if (isFraud) {
				// System.out.println("FRAUD FRAUD FRAUD: " + fraud);
			}

			return isFraud;
		}
	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}

}
