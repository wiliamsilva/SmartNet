package br.com.wps.smartnet.test.sslserver;

import java.io.IOException;

import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.exception.SendError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnet.ssl.SSLMessage;
import br.com.wps.smartnet.ssl.SSLSender;
import br.com.wps.smartnet.ssl.SSLServerEventFactory;
import br.com.wps.smartnet.ssl.SSLSession;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.Function;

public class EventFactory extends SSLServerEventFactory {

	private SmartLog logger;

	public EventFactory() {
		logger = new SmartLog(this);
	}

	@Override
	public void starting() {
		
		logger.info("Iniciando o servidor");

	}

	@Override
	public void started() {

		logger.info("Servidor iniciado com sucesso");

	}

	@Override
	public void acceptedConnection(SSLSession session) {

		logger.info(session, "Conexão aceita");

	}

	@Override
	public void rejectedConnection(SSLSession session, EnumDisconnectionReason reason) {

		logger.error(session, "Conexão rejeitada");

	}

	@Override
	public void closedConnection(SSLSession session, EnumDisconnectionReason reason) {

		logger.info(session, "Conexão encerrada: %s", reason.toString());

	}

	@Override
	public void destroyedConnection(long connId, String address) {

		logger.trace("Conexão destruï¿½da: %d - %s", connId, address);
		
	}

	@Override
	public void connectionError(SSLSession session, IOException e) {

		logger.error(session, "Falha na conexão: %s", ExceptionUtils.rootCauseMessage(e));

	}

	@Override
	public void messageReceived(SSLSession session, int bytesReceived, SSLMessage message) {

		if (bytesReceived <= 0 || message == null) {
			logger.warn(session, "Mensagem recebida: [nula]");
			return;
		}

		MessageTest decodedMessage = null;
		
		if (message instanceof MessageTest) {
			decodedMessage = (MessageTest) message; 
		} else {
			logger.warn(session, "Tipo de mensagem desconhecido: ", message.getClass().getSimpleName());
			return;
		}

		logger.info(session, "Mensagem recebida: %s", decodedMessage.getText());

		MessageTest outputMessage = SSLMessage.<MessageTest, MessageTest>copy(decodedMessage, MessageTest.class);
		
		logger.info(session, "Mensagem loopback: %s", Function.byteBufferToString(decodedMessage.getContent(), true));

		SSLSender sender = session.getSender();

		try {
			sender.send(outputMessage);
		} catch (SendError e) {
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void messageSent(SSLSession session, int bytesSent, SSLMessage message) {

		if (bytesSent <= 0 || message == null) {
			logger.warn(session, "Mensagem enviada: [nula]");
			return;
		}

		MessageTest decodedMessage = null;

		if (message instanceof MessageTest) {
			decodedMessage = (MessageTest) message; 
		} else {
			logger.warn(session, "Tipo de mensagem desconhecido: ", message.getClass().getSimpleName());
			return;
		}

		logger.info(session, "Mensagem enviada: %s", decodedMessage.getText());

	}

	@Override
	public void error(IOException e) {

		logger.error("Erro no servidor: %s", ExceptionUtils.rootCauseMessage(e));

	}

	@Override
	public void stoping() {

		logger.info("Encerrando o servidor");
		
	}

	@Override
	public void stopped() {

		logger.info("Servidor encerrado com sucesso");
		
	}

}
