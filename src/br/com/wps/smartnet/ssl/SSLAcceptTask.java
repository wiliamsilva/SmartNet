package br.com.wps.smartnet.ssl;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import br.com.wps.smartnet.core.BasicSocket;
import br.com.wps.smartnet.core.Configuration;
import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.core.ServerConfiguration;
import br.com.wps.smartnet.exception.AccepterError;
import br.com.wps.smartnet.exception.ConnectorError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.ExceptionUtils;

public class SSLAcceptTask implements Runnable {

	private SmartLog logger;
	private SSLSmartServer refServer;
	private Configuration refServerConfiguration;
	private SSLServerEventFactory refEventFactory;
	private SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList;
	private SelectionKey refKey;
	
	private Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass;
	private Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass;
	
	
	public SSLAcceptTask(SSLSmartServer refServer, SSLServerEventFactory refEventFactory, SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList, SelectionKey refKey, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) {
		logger = new SmartLog(this);
		this.refServer = refServer;
		this.refServerConfiguration = refServer.getConfiguration();
		this.refEventFactory = refEventFactory;
		this.refMultiplexerList = refMultiplexerList;
		this.refKey = refKey;
		this.decoderClass = decoderClass;
		this.encoderClass = encoderClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		try {
			
			boolean registred = false;
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) refKey.channel();
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
			SSLBasicSocket basicSocket = new SSLBasicSocket(socketChannel, this.refServer.getContext());

			SSLSender sender = new SSLSender(refMultiplexerList);
			SSLSession session = new SSLSession(this.refServer.getConfiguration(), basicSocket, sender);

			SSLMessageDecoder<SSLMessage> decoder = null;
			SSLMessageEncoder<SSLMessage> encoder = null;			
			
			try {
			
				// Gera uma instância para o decodificador e outra para o codificador de mensagem
				// baseado no protï¿½tipo informado para o server socket
				try {
		
					decoder = (SSLMessageDecoder<SSLMessage>) decoderClass.newInstance();
					encoder = (SSLMessageEncoder<SSLMessage>) encoderClass.newInstance();
		
					decoder.setRefSession(session);
					encoder.setRefSession(session);

					basicSocket.setDecoder(decoder);
					basicSocket.setEncoder(encoder);
		
				} catch (ClassCastException | InstantiationException | IllegalAccessException e) {
		
					throw new AccepterError(String.format("An error occured during create codecs for a new connection %s. The connection will be discarted. Error: %s", BasicSocket.extractAddress(socketChannel), ExceptionUtils.rootCauseMessage(e)));
		
				}

				logger.trace(session, "Accepting a new socket");

				ServerConfiguration refServerConfiguration = this.refServer.getConfiguration();
				
				/******************************************
				 * Procura o multiplexer correspondente ao
				 * endereão de conexão
				 ******************************************/
		
				logger.trace(session, "Searching for related multiplexer");
				SSLMultiplexer multiplexer = SSLMultiplexer.searchForRelatedMultiplexer(socketChannel.socket().getLocalAddress(), refMultiplexerList);
		
				// Caso não exista um multiplexer correspondente
				// criar um multiplexer para conexï¿½es ï¿½rfãos
				// Obs.: Se existir uma atualização de endereãos sem a
				// 		 reinicilização do serviço, esta alternativa
				//		 evita a rejeição de conexï¿½es
				if (multiplexer == null) {
		
					try {
						multiplexer = SSLMultiplexer.createMultiplexer(SSLMultiplexer.FOR_UNKNOWN_SOCKET, refServerConfiguration, decoderClass, encoderClass, refMultiplexerList, refEventFactory);
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
		catch (Exception e) {
			logger.error("Accepter error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		catch (Throwable e) {
			logger.fatal("Accepter error: %s", ExceptionUtils.rootCauseMessage(e));
		}			
		
	}

}
