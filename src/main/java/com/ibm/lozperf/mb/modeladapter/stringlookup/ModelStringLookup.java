package com.ibm.lozperf.mb.modeladapter.stringlookup;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModelStringLookup {
	
	public final StringLookup mcc;
	public final StringLookup city;
	public final StringLookup name;
	public final StringLookup state;
	public final StringLookup zip;

	public ModelStringLookup(String basePath) {
		Path path = Paths.get(basePath);
		mcc = loadMap(path, "MCC.csv");
		city = loadMap(path, "MerchantCity.csv");
		name = loadMap(path, "MerchantName.csv");
		state = loadMap(path, "MerchantState.csv");
		zip = loadMap(path, "Zip.csv");
	}
	
	private static StringLookup loadMap(Path basePath, String name) {
		File f = basePath.resolve(Paths.get(name)).toFile();
		System.out.println("loading " + f);
		try  {
			return new StringLookup(f);
		} catch (Exception e) {
			System.err.println("Error loading " + f);
			e.printStackTrace();
			return null;
		}
	}
}
