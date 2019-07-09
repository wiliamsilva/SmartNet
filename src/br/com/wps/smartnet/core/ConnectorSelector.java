package br.com.wps.smartnet.core;

import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;

public interface ConnectorSelector {

	Connector select(SmartConcurrentHashMap<String, Connector> connectorList) throws NotFound;
	
}
