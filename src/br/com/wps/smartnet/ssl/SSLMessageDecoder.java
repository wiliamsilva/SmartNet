package br.com.wps.smartnet.ssl;

import br.com.wps.smartnet.core.SmartBuffer;
import br.com.wps.smartnet.exception.DecoderError;

public abstract class SSLMessageDecoder<M extends SSLMessage> {

	private SSLSession refSession;
	
	public SSLMessageDecoder() {
		refSession = null;
	}
	
	public abstract M decode(SmartBuffer buffer) throws DecoderError;
	
	public abstract void close();

	public void setRefSession(SSLSession session) {
		this.refSession = session;
	}

	public SSLSession getRefSession() {
		return this.refSession;
	}
	
}
