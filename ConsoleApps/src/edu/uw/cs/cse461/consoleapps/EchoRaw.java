package edu.uw.cs.cse461.consoleapps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.net.base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.service.EchoServiceBase;
import edu.uw.cs.cse461.util.ConfigManager;

/**
 * Raw sockets version of echo client.
 * @author zahorjan
 *
 */
public class EchoRaw extends NetLoadableConsoleApp {
	private static final String TAG="EchoRaw";
	
	/**
	 * A NetLoadableConsoleApp must have a public constructor taking no arguments.
	 * You can do in the constructor whatever is required.  Note that only one
	 * instance of the application is constructed, ever.  (It is repeatedly "invoked"
	 * for execution by calling its run() method. )
	 * <p>
	 * The superclass constructor requires two arguments.  The first is an internal
	 * name for the application - other components find this application using this
	 * name.  The second is a boolean that is set to true if the implementation is ready
	 * (enough) that the infrastructure should load it, if it's named in the config file.
	 * If that argument is false, the application won't be loaded even if named in the
	 * config file.
	 */
	public EchoRaw() {
		super("echoraw");
	}
	
	/**
	 * The infrastructure invokes this method when it receives a request to run
	 * this application.
	 */
	@Override
	public void run() {
		try {
			
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			// The config object represents the configuration file
			ConfigManager config = NetBase.theNetBase().config();

			// The config file may, or may not, say where the server is located
			String targetIP = config.getProperty("net.server.ip");
			if ( targetIP == null ) {
				System.out.println("No net.server.ip entry in config file.");
				System.out.print("Enter a server ip, or empty line to exit: ");
				targetIP = console.readLine();
				if ( targetIP == null || targetIP.trim().isEmpty() ) return;
			}

			int targetUDPPort;
                        System.out.print("Enter the server's UDP port, or empty line to skip: ");
                        String targetUDPPortStr = console.readLine();
                        if ( targetUDPPortStr == null || targetUDPPortStr.trim().isEmpty() ) targetUDPPort = 0;
                        else targetUDPPort = Integer.parseInt(targetUDPPortStr);

			int targetTCPPort;
                        System.out.print("Enter the server's TCP port, or empty line to skip: ");
                        String targetTCPPortStr = console.readLine();
                        if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) targetTCPPort = 0;
                        else targetTCPPort = Integer.parseInt(targetTCPPortStr);

			// read a socket timeout value from the config file, but specify a default to be returned
			// if the config file doesn't contain a value
			
			int socketTimeout = config.getAsInt("net.timeout.socket", 5000);
			
			while ( true ) {
				try {

					System.out.print("Enter message to be echoed, or empty string to exit: ");
					String msg = console.readLine();
					if ( msg.isEmpty() ) return;

					// skip udp connection if user insisted
					if ( targetUDPPort != 0 ) {
						DatagramSocket socket = new DatagramSocket();
						socket.setSoTimeout(socketTimeout); // wait at most a bounded time when receiving on this socket
						int dataLength = EchoServiceBase.HEADER_LEN + msg.getBytes().length;
						if ( dataLength > 1400 )
							throw new Exception("Data is too long for UDP echo");
						byte[] buf = new byte[dataLength];
						ByteBuffer bufBB = ByteBuffer.wrap(buf);
						bufBB.put(EchoServiceBase.HEADER_BYTES).put(msg.getBytes());
						DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress(targetIP, targetUDPPort));
						socket.send(packet);  // tell the server we're here.  The server will get our IP and port from the received packet.

						// we're supposed to get back what we sent (but with header contents changed),
						// so the amount of buffer we need is equal to size of what we sent.
						byte[] receiveBuf = new byte[dataLength];
						DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
						try { 
							socket.receive(receivePacket);
							if ( receivePacket.getLength() != buf.length )
								throw new Exception("Bad response: sent " + buf.length + " bytes but got back " + receivePacket.getLength());
							String rcvdHeader = new String(receiveBuf,0,4);
							if ( !rcvdHeader.equalsIgnoreCase(EchoServiceBase.RESPONSE_OKAY_STR) ) 
								throw new Exception("Bad returned header: got '" + rcvdHeader + "' but wanted '" + EchoServiceBase.RESPONSE_OKAY_STR);
							String response = new String(receiveBuf, 4, receivePacket.getLength()-4);
							System.out.println("UDP: '" + response + "'");
						} catch (SocketTimeoutException e) {
							// This exception is thrown if we wait on receive() longer than the timeout
							System.out.println("UDP socket timeout");
						}
						socket.close();
					}
					
					// skip tcp connection if user said to skip itk
					if ( targetTCPPort != 0 ) {
						Socket tcpSocket = new Socket(targetIP, targetTCPPort);
						tcpSocket.setSoTimeout(socketTimeout);
						InputStream is = tcpSocket.getInputStream();
						OutputStream os = tcpSocket.getOutputStream();
						
						// send header
						os.write(EchoServiceBase.HEADER_BYTES);
						
						// now send the payload
						byte[] msgBytes = msg.getBytes();
						os.write(msgBytes, 0, msgBytes.length);
						tcpSocket.shutdownOutput();
						
						// read the header.  Either the entire header arrives in one chunk, or we
						// (mistakenly) reject it.
						byte[] headerBuf = new byte[EchoServiceBase.HEADER_LEN];
						int len = is.read(headerBuf);
						if ( len != EchoServiceBase.HEADER_LEN )
							throw new Exception("Bad response header length: got " + len + " but expected " + EchoServiceBase.HEADER_LEN);
						String headerStr = new String(headerBuf);
						if ( !headerStr.equalsIgnoreCase(EchoServiceBase.RESPONSE_OKAY_STR))
							throw new Exception("Bad response header: got '" + headerStr + "' but expected '" + EchoServiceBase.HEADER_STR + "'");
						
						// read the payload.  We don't attempt to verify that the payload is what we sent
						byte[] buf = new byte[msgBytes.length];
						System.out.print("TCP: '");
						try {
							len = 0;
							while ( len >= 0 ) {
								len = is.read(buf, 0, buf.length);
								if ( len > 0 ) {
									String response = new String(buf, 0, len);
									System.out.print(response);
								}
							}
							System.out.println("'");
						} catch (Exception e) {
							System.out.println("TCP read failed: " + e.getMessage());
						}
						tcpSocket.close();
					}
					
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				} 
			}
		} catch (Exception e) {
			System.out.println("EchoRaw.run() caught exception: " +e.getMessage());
		}
	}
}
