package edu.uw.cs.cse461.consoleapps;

import java.net.InetAddress;
import java.util.ArrayList;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.IPFinder;

/**
 * Prints out information about the network interfaces available on your system,
 * including (and especially) the IP addresses they offer.
 * @author zahorjan
 *
 */
public class ListInterfaces extends NetLoadableConsoleApp {
	
	// NetLoadableApp's must have a constructor taking no arguments
	public ListInterfaces() {
		super("listinterfaces");
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Interfaces List:\n");
			System.out.println(IPFinder.logInterfaces());
			
			ConfigManager config = NetBase.theNetBase().config();
			String configIP = config.getProperty("net.host.ip");
			if ( configIP == null ) System.out.println("\nConfig file does not specify IP (no net.host.ip field)");
			else System.out.println("\nConfig file has net.host.ip entry: " + configIP);

			ArrayList<InetAddress> inetList = IPFinder.getPreferredAddressList();
			if ( inetList.isEmpty() ) System.out.println("\nIPFinder preferred list is empty");
			else {
				System.out.println("\nIPFinder has preferred list:\n");
				for (InetAddress address : inetList ) {
					System.out.println("\t" + address.getHostAddress());
				}
			}
			
			String localIP = IPFinder.localIP();
			if ( localIP != null ) System.out.println("\nIPFinder is advertising IP " + localIP);
			else System.out.println("\nIPFinder is providing null as the local IP");
			
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
	}
}
