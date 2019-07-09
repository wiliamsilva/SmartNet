package br.com.wps.smartnet.core;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.exception.EncoderError;

public abstract class MessageEncoder<M extends Message> {

	private Session refSession;
	
	public abstract ByteBuffer encode(M message) throws EncoderError;

	public abstract void close();
	
	public void setRefSession(Session session) {
		this.refSession = session;
	}

	public Session getRefSession() {
		return this.refSession;
	}
	
}
