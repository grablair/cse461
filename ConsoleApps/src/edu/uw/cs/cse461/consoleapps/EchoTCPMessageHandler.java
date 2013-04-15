package edu.uw.cs.cse461.consoleapps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

import edu.uw.cs.cse461.consoleapps.PingInterface.PingTCPMessageHandlerInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.service.EchoServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;

/**
 * Raw sockets version of ping client.
 * @author zahorjan
 *
 */
public class EchoTCPMessageHandler extends NetLoadableConsoleApp {
	private static final String TAG="PingRaw";
	
	// ConsoleApp's must have a constructor taking no arguments
	public EchoTCPMessageHandler() {
		super("echotcpmessagehandler");
	}
	
	@Override
	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			ConfigManager config = NetBase.theNetBase().config();

			String targetIP = config.getProperty("net.server.ip");
			if ( targetIP == null ) {
				System.out.print("Enter the server's ip, or empty line to exit: ");
				targetIP = console.readLine();
				if ( targetIP == null || targetIP.trim().isEmpty() ) return;
			}

			System.out.print("Enter the server's TCP port, or empty line to exit: ");
			String targetTCPPortStr = console.readLine();
			if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) return;
			int targetTCPPort = Integer.parseInt( targetTCPPortStr );

			int socketTimeout = config.getAsInt("net.timeout.socket", 2000);

			while ( true ) {
				System.out.print("Enter message to be echoed, or empty string to exit: ");
				String msg = console.readLine();
				if ( msg.isEmpty() ) return;

				Socket tcpSocket = null;
				TCPMessageHandler tcpMessageHandlerSocket = null;
				try {
					tcpSocket = new Socket(targetIP, targetTCPPort);
					tcpMessageHandlerSocket = new TCPMessageHandler(tcpSocket);
					tcpMessageHandlerSocket.setTimeout(socketTimeout);
					tcpMessageHandlerSocket.setNoDelay(true);

					tcpMessageHandlerSocket.sendMessage(EchoServiceBase.HEADER_STR);
					tcpMessageHandlerSocket.sendMessage(msg);
					
					// read response header
					String headerStr = tcpMessageHandlerSocket.readMessageAsString();
					if ( ! headerStr.equalsIgnoreCase(EchoServiceBase.RESPONSE_OKAY_STR) )
						throw new Exception("Bad response header: '" + headerStr + "'");

					// read response payload (which should be empty)
					String response = tcpMessageHandlerSocket.readMessageAsString();
					if ( !  response.equals(msg))
						throw new Exception("Bad response payload: sent '" + msg + "' got back '" + response + "'");
					System.out.println(response);
				} catch (SocketTimeoutException e) {
					System.out.println("Timed out");
				} catch (Exception e) {
					System.out.println("TCPMessageHandler read failed: " + e.getMessage());
				} finally {
					if ( tcpMessageHandlerSocket != null ) try { tcpMessageHandlerSocket.close(); } catch (Exception e) {}
				}
			}

		} catch (Exception e) {
			System.out.println("EchoTCPMessageHandler.run() caught exception: " + e.getMessage());
		}
	}
	
}
