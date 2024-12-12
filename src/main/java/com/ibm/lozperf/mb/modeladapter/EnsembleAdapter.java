package com.ibm.lozperf.mb.modeladapter;

import java.lang.reflect.InvocationTargetException;

import com.ibm.lozperf.mb.ModelAdapter;
import com.ibm.lozperf.mb.ModelInputs;

public class EnsembleAdapter implements ModelAdapter {
	
	private final static String ENSEMBLE_MODEL1_CLASS = System.getenv("ENSEMBLE_MODEL1_CLASS");
	private final static String ENSEMBLE_MODEL2_CLASS = System.getenv("ENSEMBLE_MODEL2_CLASS");
	private final static String ENSEMBLE_UNCERTAIN =  System.getenv("ENSEMBLE_UNCERTAIN");
	
	private static Class<?> model1Class;
	private static Class<?> model2Class;
	static {
		try {
			System.out.println("Ensemble Model1 Class Name: " + ENSEMBLE_MODEL1_CLASS);
			model1Class = Class.forName(ENSEMBLE_MODEL1_CLASS);
			System.out.println("Model1 Class: " + model1Class);
			
			System.out.println("Ensemble Model2 Class Name: " + ENSEMBLE_MODEL2_CLASS);
			model1Class = Class.forName(ENSEMBLE_MODEL2_CLASS);
			System.out.println("Model2 Class: " + model2Class);
		} catch (Throwable e) {
			System.err.println("Error loading Model Adapter used by Ensemble:");
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	private FraudProbability model1;
	private ModelAdapter model2;
	
	float uncertainInterval;
	
	public EnsembleAdapter() {
		uncertainInterval = Float.parseFloat(ENSEMBLE_UNCERTAIN);
		try {
			model1 = (FraudProbability) model1Class.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			System.err.println("Error instantiating " + model1Class);
			e.printStackTrace();
		}
		try {
			model2 = (FraudProbability) model2Class.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			System.err.println("Error instantiating " + model2Class);
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws Exception {
		model1.close();
		model2.close();
	}

	@Override
	public boolean checkFraud(ModelInputs modelInputs) {
		float res = model1.checkFraudProbability(modelInputs);
		if( Math.abs(res-0.5) > uncertainInterval) {
			return res > 0.5;
		}
	
		return model2.checkFraud(modelInputs);
	}

}
