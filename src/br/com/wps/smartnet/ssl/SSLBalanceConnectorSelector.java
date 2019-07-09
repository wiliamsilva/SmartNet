package br.com.wps.smartnet.ssl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;

public class SSLBalanceConnectorSelector implements SSLConnectorSelector {

	private Iterator<String> it;
	
	public SSLBalanceConnectorSelector() {
		this.it = null;
	}

	@Override
	public synchronized SSLConnector select(SmartConcurrentHashMap<String, SSLConnector> connectorList) throws NotFound {
		
		SSLConnector result = null;
		
		
		if (connectorList == null || connectorList.size() == 0) {
			
			throw new NotFound("SSLConnector list is empty");
			
		}

		if (it == null || !it.hasNext()) {
		
			Iterator<String> source = connectorList.keySet().iterator();
			
			List<String> copiedList = new ArrayList<String>();

			source.forEachRemaining(copiedList::add);

			this.it = copiedList.iterator();
			
		}

		while (it.hasNext()) {

			String key = this.it.next();
			it.remove();
			
			SSLConnector c = connectorList.get(key);

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
