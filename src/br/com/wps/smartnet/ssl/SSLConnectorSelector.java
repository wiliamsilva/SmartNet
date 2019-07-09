package br.com.wps.smartnet.ssl;

import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;

public interface SSLConnectorSelector {

	SSLConnector select(SmartConcurrentHashMap<String, SSLConnector> connectorList) throws NotFound;
	
}
