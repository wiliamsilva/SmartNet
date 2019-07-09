package br.com.wps.smartnet.core;

import br.com.wps.smartnetutils.ext.GeneralExecutor;

public abstract class ServerEventFactory extends EventFactory {

	void _acceptedConnection(Session session) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				acceptedConnection(session);
			}
		};
		
		GeneralExecutor.execute(task);
		
	}

	public abstract void acceptedConnection(Session session);
	
}
