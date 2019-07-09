package br.com.wps.smartnet.exception;

public class InvalidPath extends Exception {

	private static final long serialVersionUID = 3803852102988993351L;

	public InvalidPath(String message) {
		super(message);
	}
	
	public InvalidPath(String message, Throwable cause) {
		super(message, cause);
	}

}
