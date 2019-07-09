package br.com.wps.smartnet.ssl;

import br.com.wps.smartnetutils.ext.GeneralExecutor;

public abstract class SSLClientEventFactory extends SSLEventFactory {

	public SSLClientEventFactory() {
		super();
	}

	void _connected(SSLSession session) {

		Runnable task = new Runnable() {

			@Override
			public void run() {
				connected(session);
			}

		};
		
		GeneralExecutor.execute(task);
		
	}
	
	public abstract void connected(SSLSession session);
	
}
