package br.com.wps.smartnet.exception;

public class DecoderError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public DecoderError(String message) {
		super(message);
	}
	
	public DecoderError(String message, Throwable cause) {
		super(message, cause);
	}

}
