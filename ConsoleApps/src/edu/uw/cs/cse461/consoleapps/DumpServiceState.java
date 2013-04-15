package edu.uw.cs.cse461.consoleapps;

import java.util.List;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;

/**
 * An application that prints the contents of the naming database used by the DDNS system. 
 * @author zahorjan
 *
 */
public class DumpServiceState extends NetLoadableConsoleApp {


	public DumpServiceState() {
		super("dumpservicestate");
	}
	
	@Override
	public void run() {
		System.out.println(_dumpState());
	}
	
	private String _dumpState() {
		
		StringBuilder sb = new StringBuilder();

		List<String> serviceList = NetBase.theNetBase().loadedServiceNames();
		for ( String sName : serviceList ) {
			sb.append("\n").append(sName).append(" Service:\n");
			NetLoadableService service = NetBase.theNetBase().getService(sName);
			if ( service != null ) sb.append(service.dumpState()).append("\n");
			else sb.append("\tNot loaded\n");
		}

		return sb.toString();
	}
	
}
