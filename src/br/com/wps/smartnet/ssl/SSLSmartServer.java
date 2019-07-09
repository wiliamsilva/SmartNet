package br.com.wps.smartnet.ssl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;

import br.com.wps.smartnet.core.Configuration;
import br.com.wps.smartnet.core.InstanceSequence;
import br.com.wps.smartnet.core.ServerConfiguration;
import br.com.wps.smartnet.exception.AccepterError;
import br.com.wps.smartnet.exception.BindError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.exception.ServerError;
import br.com.wps.smartnet.exception.UnexpectedError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.device.NetworkCard;
import br.com.wps.smartnetutils.ext.EnumIPAddressType;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.hardware.Hardware;
import br.com.wps.smartnetutils.valueobject.IPAddress;

public class SSLSmartServer extends SmartThread implements AutoCloseable {

	public final static int DEFAULT_CLOSING_TIMEOUT = 10;    
	
	private Set<IPAddress> listeningParameters;
	private SmartConcurrentHashMap<String, SSLAccepter> accepterList;
	private SmartConcurrentHashMap<String, SSLMultiplexer> multiplexerList;

	private ServerConfiguration configuration;

	private Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass;
	private Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass;	
	private SSLServerEventFactory eventFactory;	

	private long instanceNumber;
	private SmartLog logger;
	
    private SSLContext context;
	
	private boolean started;

	private SSLSmartServer(String id, ServerConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) {

		super(id);
		
		instanceNumber = InstanceSequence.nextValue();
		
		logger = new SmartLog(this);
		
		this.listeningParameters = null;
		this.accepterList = null;
		this.multiplexerList = null;

		this.configuration = configuration;
		
		this.decoderClass = decoderClass;
		this.encoderClass = encoderClass;		
		this.eventFactory = null;
		
		this.context = null;
		
		started = false;
		
	}
	
	public SSLSmartServer(String id, ServerConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass, SSLServerEventFactory eventFactory) throws InvalidParameterError {

		this(id, configuration, decoderClass, encoderClass);

		if (configuration == null) {
			logger.error("Server Configuration class not set on constructor");
			throw new InvalidParameterError("Server Configuration class not set on constructor.");
		}

		if (decoderClass == null) {
			logger.error("Decoder class not set on constructor");
			throw new InvalidParameterError("Decoder class not set on constructor.");
		}

		if (encoderClass == null) {
			logger.error("Encoder class not set on constructor");
			throw new InvalidParameterError("Encoder class not set on constructor.");
		}

		if (eventFactory == null) {
			logger.error("Event factory class not set on constructor");
			throw new InvalidParameterError("Event factory class not set on constructor.");
		}
		
		this.listeningParameters = null;
		this.accepterList = null;
		this.multiplexerList = null;

		this.eventFactory = eventFactory;

		initSSLContext(configuration);
		
	}

	public SSLSmartServer(String id, ServerConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass, SSLServerEventFactory eventFactory, String addressParameters) throws InvalidParameterError, UnexpectedError, BindError, ServerError {

		this(id, configuration, decoderClass, encoderClass, eventFactory);

		bind(addressParameters);

	}

	public void initSSLContext(Configuration configuration) throws InvalidParameterError {

		try {
		
			this.context = SSLContext.getInstance(ServerConfiguration.DEFAULT_PROTOCOL);
			
			context.init(null,null,null);
			SSLContext.setDefault(context);
			
			context.init(SSLBasicSocket.createKeyManagers(configuration.getKeyStoreFile(), configuration.getKeyStorePassword(), configuration.getKeyPassword()), SSLBasicSocket.createTrustManagers(configuration.getTrustStoreFile(), configuration.getTrustStorePassword()), new SecureRandom());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	

	public long getInstanceNumber() {
		return instanceNumber;
	}

	SSLContext getContext() {
		return context;
	}
	
	Class<? extends SSLMessageDecoder<? extends SSLMessage>> getDecoderClass() {
		return decoderClass;
	}


	void setDecoderClass(Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass) {
		this.decoderClass = decoderClass;
	}


	Class<? extends SSLMessageEncoder<? extends SSLMessage>> getEncoderClass() {
		return encoderClass;
	}


	void setEncoderClass(Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) {
		this.encoderClass = encoderClass;
	}
	
	ServerConfiguration getConfiguration() {
		return configuration;
	}

	static String createKeyNameForServer(IPAddress ipAddress) {

		String result = null;
		
		result = String.format("%s:%d", ipAddress.getHostaddress(), ipAddress.getPort());
		
		return result;
		
	}
	
	
	public void bind(String addressParameters) throws InvalidParameterError, UnexpectedError, BindError, ServerError {

		if (started) {
			logger.error("This server already started");
			throw new BindError("This server already started.");
		}

		logger.trace("Binding");

		this.eventFactory._starting();
		
		/**************************************************
		 * DECODIFICA ENDEREï¿½OS
		 **************************************************/
		int numberOfInvalidAddresses = 0;

		if (addressParameters == null || addressParameters.trim().length() == 0) {
			String errorMessage = "Null or empty addresses parameter";
			logger.error("Null or empty addresses parameter");
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError("Null or empty addresses parameter.");
		}

		listeningParameters = Hardware.ipAddressStringToObject(addressParameters);

		if (addressParameters == null || addressParameters.trim().length() == 0) {
			String errorMessage = "Invalid addresses parameter";
			logger.error(errorMessage);
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError(errorMessage);
		}

		StringBuffer out = new StringBuffer();

		for (IPAddress ipAddress : listeningParameters) {

			if (!ipAddress.isValid()) {
				numberOfInvalidAddresses++;
				out.append(ipAddress.toString());
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				String errorMessage = "Thread interrupted";
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new UnexpectedError(String.format(errorMessage));
			}

		}

		if (numberOfInvalidAddresses > 0) {
			String errorMessage = String.format("Addresses parameter contains invalid address.\n%s", out.toString());
			logger.error(errorMessage);
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError(errorMessage);
		}

		/**************************************************
		 * MONTA SERVER SOCKET
		 **************************************************/
		accepterList = new SmartConcurrentHashMap<String, SSLAccepter>("accepterList");
		multiplexerList = new SmartConcurrentHashMap<String, SSLMultiplexer>("multiplexerList");
		
		for (IPAddress ipAddress : listeningParameters) {

			try {

				String key = null;

				SSLAccepter accepter = null;
				
				key = createKeyNameForServer(ipAddress);

				if (ipAddress.getType().getValue() == EnumIPAddressType.All.getValue()) {

					accepter = new SSLAccepter(key, this, ipAddress.getPort(), multiplexerList, eventFactory, decoderClass, encoderClass);

					logger.trace("Created a TCP/IP multiaddress listening to %d port", ipAddress.getPort());

				} else {

					accepter = new SSLAccepter(key, this, ipAddress.getHostaddress(), ipAddress.getPort(), multiplexerList, eventFactory, decoderClass, encoderClass);

					logger.trace("Created a TCP/IP address listening to %s:%d port", ipAddress.getHostaddress(), ipAddress.getPort());

				}

				accepter.start();
				
				accepterList.put(key, accepter);

				List<NetworkCard> listNetworkCards = ipAddress.getRelatedNetworkCards();
				
				SSLMultiplexer.createMultiplexer(configuration, decoderClass, encoderClass, listNetworkCards, multiplexerList, eventFactory);
				
			} catch (IOException e) {
				destroyObjects();
				String errorMessage = String.format("ServerSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ServerError(errorMessage);
			} catch (AccepterError e) {
				destroyObjects();
				String errorMessage = String.format("ServerSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ServerError(errorMessage);
			} catch (MultiplexerError e) {
				destroyObjects();
				String errorMessage = String.format("ServerSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ServerError(errorMessage);
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				destroyObjects();
				String errorMessage = String.format("ServerSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ServerError(errorMessage);
			}

		}

		/**************************************************
		 * Monta Server Sockets
		 **************************************************/
		this.start();
		
		this.eventFactory._started();

		this.started = true;

	}

	private void destroyObjects() {

		if (accepterList != null) {

			SSLAccepter accepter = null;

			Set<String> accepterKeys = accepterList.keySet();
			
			for (String a: accepterKeys) {
				
				accepter = accepterList.get(a);
				
				if (accepter == null) {
					continue;
				}
				
				try {
					accepter.close();
					accepter.join(DEFAULT_CLOSING_TIMEOUT * 1000L);
				} catch (Exception e) {
					logger.error("Close error: %s", ExceptionUtils.rootCauseMessage(e));
				} finally {
					accepter = null;
				}
				
			}

			accepterList.clear();

			accepterList = null;

		}

		if (multiplexerList != null) {

			SSLMultiplexer multiplexer = null;

			Set<String> multiplexerKeys = multiplexerList.keySet();
			
			for (String mp: multiplexerKeys) {
				
				multiplexer = multiplexerList.get(mp);
				
				if (multiplexer == null) {
					continue;
				}
				
				try {
					multiplexer.close();
					multiplexer.join(DEFAULT_CLOSING_TIMEOUT * 1000L);
				} catch (Exception e) {
					logger.error("Close error: %s", ExceptionUtils.rootCauseMessage(e));
				} finally {
					multiplexer = null;
				}
				
			}

			multiplexerList.clear();

			multiplexerList = null;

		}


		if (listeningParameters != null) {

			listeningParameters.clear();

			listeningParameters = null;

		}

		// Não pode destruir por aqui porque podem existir outras instâncias
		//GeneralExecutor.close();

	}

	@Override
	public void close() throws Exception {

		this.stopWork();

		destroyObjects();
		
	}

	@Override
	public void doWorkAndRepeat() throws IOException, Throwable {


	}

	@Override
	public void startEvent() {

		logger.trace("Started server %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void pauseEvent() {

		logger.trace("Paused server %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void stopEvent() {

		logger.trace("Stopped server %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void resumeEvent() {

		logger.trace("Resumed server %d - %s", super.getId(), super.getName());
		
	}

}
