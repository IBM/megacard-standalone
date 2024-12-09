package com.ibm.lozperf.mb.batching;

public class Job<E> {
	private volatile E input;
	private float result;
	private boolean finished = false;

	public Job(E input) {
		this.input = input;
	}

	public E getInput() {
		return input;
	}

	public synchronized float getResult() {
		while (!isFinished()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	public synchronized void setResult(float result) {
		this.result = result;
		this.finished = true;
		notifyAll();
	}

	public boolean isFinished() {
		return finished;
	}

}