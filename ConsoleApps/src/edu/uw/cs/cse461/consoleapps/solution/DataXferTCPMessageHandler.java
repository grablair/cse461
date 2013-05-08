package edu.uw.cs.cse461.consoleapps.solution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.consoleapps.DataXferInterface.DataXferTCPMessageHandlerInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.net.tcpmessagehandler.TCPMessageHandler;
import edu.uw.cs.cse461.service.DataXferServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public class DataXferTCPMessageHandler extends NetLoadableConsoleApp implements DataXferTCPMessageHandlerInterface {

	public DataXferTCPMessageHandler() {
		super("dataxfertcpmessagehandler");
	}

	@Override
	public void run() throws Exception {
		try {

			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

			ConfigManager config = NetBase.theNetBase().config();
			String server = config.getProperty("net.server.ip");
			if ( server == null ) {
				System.out.print("Enter a host ip, or exit to exit: ");
				server = console.readLine();
				if ( server == null ) return;
				if ( server.equals("exit")) return;
			}

			System.out.print("Enter the server's port number, or empty line to exit: ");
			String targetPortStr = console.readLine();
			if ( targetPortStr == null || targetPortStr.trim().isEmpty() ) return;
			int targetPort = Integer.parseInt( targetPortStr );
			
			int socketTimeout = config.getAsInt("net.timeout.socket", -1);
			if ( socketTimeout < 0 ) {
				System.out.print("Enter socket timeout (in msec.): ");
				String timeoutStr = console.readLine();
				socketTimeout = Integer.parseInt(timeoutStr);
				
			}

			System.out.print("Enter number of trials: ");
			String trialStr = console.readLine();
			int nTrials = Integer.parseInt(trialStr);
			
			System.out.print("Enter the ammount of data to transfer, or empty line to exit: ");
			String xferLengthStr = console.readLine();
			if ( xferLengthStr == null || xferLengthStr.trim().isEmpty() ) return;
			int xferLength = Integer.parseInt( xferLengthStr );
			
			TransferRate.clear();

			System.out.println("\n" + xferLength + " bytes");

			//-----------------------------------------------------
			// TCP transfer
			//-----------------------------------------------------

			TransferRateInterval xferStats = DataXferRate(DataXferServiceBase.HEADER_STR, server, targetPort, socketTimeout, xferLength, nTrials);

			System.out.println("\nTCP: xfer rate = " + String.format("%9.0f", xferStats.mean() * 1000.0) + " bytes/sec.");
			System.out.println("TCP: failure rate = " + String.format("%5.1f", xferStats.failureRate()) +
					" [" + xferStats.nAborted()+ "/" + xferStats.nTrials() + "]");

			
		} catch (Exception e) {
			System.out.println("Unanticipated exception: " + e.getMessage());
		}
		
	}
	

	@Override
	public TransferRateInterval DataXferRate(String header, String hostIP,
			int port, int timeout, int xferLength, int nTrials) {
		
		for ( int trial=0; trial<nTrials; trial++) {
			try {
				TransferRate.start("xfer");
				DataXfer(header, hostIP, port, timeout, xferLength);
				TransferRate.stop("xfer", xferLength);
			} catch (ConnectException e) {
				TransferRate.abort("xfer", xferLength);
			} catch (Exception e) {
				TransferRate.abort("xfer", xferLength);
				System.out.println("xfer trial failed: " + e.getMessage());
			}
		
		}
		return TransferRate.get("xfer");
	}
	
	@Override
	public byte[] DataXfer(String header, String hostIP, int port, int timeout,
			int xferLength) throws JSONException, IOException {
		Socket tcpSocket = null;
		TCPMessageHandler tcpMessageHandlerSocket = null;
		try {
			// set up the Message Handler
			tcpSocket = new Socket(hostIP, port);
			tcpMessageHandlerSocket = new TCPMessageHandler(tcpSocket);
			tcpMessageHandlerSocket.setTimeout(timeout);
			tcpMessageHandlerSocket.setNoDelay(true);
			
			// send the header and message
			tcpMessageHandlerSocket.sendMessage(header);
			JSONObject lengthRequest = new JSONObject();
			lengthRequest.put("transferSize", xferLength);
			tcpMessageHandlerSocket.sendMessage(lengthRequest);
			
			// read response header
			String headerStr = tcpMessageHandlerSocket.readMessageAsString();
			if ( ! headerStr.equalsIgnoreCase(DataXferServiceBase.RESPONSE_OKAY_STR) )
				throw new IOException("Bad response header: '" + headerStr + "'");

			// read response
			ByteBuffer message = ByteBuffer.allocate(xferLength);
			int bytesRead = 0;
			
			while (bytesRead < xferLength) {
				byte[] chunk = tcpMessageHandlerSocket.readMessageAsBytes();
				message.put(chunk);
				bytesRead += chunk.length;
			}
			
			tcpMessageHandlerSocket.close();
			return message.array();
		} catch (ConnectException e) {
			System.out.println("TCP connection refused");
			throw e;
		} catch (IOException e) {
			System.out.println("Exception: " + e.getMessage());
			throw e;
		} finally {
			if ( tcpMessageHandlerSocket != null ) try {tcpMessageHandlerSocket.close();} catch (Exception e) {}
		}
	}
	

}
