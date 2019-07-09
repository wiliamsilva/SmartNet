package br.com.wps.smartnet.core;

import br.com.wps.smartnetutils.ext.GeneralExecutor;

public abstract class ClientEventFactory extends EventFactory {

	public ClientEventFactory() {
		super();
	}

	void _connected(Session session) {

		Runnable task = new Runnable() {

			@Override
			public void run() {
				connected(session);
			}

		};
		
		GeneralExecutor.execute(task);
		
	}
	
	public abstract void connected(Session session);
	
}
