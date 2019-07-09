package br.com.wps.smartnet.core;

import java.util.Set;

import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;

public class DefaultConnectorSelector implements ConnectorSelector {

	@Override
	public synchronized Connector select(SmartConcurrentHashMap<String, Connector> connectorList) throws NotFound {
		
		Connector result = null;
		
		
		if (connectorList == null || connectorList.size() == 0) {
			
			throw new NotFound("Connector list is empty");
			
		}

		Set<String> keys = connectorList.keySet();
		
		for (String key: keys) {
			
			Connector c = connectorList.get(key);
			
			if (c.isOnline()) {
				result = c;
				break;
			}
			
		}
		
		if (result == null) {
			throw new NotFound("No connector available");
		}
		
		return result;

	}

}
