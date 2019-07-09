package br.com.wps.smartnet.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;

import br.com.wps.smartnet.core.ClientConfiguration;
import br.com.wps.smartnet.core.EnumConnectionType;
import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.core.InstanceSequence;
import br.com.wps.smartnet.exception.ConnectorError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.exception.SendError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.valueobject.IPAddress;

final class SSLConnector extends SmartThread implements AutoCloseable {

	public static final int DEFAULT_CONNECTION_ATTEMPT = 3; // 3 tentativas de conexão
	public static final int DEFAULT_CONNECTION_TIMEOUT = 60; // 1 minuto
	public static final int DEFAULT_AUTOCONNECT_INTERVAL = 30; // 30 segundos
	public static final int DEFAULT_WAIT_TIMEOUT = 30; // 30 segundos
	
	private IPAddress serverAddress;
	private SSLSmartClient refClient;
	private SSLClientEventFactory refEventFactory;
	private SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList;
	private SSLSession refSession;

	private Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass;
	private Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass;

	private long instanceNumber;

	private SmartLog logger;
	
	private AtomicBoolean online;
	private AtomicReference<DateTime> startedDate;
	private AtomicReference<DateTime> endedDate;
	
	private Object object;

	public SSLConnector(String id, SSLSmartClient refClient, IPAddress serverAddress, SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList, SSLClientEventFactory refEventFactory, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) throws IOException, InvalidParameterError, ConnectorError {

		super(id);

		instanceNumber = InstanceSequence.nextValue();
		
		logger = new SmartLog(this);
		
		object = new Object();
		
		/********************************************
		 * Valida parâmetros
		 ********************************************/
		if (serverAddress == null) {
			logger.error("Server address not set on constructor");
			throw new InvalidParameterError("Server address not set on constructor.");
		}
		
		if (serverAddress.getHostaddress() == null) {
			logger.error("Hostname not set on constructor");
			throw new InvalidParameterError("Client not set on constructor.");
		}
		
		if (!(serverAddress.getPort() >= 0x00 && serverAddress.getPort() <= 0x0000FFFF)) {
			logger.error("Port %d is invalid", serverAddress.getPort());
			throw new InvalidParameterError(String.format("Port %d is invalid.", serverAddress.getPort()));
		}
		
		if (refClient == null) {
			logger.error("Client not set on constructor");
			throw new InvalidParameterError("Client not set on constructor.");
		}
		
		if (refMultiplexerList == null) {
			logger.error("List of multiplexers not set on constructor");
			throw new InvalidParameterError("List of multiplexers not set on constructor.");
		}

		if (refEventFactory == null) {
			logger.error("Event factory not set on constructor");
			throw new InvalidParameterError("Event factory not set on constructor.");
		}

		if (decoderClass == null) {
			logger.error("Decoder class not set on constructor");
			throw new InvalidParameterError("Decoder class not set on constructor.");
		}

		if (encoderClass == null) {
			logger.error("Encoder class not set on constructor");
			throw new InvalidParameterError("Encoder class not set on constructor.");
		}

		this.serverAddress = serverAddress;
		this.decoderClass = decoderClass;
		this.encoderClass = encoderClass;
		this.refClient = refClient;
		this.refEventFactory = refEventFactory;
		this.refMultiplexerList = refMultiplexerList;

		this.online = new AtomicBoolean(Boolean.FALSE);
		this.startedDate = new AtomicReference<DateTime>();
		this.endedDate = new AtomicReference<DateTime>();
		
		this.connect(serverAddress);

	}	

	public boolean isOnline() {
		return this.online.get();
	}
	
	@SuppressWarnings("unchecked")
	private void connect(IPAddress serverAddress) throws IOException, ConnectorError {

		synchronized(object) {

			if (this.online.get()) {
				throw new ConnectorError("Already connected");
			}

			/********************************
			 * Cria uma conexão socket
			 ********************************/
			long connectionTimeout = 0L; 
			boolean registred = false;
			SSLSession session = null;
	
			try {
	
				SocketAddress remoteAddress = null;
				SocketChannel socketChannel = null;
	
				int maxAttempt = 0;
				int currentAttempt = 0;
				
				if (this.refClient != null && this.refClient.getConfiguration() != null && this.refClient.getConfiguration().getConnectAttempt() > 0) {
					maxAttempt = refClient.getConfiguration().getConnectAttempt();
				} else {
					maxAttempt = DEFAULT_CONNECTION_ATTEMPT;
				}
				
				while (currentAttempt < maxAttempt) {
				
					currentAttempt++;
	
					try {
					
						remoteAddress = new InetSocketAddress(serverAddress.getHostaddress(), serverAddress.getPort());
						socketChannel = SocketChannel.open();
						//socketChannel.configureBlocking(false);
		
						if (currentAttempt == 1) {
							logger.trace(serverAddress, "Connecting a new socket");
						} else {
							logger.trace(serverAddress, "Connecting a new socket. Attempt %d of %d", currentAttempt, maxAttempt);
						}
					
						socketChannel.connect(remoteAddress);
			
						if (this.refClient != null && this.refClient.getConfiguration() != null && this.refClient.getConfiguration().getWaitTimeout() > 0) {
							connectionTimeout = System.currentTimeMillis() + this.refClient.getConfiguration().getWaitTimeout() * 1000L;
						} else {
							connectionTimeout = System.currentTimeMillis() + DEFAULT_CONNECTION_TIMEOUT * 1000L;
						}
						
						// Aguarda estabelecimento da conexão 
						while(!socketChannel.finishConnect()) {
		
							if (connectionTimeout < System.currentTimeMillis()) {
								throw new ConnectorError("Connection time outed");
							}
	
							Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
	
						}
	
						// Estabeleceu uma conexão com sucesso
						break;
						
					} catch (Exception e) {
						
						try {
							if (socketChannel != null) {
								socketChannel.close();
							}
						} catch(IOException e1) {
							socketChannel = null;
						} finally {
							if (socketChannel != null) {
								socketChannel.close();
							}
						}
						
						if (currentAttempt < maxAttempt) {
							logger.error(serverAddress, "Error while conecting to server. Error: %s", ExceptionUtils.rootCauseMessage(e));
						} else {
							throw new ConnectorError(String.format("Exceeded attempts to connect on %s:%d.", serverAddress.getHostaddress(), serverAddress.getPort()), e);
						}
	
					} catch (Throwable e) {
	
						try {
							if (socketChannel != null) {
								socketChannel.close();
							}
						} catch(IOException e1) {
							socketChannel = null;
						} finally {
							if (socketChannel != null) {
								socketChannel.close();
							}
						}
	
						if (currentAttempt < maxAttempt) {
							logger.error(serverAddress, "Error while conecting to server. Error: %s", ExceptionUtils.rootCauseMessage(e));
						} else {
							throw new ConnectorError(String.format("Exceeded attempts to connect on %s:%d.", serverAddress.getHostaddress(), serverAddress.getPort()), e);
						}
	
					}
	
					Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
					
				}
				
				/********************************
				 * Monta uma sessão para a nova conexão
				 ********************************/
				SSLBasicSocket basicSocket = new SSLBasicSocket(socketChannel, this.refClient.getContext());
				basicSocket.setConnectionType(EnumConnectionType.ActiveSocket);

				SSLSender sender = new SSLSender(refMultiplexerList);
				session = new SSLSession(refClient.getConfiguration(), basicSocket, sender);
				
				basicSocket.addOnOpenedEvent(new Runnable() {
					
					@Override
					public void run() {

						if (online != null) {
							online.set(Boolean.TRUE);
						}
						
						logger.debug("Opened Event");
						
						if (startedDate != null) {
							startedDate.set(DateTime.now());
						}

						if (endedDate != null) {
							endedDate.set(null);
						}
						
					}

				});

				basicSocket.addOnClosingEvent(new Runnable() {
					
					@Override
					public void run() {

						if (online != null) {
							online.set(Boolean.FALSE);
						}

						logger.debug("Closing Event");
						
						if (endedDate != null) {
							endedDate.set(DateTime.now());
						}
						
					}

				});
	
				SSLMessageDecoder<SSLMessage> decoder = null;
				SSLMessageEncoder<SSLMessage> encoder = null;
				
				// Determina decodificador/codificador de mensagens
				try {
					
					decoder = (SSLMessageDecoder<SSLMessage>) decoderClass.newInstance();
					encoder = (SSLMessageEncoder<SSLMessage>) encoderClass.newInstance();
		
					decoder.setRefSession(session);
					encoder.setRefSession(session);
	
					basicSocket.setDecoder(decoder);
					basicSocket.setEncoder(encoder);
					
				} catch (ClassCastException | InstantiationException | IllegalAccessException e) {
		
					throw new ConnectorError(String.format("An error occured during create codecs for a new connection %s. The connection will be discarted. Error: %s", SSLBasicSocket.extractAddress(socketChannel), ExceptionUtils.rootCauseMessage(e)));
		
				}
	
				logger.trace(session, "Connecting a new socket");
				
				ClientConfiguration refClientConfiguration = refClient.getConfiguration();
				
				// Procura pelo multiplexador correspondente a interface de rede utilizada pela conexão
				logger.trace(session, "Searching for related multiplexer");

				SSLMultiplexer multiplexer = SSLMultiplexer.searchForRelatedMultiplexer(socketChannel.socket().getLocalAddress(), refMultiplexerList);
		
				// Caso  não exista um multiplexer correspondente
				// criar um multiplexer para conexÃµes órfãos
				// Obs.: Se existir uma atualização de endereços sem a
				// 		 reinicilização do serviço, esta alternativa
				//		 evita a rejeição de conexÃµes
				if (multiplexer == null) {
	
					try {
						multiplexer = SSLMultiplexer.createMultiplexer(SSLMultiplexer.FOR_UNKNOWN_SOCKET, refClientConfiguration, decoderClass, encoderClass, refMultiplexerList, this.refEventFactory);
					} catch (MultiplexerError e) {
						try {
							session.close();
						} catch (Exception e1) {
							session = null;
						}
						throw new ConnectorError(String.format("Error while creating a new mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
					}
	
				}
	
				// Verifica se encontrou um multiplexer correspondente 
				// A partir do registro em multiplexer é possível lanÃ§ar evento de conexão rejeitada ou erro de conexão
				// pois o monitoramento do multiplexer fará o encerramento do objeto de conexão após alguns segundos
				if (multiplexer != null) {
	
					try {
	
						// Registra a sessão socket no multiplexer encontrado
						multiplexer.register(session, refMultiplexerList);
						
						// Faz o handshake caso necessário
						if (!basicSocket.isHandshakeFinished()) {

							// Quando o handshake está pendente, a mensagem não chega ao servidor, somente faz iniciar o handshake
							multiplexer.send(socketChannel, ByteBuffer.wrap(SSLMultiplexer.DUMMY_BUFFER));
							
							if (this.refClient != null && this.refClient.getConfiguration() != null && this.refClient.getConfiguration().getWaitTimeout() > 0) {
								connectionTimeout = System.currentTimeMillis() + this.refClient.getConfiguration().getWaitTimeout() * 1000L;
							} else {
								connectionTimeout = System.currentTimeMillis() + DEFAULT_CONNECTION_TIMEOUT * 1000L;
							}

							logger.trace(session, "Wait for handshake");

							// Aguarda estabelecimento da conexão 
							while(!basicSocket.isHandshakeFinished()) {
			
								if (connectionTimeout < System.currentTimeMillis()) {
									throw new ConnectorError("Handshake time outed");
								}

								Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
		
							}							

						}				

						registred = true;
						
						refEventFactory._connected(session);

						this.refSession = session;
	
					} catch (Exception e) {
	
						try {
							if (session != null) {
								session.close();	
							}
						} catch (Exception e1) {
							session = null;
						}
	
						throw new ConnectorError(String.format("Error while registering new socket on mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
	
					} catch (Throwable e) {
	
						try {
							if (session != null) {
								session.close();
							}
						} catch (Exception e1) {
							session = null;
						}
	
						throw new ConnectorError(String.format("Error while registering new socket on mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
	
					}					
					
				} else {
	
					throw new ConnectorError("Multiplex not found to register new socket.");				
	
				}			
				
			} catch (Exception e) {
	
				if (registred) {
					refEventFactory._rejectedConnection(session, EnumDisconnectionReason.ConnectionError);
					session.getBasicSocket().prepareToDisconnect(EnumDisconnectionReason.ConnectionError);
				} else {
					if (session != null) {
						try {
							session.close();
						} catch (Exception e1) {
							logger.error("Error while destroying objects of connector. Error: %s", ExceptionUtils.rootCauseMessage(e1));
						}
					}				
				}
				logger.error("Error while opening socket channel: %s", ExceptionUtils.rootCauseMessage(e));
				throw new ConnectorError(String.format("Error while opening socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
	
			} catch (Throwable e) {
	
				if (registred) {
					refEventFactory._rejectedConnection(session, EnumDisconnectionReason.ConnectionError);
					session.getBasicSocket().prepareToDisconnect(EnumDisconnectionReason.ConnectionError);
				} else {
					if (session != null) {
						try {
							session.close();
						} catch (Exception e1) {
							logger.error("Error while destroying objects of connector. Error: %s", ExceptionUtils.rootCauseMessage(e1));
						}
					}				
				}
				logger.error("Error while opening socket channel: %s", ExceptionUtils.rootCauseMessage(e));
				throw new ConnectorError(String.format("Error while opening socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
	
			}
			
		}
			
	}

	public long getInstanceNumber() {
		return instanceNumber;
	}

	@Override
	public void close() throws Exception {

		this.stopWork();
		
	}

	@Override
	protected void doWorkAndRepeat() throws IOException, Throwable {

		boolean reconnect = false;
		int reconnectInterval = 0;
		
		//  Verifica a conexão foi desfeita e se existe configuração de auto conexão para ser operada 
		if (!this.online.get() && this.refClient != null && this.refClient.getConfiguration() != null && this.refClient.getConfiguration().isAutoConnect()) {

			reconnect = true;
			
			if (this.refClient.getConfiguration().getAutoConnectInterval() > 0) {
				reconnectInterval = this.refClient.getConfiguration().getAutoConnectInterval();
			} else {
				reconnectInterval = DEFAULT_AUTOCONNECT_INTERVAL;
			}
			
			if (this.endedDate.get() != null && this.endedDate.get().plusSeconds(reconnectInterval).getMillis() < DateTime.now().getMillis()) {
				reconnect = true;
			}
					
		}
		
		// Monitorar conexão
		if (reconnect) {

			this.connect(this.serverAddress);

		}

	}
	
	public boolean sendAndConfirm(SSLMessage message) throws SendError {

		if (!this.online.get()) {
			throw new SendError("Connection closed");
		}

		boolean result = false;
		
		result = this.refSession.getSender().sendAndConfirm(message);
		
		return result;

	}	
	
	public SSLMessage sendAndReceive(SSLMessage message) throws SendError {

		if (!this.online.get()) {
			throw new SendError("Connection closed");
		}

		SSLMessage result = null;

		int timeout = 0;

		if (this.refClient.getConfiguration() != null && this.refClient.getConfiguration().getWaitTimeout() > 0) {
			timeout = this.refClient.getConfiguration().getWaitTimeout();
		} else {
			timeout = DEFAULT_WAIT_TIMEOUT;
		}

		result = this.refSession.getSender().sendAndReceive(message, timeout);

		return result;

	}	

	public void send(SSLMessage message) throws SendError {

		if (!this.online.get()) {
			throw new SendError("Connection closed");
		}

		this.refSession.getSender().send(message);

	}	

	public void send(SSLMessage message, Runnable successHandler, Runnable failureHandler) throws SendError {

		if (!this.online.get()) {
			throw new SendError("Connection closed");
		}

		int timeout = 0;

		if (this.refClient.getConfiguration() != null && this.refClient.getConfiguration().getWaitTimeout() > 0) {
			timeout = this.refClient.getConfiguration().getWaitTimeout();
		} else {
			timeout = DEFAULT_WAIT_TIMEOUT;
		}

		this.refSession.getSender().send(message, timeout, successHandler, failureHandler);

	}	

	@Override
	protected void startEvent() {

		logger.trace(this.refSession, "Started connector %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void pauseEvent() {

		logger.trace(this.refSession, "Paused connector %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void stopEvent() {

		logger.trace(this.refSession, "Stopped connector %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void resumeEvent() {

		logger.trace(this.refSession, "Resumed connector %d - %s", super.getId(), super.getName());

	}
	
}
