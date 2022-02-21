package com.ibm.lozperf.mb.batching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class CPUTopology {

	public static BitSet[] getSockets() {
		File cpu_dir = new File("/sys/devices/system/cpu");
		File[] cpu_list = cpu_dir.listFiles((d, n) -> n.matches("cpu\\d+"));

		Set<BitSet> socketSet = new HashSet<>();
		for (File cpu : cpu_list) {
			String siblingsStr;
			try (BufferedReader br = new BufferedReader(
					new FileReader(cpu.toPath().resolve("topology").resolve("core_siblings").toFile()))) {
				siblingsStr = br.readLine();
			} catch (IOException e) {
				System.err.println("Can't get CPU Topology");
				e.printStackTrace();
				return null;
			}
			siblingsStr = siblingsStr.replace(",", "");
			BigInteger siblingsInt = new BigInteger(siblingsStr, 16);
			byte[] siblingsBytes = siblingsInt.toByteArray();
			for (int i = 0; i < siblingsBytes.length / 2; i++) {
				byte tmp = siblingsBytes[i];
				siblingsBytes[i] = siblingsBytes[siblingsBytes.length - 1 - i];
				siblingsBytes[siblingsBytes.length - 1 - i] = tmp;
			}
			BitSet siblings = BitSet.valueOf(siblingsBytes);

			socketSet.add(siblings);
		}
		BitSet[] socketList = new BitSet[socketSet.size()];
		socketList = socketSet.toArray(socketList);
		Arrays.sort(socketList, (a, b) -> b.cardinality() - a.cardinality());
		System.out.println("CPU Sockets: " + Arrays.toString(socketList));
		return socketList;
	}
}
