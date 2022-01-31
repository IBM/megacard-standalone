package com.ibm.lozperf.mb.batching;

import java.util.ArrayList;
import java.util.List;

public class BatchCollector<E> {

	private ArrayList<Job<E>> jobList = new ArrayList<>();
	private Object lock = new Object();

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

}
