package br.com.wps.smartnet.ssl;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.exception.EncoderError;

public abstract class SSLMessageEncoder<M extends SSLMessage> {

	private SSLSession refSession;
	
	public abstract ByteBuffer encode(M message) throws EncoderError;

	public abstract void close();
	
	public void setRefSession(SSLSession session) {
		this.refSession = session;
	}

	public SSLSession getRefSession() {
		return this.refSession;
	}
	
}
