package edu.uw.cs.cse461.consoleapps.solution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import edu.uw.cs.cse461.consoleapps.DataXferInterface.DataXferRawInterface;
import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.service.DataXferRawService;
import edu.uw.cs.cse461.service.DataXferServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

/**
 * Raw sockets version of ping client.
 * @author zahorjan
 *
 */
public class DataXferRaw extends NetLoadableConsoleApp implements DataXferRawInterface {
	private static final String TAG="DataXferRaw";
	
	// ConsoleApp's must have a constructor taking no arguments
	public DataXferRaw() throws Exception {
		super("dataxferraw");
	}

	/**
	 * This method is invoked each time the infrastructure is asked to launch this application.
	 */
	@Override
	public void run() {
		
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

			int basePort = config.getAsInt("dataxferraw.server.baseport", -1);
			if ( basePort == -1 ) {
				System.out.print("Enter port number, or empty line to exit: ");
				String portStr = console.readLine();
				if ( portStr == null || portStr.trim().isEmpty() ) return;
				basePort = Integer.parseInt(portStr);
			}
			
			int socketTimeout = config.getAsInt("net.timeout.socket", -1);
			if ( socketTimeout < 0 ) {
				System.out.print("Enter socket timeout (in msec.): ");
				String timeoutStr = console.readLine();
				socketTimeout = Integer.parseInt(timeoutStr);
				
			}

			System.out.print("Enter number of trials: ");
			String trialStr = console.readLine();
			int nTrials = Integer.parseInt(trialStr);

			for ( int index=0; index<DataXferRawService.NPORTS; index++ ) {

				TransferRate.clear();
				
				int port = basePort + index;
				int xferLength = DataXferRawService.XFERSIZE[index];

				System.out.println("\n" + xferLength + " bytes");

				//-----------------------------------------------------
				// UDP transfer
				//-----------------------------------------------------

				TransferRateInterval udpStats = udpDataXferRate(DataXferServiceBase.HEADER_BYTES, server, port, socketTimeout, xferLength, nTrials);
				
				System.out.println("UDP: xfer rate = " + String.format("%9.0f", udpStats.mean() * 1000.0) + " bytes/sec.");
				System.out.println("UDP: failure rate = " + String.format("%5.1f", udpStats.failureRate()) +
						           " [" + udpStats.nAborted() + "/" + udpStats.nTrials() + "]");

				//-----------------------------------------------------
				// TCP transfer
				//-----------------------------------------------------

				TransferRateInterval tcpStats = tcpDataXferRate(DataXferServiceBase.HEADER_BYTES, server, port, socketTimeout, xferLength, nTrials);

				System.out.println("\nTCP: xfer rate = " + String.format("%9.0f", tcpStats.mean() * 1000.0) + " bytes/sec.");
				System.out.println("TCP: failure rate = " + String.format("%5.1f", tcpStats.failureRate()) +
						           " [" + tcpStats.nAborted()+ "/" + tcpStats.nTrials() + "]");

			}
			
		} catch (Exception e) {
			System.out.println("Unanticipated exception: " + e.getMessage());
		}
	}
	
	/**
	 * This method performs the actual data transfer, returning the result.  It doesn't measure
	 * performance, though.
	 * 
	 * @param header The header to put on the outgoing packet
	 * @param hostIP  Destination IP address
	 * @param udpPort Destination port
	 * @param socketTimeout how long to wait for response before giving up
	 * @param xferLength The number of data bytes each response packet should carry
	 */
	@Override
	public byte[] udpDataXfer(byte[] header, String hostIP, int udpPort, int socketTimeout, int xferLength) throws IOException, Exception {
		//TODO: implement this method
		//(note it is used by udpDataXferRate, an important method for you to look at)
		
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(socketTimeout); // wait at most a bounded time when receiving on this socket
	
			DatagramPacket packet = new DatagramPacket(header, header.length, new InetSocketAddress(hostIP, udpPort));
			socket.send(packet);
			
			byte[] result = new byte[xferLength];
			
			int bytesRead = 0;
			try {				
				byte[] receiveBuf = new byte[header.length + 1000];
				DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				while (bytesRead < xferLength) {
					socket.receive(receivePacket);
					if ( receivePacket.getLength() != Math.min(1000, xferLength - bytesRead) + header.length ) {
						socket.close();
						throw new Exception("Bad response: did not get back a multiple of 1000 bytes and is not final packet.");
					}
					String rcvdHeader = new String(receiveBuf, 0, 4);
					if ( !rcvdHeader.equalsIgnoreCase(DataXferServiceBase.RESPONSE_OKAY_STR) ) {
						socket.close();
						throw new Exception("Bad returned header: got '" + rcvdHeader + "' but wanted '" + DataXferServiceBase.RESPONSE_OKAY_STR);
					}
					// Copy section of data to result
					for (int i = header.length; i < receivePacket.getLength(); i++)
						result[bytesRead+i] = receiveBuf[i];
					
					// Increment data read.
					bytesRead += receivePacket.getLength() - header.length;
				}
				socket.close();
				return result;
			} catch (SocketTimeoutException e) {
				System.out.println("UDP socket timeout");
				throw e;
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Performs nTrials trials via UDP of a data xfer to host hostIP on port udpPort.  Expects to get xferLength
	 * bytes in total from that host/port.  Is willing to wait up to socketTimeout msec. for new data to arrive.
	 * @return A TransferRateInterval object that measured the total bytes of data received over all trials and
	 * the total time taken.  The measured time should include socket creation time.
	 */
	@Override
	public TransferRateInterval udpDataXferRate(byte[] header, String hostIP, int udpPort, int socketTimeout, int xferLength, int nTrials) {

		for ( int trial=0; trial<nTrials; trial++ ) {
			try {
				TransferRate.start("udp");
				udpDataXfer(header, hostIP, udpPort, socketTimeout, xferLength);
				TransferRate.stop("udp", xferLength);
			} catch ( java.net.SocketTimeoutException e) {
				TransferRate.abort("udp", xferLength);
				//System.out.println("UDP trial timed out");
			} catch (Exception e) {
				TransferRate.abort("udp", xferLength);
				//System.out.println("Unexpected " + e.getClass().getName() + " exception in UDP trial: " + e.getMessage());
			}
		}
		
		return TransferRate.get("udp");
	}
	
	private static final int MAX_CHUNK_SIZE = 1000000;

	/**
	 * Method to actually transfer data over TCP, without measuring performance.
	 */
	@Override
	public byte[] tcpDataXfer(byte[] header, String hostIP, int tcpPort, int socketTimeout, int xferLength) throws IOException, Exception {
		//TODO: implement this method
		//(note it is used by tcpDataXferRate, an important method for you to look at)		
		try {
			Socket tcpSocket = new Socket(hostIP, tcpPort);
			tcpSocket.setSoTimeout(socketTimeout);
			InputStream is = tcpSocket.getInputStream();
			OutputStream os = tcpSocket.getOutputStream();
			
			// send header
			os.write(DataXferServiceBase.HEADER_BYTES);
			tcpSocket.shutdownOutput();
			
			byte[] result = new byte[xferLength];
			
			int bytesRead = -header.length;
			byte[] receiveBuf = new byte[MAX_CHUNK_SIZE];
			while (bytesRead < xferLength) {
				int len = is.read(receiveBuf);
				if (len == 0) {
					tcpSocket.close();
					throw new Exception("Didn't read anything.");
				}
				if (bytesRead == -header.length) {
					// Check header if first chunk.
					String headerStr = new String(receiveBuf, 0, 4);
					if ( !headerStr.equalsIgnoreCase(DataXferServiceBase.RESPONSE_OKAY_STR)) {
						tcpSocket.close();
						throw new Exception("Bad response header: got '" + headerStr + "' but expected '" + DataXferServiceBase.RESPONSE_OKAY_STR + "'");
					}
					// Copy into result array.
					for (int i = header.length; i < len; i++)
						result[i-header.length] = receiveBuf[i];
				} else {
					//Copy into result array.
					for (int i = 0; i < len; i++)
						result[bytesRead+i] = receiveBuf[i];
				}
				bytesRead += len;
			}
			tcpSocket.close();
			return result;
		} catch (ConnectException e) {
			System.out.println("TCP connection refused");
			throw e;
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Performs nTrials trials via UDP of a data xfer to host hostIP on port udpPort.  Expects to get xferLength
	 * bytes in total from that host/port.  Is willing to wait up to socketTimeout msec. for new data to arrive.
	 * @return A TransferRateInterval object that measured the total bytes of data received over all trials and
	 * the total time taken.  The measured time should include socket creation time.
	 */
	@Override
	public TransferRateInterval tcpDataXferRate(byte[] header, String hostIP, int tcpPort, int socketTimeout, int xferLength, int nTrials) {

		for ( int trial=0; trial<nTrials; trial++) {
			try {
				TransferRate.start("tcp");
				tcpDataXfer(header, hostIP, tcpPort, socketTimeout, xferLength);
				TransferRate.stop("tcp", xferLength);
			} catch (ConnectException e) {
				TransferRate.abort("tcp", xferLength);
			} catch (Exception e) {
				TransferRate.abort("tcp", xferLength);
				//System.out.println("TCP trial failed: " + e.getMessage());
			}
		
		}
		return TransferRate.get("tcp");
	}
	
}
