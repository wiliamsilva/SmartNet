package br.com.wps.smartnet.ssl;

import java.io.IOException;

import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.GeneralExecutor;

public abstract class SSLEventFactory {

	private SmartLog logger;
	
	public SSLEventFactory() {
		logger = new SmartLog(this);
	}

	void _starting() {

		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				starting();
			}
		};
		
		GeneralExecutor.execute(task);
		
	};

	void _started() {

		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				started();
			}
		};
		
		GeneralExecutor.execute(task);
		
	}

	void _rejectedConnection(SSLSession session, EnumDisconnectionReason reason) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				rejectedConnection(session, reason);
			}
		};
		
		GeneralExecutor.execute(task);
		
	};

	void _closedConnection(SSLSession session, EnumDisconnectionReason reason) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				closedConnection(session, reason);
			}
		};
		
		GeneralExecutor.execute(task);
		
	};

	void _destroyedConnection(long connId, String address) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				destroyedConnection(connId, address);
			}
		};
		
		GeneralExecutor.execute(task);
		
	};

	void _connectionError(SSLSession session, IOException e) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				connectionError(session, e);
			}
		};
		
		GeneralExecutor.execute(task);
		
		
	};
	
	void _messageReceived(SSLSession session, int bytesReceived, SSLMessage message) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {

				try {
					if (message != null) {
						message.unwrap();
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to unwrap received message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to unwrap received message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				}

				try {
					if (message != null) {
						messageReceived(session, bytesReceived, message);
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to perform received message event. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to perform received message event: %s", ExceptionUtils.rootCauseMessage(e));	
				}
				
				try {
					if (message != null) {
						message.close();
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to close received message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to close received message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				}
				
			}
		};
		
		GeneralExecutor.execute(task);
		
	};

	void _messageSent(SSLSession session, int bytesSent, SSLMessage message) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {

				try {
					if (message != null) {
						message.unwrap();
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to unwrap sent message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to unwrap sent message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				}

				try {
					if (message != null) {
						messageSent(session, bytesSent, message);
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to perform sent message event. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to perform sent message event: %s", ExceptionUtils.rootCauseMessage(e));	
				}
				
				try {
					if (message != null) {
						message.close();
					}
				} catch (Exception e) {
					logger.error(session, "Unexpected failure to close sent message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				} catch (Throwable e) {
					logger.error(session, "Unexpected failure to close sent message. Error: %s", ExceptionUtils.rootCauseMessage(e));	
				}
				
			}
		};
		
		GeneralExecutor.execute(task);

	};

	void _error(IOException e) {
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				error(e);
			}
		};
		
		GeneralExecutor.execute(task);

	};

	void _stoping() { 

		Runnable task = new Runnable() {

			@Override
			public void run() {
				stoping();
			}
		};

		GeneralExecutor.execute(task);

	};

	void _stopped() { 
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				stopped();
			}
		};
		
		GeneralExecutor.execute(task);

	};
	
	public abstract void starting();

	public abstract void started();

	public abstract void rejectedConnection(SSLSession session, EnumDisconnectionReason reason);

	public abstract void closedConnection(SSLSession session, EnumDisconnectionReason reason);

	public abstract void destroyedConnection(long connId, String address);

	public abstract void connectionError(SSLSession session, IOException e);
	
	public abstract void messageReceived(SSLSession session, int bytesReceived, SSLMessage message);

	public abstract void messageSent(SSLSession session, int bytesSent, SSLMessage message);

	public abstract void error(IOException e);

	public abstract void stoping();

	public abstract void stopped();
	
}
