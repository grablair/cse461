package edu.uw.cs.cse461.consoleapps.solution;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import edu.uw.cs.cse461.consoleapps.PingInterface.PingTCPMessageHandlerInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.service.EchoServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public class PingTCPMessageHandler extends NetLoadableConsoleApp implements PingTCPMessageHandlerInterface {
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingTCPMessageHandler() {
		super("pingtcpmessagehandler");
	}

	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.ConsoleApps.PingInterface#run()
	 */
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

			System.out.print("Enter number of trials: ");
			String trialStr = console.readLine();
			int nTrials = Integer.parseInt(trialStr);

			int socketTimeout = config.getAsInt("net.timeout.socket", 2000);

			System.out.println("Host: " + targetIP);
			System.out.println("tcp port: " + targetTCPPort);
			System.out.println("trials: " + nTrials);

			ElapsedTimeInterval pingResult = null;

			if ( targetTCPPort != 0 ) {
				ElapsedTime.clear();
				pingResult = ping("ping", targetIP, targetTCPPort, socketTimeout, nTrials);
			}

			if ( pingResult != null ) System.out.println("PING: " + String.format("%.2f msec (%d failures)", pingResult.mean(), pingResult.nAborted()));

		} catch (Exception e) {
			System.out.println("PingTCPMessageHandler.run() caught exception: " + e.getMessage());
		}
	}
	
	
	@Override
	public ElapsedTimeInterval ping(String header, String hostIP, int port, int timeout, int nTrials) throws Exception {
		
		//TODO: implement this method
		
		try {

			Socket tcpSocket = null;
			TCPMessageHandler tcpMessageHandlerSocket = null;
			String msg = "";
			for(int i = 0 ; i < nTrials ; i++) {
				try {
					ElapsedTime.start("PingTCPMessageHandler_PingTotalDelay");
					
					// set up the Message Handler
					tcpSocket = new Socket(hostIP, port);
					tcpMessageHandlerSocket = new TCPMessageHandler(tcpSocket);
					tcpMessageHandlerSocket.setTimeout(timeout);
					tcpMessageHandlerSocket.setNoDelay(true);
					
					// send the header and message
					tcpMessageHandlerSocket.sendMessage(header);
					tcpMessageHandlerSocket.sendMessage(msg);

					// read response header
					String headerStr = tcpMessageHandlerSocket.readMessageAsString();
					if ( ! headerStr.equalsIgnoreCase(EchoServiceBase.RESPONSE_OKAY_STR) )
						throw new Exception("Bad response header: '" + headerStr + "'");

					// read response payload (which should be empty)
					String response = tcpMessageHandlerSocket.readMessageAsString();
					if ( !  response.equals(msg))
						throw new Exception("Bad response payload: sent '" + msg + "' got back '" + response + "'");
				} catch (SocketTimeoutException e) {
					System.out.println("Timed out");
					ElapsedTime.abort("PingTCPMessageHandler_PingTotalDelay");
				} catch (Exception e) {
					System.out.println("TCPMessageHandler read failed: " + e.getMessage());
					ElapsedTime.abort("PingTCPMessageHandler_PingTotalDelay");
				} finally {
					if ( tcpMessageHandlerSocket != null ) {
						try {
							tcpMessageHandlerSocket.close();
							ElapsedTime.stop("PingTCPMessageHandler_PingTotalDelay");
						} catch (Exception e) {
							ElapsedTime.abort("PingTCPMessageHandler_PingTotalDelay");
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			ElapsedTime.abort("PingTCPMessageHandler_PingTotalDelay");
		}

		return ElapsedTime.get("PingTCPMessageHandler_PingTotalDelay");
	}

}
