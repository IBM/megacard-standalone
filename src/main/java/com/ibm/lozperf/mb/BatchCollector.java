package com.ibm.lozperf.mb;

import java.util.ArrayList;
import java.util.List;

public class BatchCollector<E> {

	public class Job {
		private volatile E input;
		private boolean result;
		private boolean finished = false;

		private Job(E input) {
			this.input = input;
		}

		public E getInput() {
			return input;
		}

		public synchronized boolean getResult() {
			while (!isFinished()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return result;
		}

		public synchronized void setResult(boolean result) {
			this.result = result;
			this.finished = true;
			notifyAll();
		}

		public boolean isFinished() {
			return finished;
		}

	}

	private ArrayList<Job> jobList = new ArrayList<>();
	private Object lock = new Object();
	private long nBatches = 0;
	private long nElements = 0;

	public boolean predict(E inputs) {
		Job job = new Job(inputs);
		synchronized (lock) {
			jobList.add(job);
			lock.notify();
		}		
		return job.getResult();
	}

	public List<Job> removeBatch() throws InterruptedException {
		List<Job> oldJobList;
		synchronized (lock) {
			while (jobList.size() == 0) {
				lock.wait();
			}
			oldJobList = jobList;
			jobList = new ArrayList<>(jobList.size());
		}
		nBatches++;
		nElements+= oldJobList.size();
		if(nBatches % 1000 ==0) {
			System.out.println("Avg Batch Size: " + (nElements/nBatches));
		}
		return oldJobList;
	}

}
