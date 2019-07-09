package br.com.wps.smartnet.exception;

public class PathCreateError extends Exception {

	private static final long serialVersionUID = 5240955278826376484L;

	public PathCreateError(String message) {
		super(message);
	}
	
	public PathCreateError(String message, Throwable cause) {
		super(message, cause);
	}

}
