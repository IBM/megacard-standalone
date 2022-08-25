package com.ibm.lozperf.mb.modeladapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class StringLookup {
	
	private Map<String, Integer> map;
	private int unk;
	
	public StringLookup(File f) throws FileNotFoundException, IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			map = br.lines().map(s -> s.split("\\|"))
					.collect(Collectors.toMap(a -> a[0], a -> Integer.parseInt(a[1])));
		} 
		unk = map.get("[UNK]");
	}
	
	public int lookup(String str) {
		return map.getOrDefault(str, unk);
	}

	public void lookup(String[] inp, long[] target, int targetbase) {
		for (int j = 0; j < inp.length; j++) {
			target[targetbase + j] = map.getOrDefault(inp[j], unk);
		}
	}
}
