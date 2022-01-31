package com.ibm.lozperf.mb;

public class Job<E> {
	private volatile E input;
	private boolean result;
	private boolean finished = false;

	public Job(E input) {
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