package br.com.wps.smartnet.test.client;

import br.com.wps.smartnet.core.ClientConfiguration;
import br.com.wps.smartnet.core.SmartClient;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.ext.BasicLog;

public class TestClientSimple {

	private static SmartLog logger;
	
	public static void main(String[] args) {
		
		System.setProperty(BasicLog.CONFIGURATION_FILE_SYSTEM_PROPERTY, "C:/Users/wiliam/workspace/SmartNet/client_log4j2.xml");
		
		ClientConfiguration configuration = new ClientConfiguration();
		configuration.setWaitTimeout(30);
		configuration.setIdleTimeout(0);
		configuration.setReconnectionInterval(0);
		configuration.setConnectAttempt(3);
		configuration.setAutoConnect(true);
		configuration.setAutoConnectInterval(10);
		
		String addressParam = "127.0.0.1 9001; ::1 9002; 192.168.0.173 9010";

		SmartClient client = null;

		try {
			
			client = new SmartClient("client", configuration, DecoderTest.class, EncoderTest.class, new EventFactorySimple());

			logger = new SmartLog(client);

			logger.info("Teste de client no modo padr�o");
			
			client.open(addressParam);
			
			MessageTest outMessage = new MessageTest();
			outMessage.setText("Mensagem 1 de teste");

			client.send(outMessage);
			
			outMessage = new MessageTest();
			outMessage.setText("Mensagem 2 de teste");

			client.send(outMessage);

			outMessage = new MessageTest();
			outMessage.setText("Mensagem 3 de teste");
			
			client.send(outMessage);
			
			// Aguarda 10 segundos
			long timeout = (10 * 1000L) + System.currentTimeMillis();
			
			while (timeout > System.currentTimeMillis()) {
				
				Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
				
			}
			
			client.close();

		} catch (Exception e) {
			logger.error("Falha durante teste de cliente no modo padr�o. Erro: %s", ExceptionUtils.rootCauseMessage(e));;
		}

		
	}
	
	
}
