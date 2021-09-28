/**
 * 
 */
package com.ibm.lozperf.mb;

/**
 * @author Rob Maiolini
 *
 */
public class MegaBankException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6000L;
	/**
	 * @param Error message
	 */
	public MegaBankException(String message){
		super(message);
	}
	/**
	 * @param causing exception
	 */
	public MegaBankException(Throwable cause){
		super(cause);
	}
	/**
	 * @param Error message
	 * @param causing exception
	 */
	public MegaBankException(String message, Throwable cause){
		super(message,cause);
	}
	/**
	 * 
	 */
	public MegaBankException(){
	
	}

}
