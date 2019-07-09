package br.com.wps.smartnet.exception;

public class MultiplexerError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public MultiplexerError(String message) {
		super(message);
	}
	
	public MultiplexerError(String message, Throwable cause) {
		super(message, cause);
	}

}
