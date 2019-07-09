package br.com.wps.smartnet.exception;

public class PathNotFound extends Exception {

	private static final long serialVersionUID = -2313358444751551051L;

	public PathNotFound(String message) {
		super(message);
	}
	
	public PathNotFound(String message, Throwable cause) {
		super(message, cause);
	}

}
