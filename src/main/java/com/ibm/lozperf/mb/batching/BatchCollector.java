package com.ibm.lozperf.mb.batching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import net.openhft.affinity.impl.LinuxHelper;

public class BatchCollector<E> implements AutoCloseable {

	final static int nPredictThreads = Integer.parseInt(System.getenv("PREDICT_THREADS"));
	final static boolean PROFILE = Boolean.parseBoolean(System.getenv("PROFILE"));
	final static int TARGET_BS = Integer.parseInt(System.getenv("TARGET_BS"));
	final static int BATCH_TIMEOUT = Integer.parseInt(System.getenv("TARGET_BS"));
	final static boolean PIN_PREDICT_THREADS = Boolean.parseBoolean(System.getenv("PIN_PREDICT_THREADS"));

	private ArrayList<Job<E>> jobList = new ArrayList<>();
	private Object lock = new ReentrantLock(true);
	private boolean shutdown = false;
	private Consumer<List<Job<E>>> predictCallback;
	private Thread[] predictThreads = new Thread[nPredictThreads];
	private long waitSince = Long.MAX_VALUE;

	final static BitSet[] CPU_SOCKETS = CPUTopology.getSockets();

	public BatchCollector(Consumer<List<Job<E>>> predictCallback) {
		this.predictCallback = predictCallback;
		for (int i = 0; i < predictThreads.length; i++) {
			predictThreads[i] = new PredictThread(i);
			predictThreads[i].start();
		}
	}

	public boolean predict(E inputs) {
		long insTime = System.currentTimeMillis();
		Job<E> job = new Job<E>(inputs);
		synchronized (lock) {
			jobList.add(job);
			if (insTime < waitSince)
				waitSince = insTime;
			if (jobList.size() == TARGET_BS)
				lock.notify();
		}
		return job.getResult();
	}

	private List<Job<E>> removeBatch() throws InterruptedException {
		ArrayList<Job<E>> newJobList = new ArrayList<>(jobList.size() * 2);
		synchronized (lock) {
			long wt = BATCH_TIMEOUT;
			while (jobList.size() == 0 || (jobList.size() < TARGET_BS
					&& (wt = waitSince + BATCH_TIMEOUT - System.currentTimeMillis()) > 0)) {
				if(shutdown)
					throw new InterruptedException();
				lock.wait(wt);
			}
			List<Job<E>> oldJobList = jobList;
			jobList = newJobList;
			waitSince = Long.MAX_VALUE;
			return oldJobList;
		}
	}

	private final static int SAMPSIZE = Integer.parseInt(System.getenv("SAMPSIZE"));
	private final static int PRESAMP = Integer.parseInt(System.getenv("PRESAMP"));

	private class PredictThread extends Thread {
		private long nBatches = 0;
		private long nElements = 0;
		private long totPredictTime = 0;
		private final int thNum;

		private short[] bs_buff = new short[SAMPSIZE];
		private short[] lat_buff = new short[SAMPSIZE];
		private int sampCount;
		private int batchCount;

		public PredictThread(int thNum) {
			this.thNum = thNum;
			setPriority(10);
			setDaemon(true);
		}

		@Override
		public void run() {
			Timer timer = null;
			if (PROFILE)
				timer = new Timer();
			if(PIN_PREDICT_THREADS) {
				BitSet socket = CPU_SOCKETS[thNum % CPU_SOCKETS.length];
				System.out.println("Pin PredictThread " + thNum + " to CPUs " + socket);
				LinuxHelper.sched_setaffinity(socket);
			}
			
			while (!shutdown) {
				try {
					batchCount++;
					List<Job<E>> batch = removeBatch();
					int bs = batch.size();
					long start = System.currentTimeMillis();
					;
					boolean sample = false;
					StackDumpTask stackDump = null;
					if (PROFILE) {
						if (batchCount > PRESAMP && sampCount < SAMPSIZE) {
							if (sampCount == 0)
								System.out.println("Start Profile");
							sample = true;
							stackDump = new StackDumpTask();
							timer.schedule(stackDump, 30);
							stackDump.start = start;
						}
					}
					predictCallback.accept(batch);
					long end = System.currentTimeMillis();
					if (sample) {
						if (stackDump != null) {
							stackDump.cancel();
						}

						lat_buff[sampCount] = (short) (end - start);
						bs_buff[sampCount] = (short) bs;
						sampCount++;
						if (sampCount == SAMPSIZE) {
							System.out.println("Stop Profile");
							try (BufferedWriter br = new BufferedWriter(
									new FileWriter("/tmp/predict" + thNum + ".profile"))) {
								br.write(Arrays.toString(bs_buff));
								br.write("\n");
								br.write(Arrays.toString(lat_buff));
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}

					nBatches++;
					nElements += bs;
					totPredictTime += end - start;
					if (nElements % 4000 == 0) {
						System.out.println(thNum + ": Avg Batch Size: " + ((float) nElements / nBatches)
								+ " Time per Batch: " + ((float) totPredictTime / nBatches) + " per Element: "
								+ ((float) totPredictTime / nElements));
						nBatches = 0;
						nElements = 0;
						totPredictTime = 0;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private class StackDumpTask extends TimerTask {

			long start;

			@Override
			public void run() {
				long now = System.currentTimeMillis();
				State state = getState();
				StackTraceElement[] stackTrace = getStackTrace();
				StringBuilder dump = new StringBuilder("Predict Thread ");
				dump.append(thNum);
				dump.append(": ");
				dump.append(state);
				dump.append(" after ");
				dump.append(now - start);
				dump.append("ms");
				for (final StackTraceElement stackTraceElement : stackTrace) {
					dump.append("\n        at ");
					dump.append(stackTraceElement);
				}
				dump.append("\n\n");
				System.out.println(dump.toString());
			}
		}
	}

	@Override
	public void close() throws Exception {
		System.out.println("close BatchCollector");
		shutdown = true;
		for (Thread predictThread : predictThreads)
			predictThread.interrupt();
	}

}
