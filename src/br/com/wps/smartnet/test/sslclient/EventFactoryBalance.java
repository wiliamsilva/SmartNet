package br.com.wps.smartnet.test.sslclient;

import java.io.IOException;

import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnet.ssl.SSLClientEventFactory;
import br.com.wps.smartnet.ssl.SSLMessage;
import br.com.wps.smartnet.ssl.SSLSession;
import br.com.wps.smartnetutils.ext.ExceptionUtils;

public class EventFactoryBalance extends SSLClientEventFactory {

	private SmartLog logger;

	public EventFactoryBalance() {
		logger = new SmartLog(this);
	}

	@Override
	public void starting() {
		
		logger.info("Iniciando o client");

	}

	@Override
	public void started() {

		logger.info("Client iniciado com sucesso");

	}

	@Override
	public void connected(SSLSession session) {

		logger.info(session, "Conexão realizada com sucesso.");
		
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

		logger.trace("Conexão destruída: %d - %s", connId, address);
		
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
			logger.warn(session, "Tipo de mensagem desconhecido: %s", message.getClass().getSimpleName());
			return;
		}

		decodedMessage.unwrap();

		logger.info(session, "Mensagem recebida: %s", decodedMessage.getText());

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
			logger.warn(session, "Tipo de mensagem desconhecido: %s", message.getClass().getSimpleName());
			return;
		}

		logger.info(session, "Mensagem recebida: %s", decodedMessage.getText());
		
	}

	@Override
	public void error(IOException e) {

		logger.error("Erro no client: %s", ExceptionUtils.rootCauseMessage(e));

	}

	@Override
	public void stoping() {

		logger.info("Encerrando o client");
		
	}

	@Override
	public void stopped() {

		logger.info("Client encerrado com sucesso");
		
	}

}
