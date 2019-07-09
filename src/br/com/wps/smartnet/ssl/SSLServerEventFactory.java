package br.com.wps.smartnet.ssl;

import br.com.wps.smartnetutils.ext.GeneralExecutor;

public abstract class SSLServerEventFactory extends SSLEventFactory {

	void _acceptedConnection(SSLSession session) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				acceptedConnection(session);
			}
		};
		
		GeneralExecutor.execute(task);
		
	}

	public abstract void acceptedConnection(SSLSession session);
	
}
