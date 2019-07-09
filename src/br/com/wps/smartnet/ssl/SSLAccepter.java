package br.com.wps.smartnet.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import br.com.wps.smartnet.core.InstanceSequence;
import br.com.wps.smartnet.exception.AccepterError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.GeneralExecutor;
import br.com.wps.smartnetutils.ext.SmartThread;

final class SSLAccepter extends SmartThread implements AutoCloseable {

	private Selector selector;
	private SelectionKey serverKey;
	private ServerSocketChannel serverSocketChannel;
	
	private SSLSmartServer refServer;
	
	private SSLServerEventFactory refEventFactory;
	private SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList;
	
	private Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass;
	private Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass;
	
	private long instanceNumber;
	private SmartLog logger;
	
	private boolean started;
	
	public SSLAccepter(String id, SSLSmartServer refServer, String hostname, int port, SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList, SSLServerEventFactory refEventFactory, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) throws IOException, InvalidParameterError, AccepterError {

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
		this.refEventFactory = refEventFactory;
		this.refMultiplexerList = refMultiplexerList;

		started = true;

	}	
	
	public SSLAccepter(String id, SSLSmartServer refServer, int port, SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList, SSLServerEventFactory refEventFactory, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) throws IOException, InvalidParameterError, AccepterError {

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

	private void accept(SelectionKey key)  {

		SSLAcceptTask sslAcceptTask = new SSLAcceptTask(refServer, refEventFactory, refMultiplexerList, key, decoderClass, encoderClass);
		
		GeneralExecutor.execute(sslAcceptTask);

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
