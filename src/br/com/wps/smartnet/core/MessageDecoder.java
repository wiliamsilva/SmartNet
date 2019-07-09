package br.com.wps.smartnet.core;

import br.com.wps.smartnet.exception.DecoderError;

public abstract class MessageDecoder<M extends Message> {

	private Session refSession;
	
	public MessageDecoder() {
		refSession = null;
	}
	
	public abstract M decode(SmartBuffer buffer) throws DecoderError;
	
	public abstract void close();

	public void setRefSession(Session session) {
		this.refSession = session;
	}

	public Session getRefSession() {
		return this.refSession;
	}
	
}
