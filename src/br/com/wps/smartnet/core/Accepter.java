package br.com.wps.smartnet.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import br.com.wps.smartnet.exception.AccepterError;
import br.com.wps.smartnet.exception.ConnectorError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;

final class Accepter extends SmartThread implements AutoCloseable {

	private Selector selector;
	private SelectionKey serverKey;
	private ServerSocketChannel serverSocketChannel;
	
	private SmartServer refServer;
	private Configuration refServerConfiguration;
	
	private ServerEventFactory refEventFactory;
	private SmartConcurrentHashMap<String, Multiplexer> refMultiplexerList;
	
	private Class<? extends MessageDecoder<? extends Message>> decoderClass;
	private Class<? extends MessageEncoder<? extends Message>> encoderClass;
	
	private long instanceNumber;
	private SmartLog logger;
	
	private boolean started;
	
	public Accepter(String id, SmartServer refServer, String hostname, int port, SmartConcurrentHashMap<String, Multiplexer> refMultiplexerList, ServerEventFactory refEventFactory, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass) throws IOException, InvalidParameterError, AccepterError {

		super(id);

		instanceNumber = InstanceSequence.nextValue();
		
		logger = new SmartLog(this);
		
		started = false;

		/********************************************
		 * Valida parâmetros
		 ********************************************/
		if (!(port >= 0x00 && port <= 0x0000FFFF)) {
			logger.error("Port %d is invalid", port);
			destroyObjects();
			throw new InvalidParameterError(String.format("Port %d is invalid.", port));
		}
		
		if (refServer == null) {
			logger.error("Server not set on constructor");
			destroyObjects();
			throw new InvalidParameterError("Server not set on constructor.");
		}
		
		if (refMultiplexerList == null) {
			logger.error("List of multiplexers not set on constructor");
			destroyObjects();
			throw new InvalidParameterError("List of multiplexers not set on constructor.");
		}

		if (refEventFactory == null) {
			logger.error("Event factory not set on constructor");
			destroyObjects();
			throw new InvalidParameterError("Event factory not set on constructor.");
		}

		if (decoderClass == null) {
			logger.error("Decoder class not set on constructor");
			destroyObjects();
			throw new InvalidParameterError("Decoder class not set on constructor.");
		}

		if (encoderClass == null) {
			logger.error("Encoder class not set on constructor");
			destroyObjects();
			throw new InvalidParameterError("Encoder class not set on constructor.");
		}

		
		/********************************************
		 * Monta um listening não bloqueante
		 ********************************************/
		try {
			this.selector = SelectorProvider.provider().openSelector();
		} catch (Exception e) {
			logger.error("Error while opening selector: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while opening selector: %s.", ExceptionUtils.rootCauseMessage(e)));
		} catch (Throwable e) {
			logger.fatal("Error while opening selector: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while opening selector: %s.", ExceptionUtils.rootCauseMessage(e)));
		}

		try {
			this.serverSocketChannel = ServerSocketChannel.open();
		} catch (Exception e) {
			logger.error("Error while opening server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while opening server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		} catch (Throwable e) {
			logger.fatal("Error while opening server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while opening server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		}

		this.serverSocketChannel.configureBlocking(false);

		// Vincula o server socket a um endereão e porta
		InetSocketAddress isa = null;

		if (hostname != null) {
			isa = new InetSocketAddress(hostname, port);
		} else {
			isa = new InetSocketAddress(port);
		}

		try {
			this.serverSocketChannel.socket().bind(isa);
		} catch (Exception e) {
			logger.error("Error while binding server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while binding server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		} catch (Throwable e) {
			logger.fatal("Error while binding server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error while binding server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		}

		// Registra o canal server socket e indica interesse em aceitar novas conexï¿½es
		try {
			this.serverKey = serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} catch (Exception e) {
			logger.error("Error on accept key register to server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error on accept key register to server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		} catch (Throwable e) {
			logger.fatal("Error on accept key register to server socket channel: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new AccepterError(String.format("Error on accept key register to server socket channel: %s.", ExceptionUtils.rootCauseMessage(e)));
		}
		
		this.decoderClass = decoderClass;
		this.encoderClass = encoderClass;
		this.refServer = refServer;
		this.refServerConfiguration = refServer.getConfiguration();
		this.refEventFactory = refEventFactory;
		this.refMultiplexerList = refMultiplexerList;

		started = true;

	}	
	
	public Accepter(String id, SmartServer refServer, int port, SmartConcurrentHashMap<String, Multiplexer> refMultiplexerList, ServerEventFactory refEventFactory, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass) throws IOException, InvalidParameterError, AccepterError {

		this(id, refServer, null, port, refMultiplexerList, refEventFactory, decoderClass, encoderClass);

	}


	@Override
	public void doWorkAndRepeat() throws IOException, Throwable {

		// Ignora se o servidor não estiver ligado
		if (!this.started) {
			return;
		}

		try {

			if (this.selector.selectNow() > 0) {

				// Iteração sobre chaves com eventos disponï¿½veis
				Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {

					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					// Dorme rápido
					fastSleep();

					if (key == this.serverKey && key.isValid() && key.isAcceptable()) {
						accept(key);
					}

				}

			}				

		}
		catch (Exception e) {
			logger.error("Accepter error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		catch (Throwable e) {
			logger.fatal("Accepter error: %s", ExceptionUtils.rootCauseMessage(e));
		}

	}

	@SuppressWarnings("unchecked")
	private void accept(SelectionKey key) throws IOException, AccepterError {

		boolean registred = false;
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket;

		if (socketChannel == null) {
			return;
		}		

		socket = socketChannel.socket();
		
		if (refServerConfiguration.isKeepAlive()) {
			socket.setKeepAlive(true);
		}
		
		/********************************
		 * Monta uma sessão para a nova conexão
		 ********************************/
		BasicSocket basicSocket = new BasicSocket(socketChannel);
		MessageDecoder<Message> decoder = null;
		MessageEncoder<Message> encoder = null;

		Sender sender = new Sender(refMultiplexerList);
		Session session = new Session(refServer.getConfiguration(), basicSocket, sender);

		logger.trace(session, "Accepting a new socket");

		try {
		
			// Gera uma instância para o decodificador e outra para o codificador de mensagem
			// baseado no protï¿½tipo informado para o server socket
			try {
	
				decoder = (MessageDecoder<Message>) decoderClass.newInstance();
				encoder = (MessageEncoder<Message>) encoderClass.newInstance();
	
				decoder.setRefSession(session);
				encoder.setRefSession(session);

				basicSocket.setDecoder(decoder);
				basicSocket.setEncoder(encoder);
	
			} catch (ClassCastException | InstantiationException | IllegalAccessException e) {
	
				throw new AccepterError(String.format("An error occured during create codecs for a new connection %s. The connection will be discarted. Error: %s", BasicSocket.extractAddress(socketChannel), ExceptionUtils.rootCauseMessage(e)));
	
			}
	
			
			/******************************************
			 * Procura o multiplexer correspondente ao
			 * endereão de conexão
			 ******************************************/
	
			logger.trace(session, "Searching for related multiplexer");
			Multiplexer multiplexer = Multiplexer.searchForRelatedMultiplexer(socketChannel.socket().getLocalAddress(), refMultiplexerList);
	
			// Caso não exista um multiplexer correspondente
			// criar um multiplexer para conexï¿½es ï¿½rfãos
			// Obs.: Se existir uma atualização de endereãos sem a
			// 		 reinicilização do serviço, esta alternativa
			//		 evita a rejeição de conexï¿½es
			if (multiplexer == null) {
	
				ServerConfiguration refServerConfiguration = refServer.getConfiguration();
				
				try {
					multiplexer = Multiplexer.createMultiplexer(Multiplexer.FOR_UNKNOWN_SOCKET, refServerConfiguration, decoderClass, encoderClass, refMultiplexerList, this.refEventFactory);
				} catch (MultiplexerError e) {
					try {
						session.close();
					} catch (Exception e1) {
						session = null;
					}
					logger.error("Error while creating a new mutiplex: %s", ExceptionUtils.rootCauseMessage(e));
					throw new AccepterError(String.format("Error while creating a new mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
				}
	
			}
	
			// Verifica se encontrou um multiplexer correspondente 
			if (multiplexer != null) {
	
				try {
					// Registra a sessão socket no multiplexer encontrado
					multiplexer.register(session, refMultiplexerList);
					registred = true;
				} catch (Exception e) {
					throw new AccepterError(String.format("Error while registering new socket on mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
				} catch (Throwable e) {
					throw new AccepterError(String.format("Error while registering new socket on mutiplex: %s.", ExceptionUtils.rootCauseMessage(e)));
				}					
				
				logger.trace(session, "New socket regitered on mutiplex");
				refEventFactory._acceptedConnection(session);
	
			} else {
	
				throw new ConnectorError("Multiplex not found to register new socket.");				

			}

		} catch (Exception e) {

			if (registred) {
				refEventFactory._rejectedConnection(session, EnumDisconnectionReason.AcceptError);
				session.getBasicSocket().prepareToDisconnect(EnumDisconnectionReason.AcceptError);
			} else {
				if (session != null) {
					try {
						session.close();
					} catch (Exception e1) {
						logger.error("Error while destroying objects of connector. Error: %s", ExceptionUtils.rootCauseMessage(e1));
					}
				}				
			}
			logger.error("Error while accepting new socket: %s", ExceptionUtils.rootCauseMessage(e));
			throw new AccepterError(String.format("Error while accepting new socket: %s", ExceptionUtils.rootCauseMessage(e)));

		} catch (Throwable e) {

			if (registred) {
				refEventFactory._rejectedConnection(session, EnumDisconnectionReason.AcceptError);
				session.getBasicSocket().prepareToDisconnect(EnumDisconnectionReason.AcceptError);
			} else {
				if (session != null) {
					try {
						session.close();
					} catch (Exception e1) {
						logger.error("Error while destroying objects of connector. Error: %s", ExceptionUtils.rootCauseMessage(e1));
					}
				}				
			}
			logger.error("Error while accepting new socket: %s", ExceptionUtils.rootCauseMessage(e));
			throw new AccepterError(String.format("Error while accepting new socket: %s", ExceptionUtils.rootCauseMessage(e)));

		}
			
	}
	
	public long getInstanceNumber() {
		return instanceNumber;
	}

	@Override
	protected void startEvent() {

		logger.trace("Started accepter %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void pauseEvent() {

		logger.trace("Paused accepter %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void stopEvent() {

		logger.trace("Stopped accepter %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void resumeEvent() {

		logger.trace("Resumed accepter %d - %s", super.getId(), super.getName());

	}

	private void destroyObjects() {

		if (serverKey != null) {
			if (serverKey.isValid()) {
				serverKey.cancel();
			}
			serverKey = null;
		}

		if (serverSocketChannel != null) {
			if (serverSocketChannel.isOpen()) {
				try {
					serverSocketChannel.close();
				} catch (Exception e) {
				} catch (Throwable e) {
				}
			}
			serverSocketChannel = null;
		}

		if (selector != null) {
			if (selector.isOpen()) {
				try {
					selector.close();
				} catch (Exception e) {
				} catch (Throwable e) {
				}
			}
			selector = null;
		}

	}

	@Override
	public void close() throws Exception {

		try {

			this.stopWork();

			destroyObjects();

		} catch (Exception e) {
			logger.trace("Error while closing accepter. Error: %s", ExceptionUtils.rootCauseMessage(e));
		} catch (Throwable e) {
			logger.trace("Error while closing accepter. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}

	}	
	
}
