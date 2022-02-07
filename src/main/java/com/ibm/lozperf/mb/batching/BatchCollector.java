package com.ibm.lozperf.mb.batching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class BatchCollector<E> implements AutoCloseable {

	private final static int nPredictThreads = Integer.parseInt(System.getenv("PREDICT_THREADS"));
	private final static boolean PROFILE = Boolean.parseBoolean(System.getenv("PROFILE"));

	private ArrayList<Job<E>> jobList = new ArrayList<>();
	private Object lock = new Object();
	private boolean shutdown = false;
	private Consumer<List<Job<E>>> predictCallback;
	private Thread[] predictThreads = new Thread[nPredictThreads];

	public BatchCollector(Consumer<List<Job<E>>> predictCallback) {
		this.predictCallback = predictCallback;
		for (int i = 0; i < predictThreads.length; i++) {
			predictThreads[i] = new PredictThread(i);
			predictThreads[i].start();
		}
	}

	public boolean predict(E inputs) {
		Job<E> job = new Job<E>(inputs);
		synchronized (lock) {
			jobList.add(job);
			lock.notify();
		}
		return job.getResult();
	}

	public List<Job<E>> removeBatch() throws InterruptedException {
		List<Job<E>> oldJobList;
		synchronized (lock) {
			while (jobList.size() == 0) {
				lock.wait();
			}
			oldJobList = jobList;
			jobList = new ArrayList<>(jobList.size());
		}
		return oldJobList;
	}

	private class PredictThread extends Thread {
		private long nBatches = 0;
		private long nElements = 0;
		private final int thNum;

		private final static int SAMPSIZE = 30000;
		private short[] bs_buff = new short[SAMPSIZE];
		private short[] lat_buff = new short[SAMPSIZE];
		private int sampCount;
		private int preSamp = 100000;

		public PredictThread(int thNum) {
			this.thNum = thNum;
			setPriority(10);
		}

		@Override
		public void run() {
			Timer timer = new Timer();
			while (!shutdown) {
				try {
					List<Job<E>> batch = removeBatch();
					int bs = batch.size();
					long start = 0;
					boolean sample = false;
					StackDumpTask stackDump = null;
					if (PROFILE) {
						if (preSamp == 0 && sampCount < SAMPSIZE) {
							if (preSamp == 0 && sampCount == 0)
								System.out.println("Start Profile");
							sample = true;
							stackDump = new StackDumpTask();
							timer.schedule(stackDump, 30);
							start = System.currentTimeMillis();
							stackDump.start = start;
						} else
							preSamp--;
					}
					predictCallback.accept(batch);
					if (sample) {
						long end = System.currentTimeMillis();
						if(stackDump!=null) {
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
					if (nElements % 4000 == 0) {
						System.out.println(thNum + ": Avg Batch Size: " + ((float) nElements / nBatches));
						nBatches = 0;
						nElements = 0;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		private class StackDumpTask extends TimerTask{
			
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
				dump.append(now-start);
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
		shutdown = true;
		for (Thread predictThread : predictThreads)
			predictThread.interrupt();
	}

}
