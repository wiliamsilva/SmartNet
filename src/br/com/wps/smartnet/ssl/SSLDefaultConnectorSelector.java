package br.com.wps.smartnet.ssl;

import java.util.Set;

import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;

public class SSLDefaultConnectorSelector implements SSLConnectorSelector {

	@Override
	public synchronized SSLConnector select(SmartConcurrentHashMap<String, SSLConnector> SSLConnectorList) throws NotFound {
		
		SSLConnector result = null;
		
		
		if (SSLConnectorList == null || SSLConnectorList.size() == 0) {
			
			throw new NotFound("SSLConnector list is empty");
			
		}

		Set<String> keys = SSLConnectorList.keySet();
		
		for (String key: keys) {
			
			SSLConnector c = SSLConnectorList.get(key);
			
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
