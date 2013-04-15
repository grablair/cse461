package edu.uw.cs.cse461.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import edu.uw.cs.cse461.net.base.NetBase;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * An echo service that communicates directly over UPD and TCP sockets that it creates.
 * @author zahorjan
 *
 */
public class EchoRawService extends EchoServiceBase  {
	private static final String TAG="EchoRawService";
	
	private ServerSocket mServerSocket;
	private DatagramSocket mDatagramSocket;
	
	/**
	 * A NetLoadableService must provide a public constructor taking no arguments.
	 * <p>
	 * This service must listen to both a UDP and a TCP port.  It creates sockets
	 * bound to those ports in this constructor.  It also creates a thread per socket -
	 * the thread blocks trying to receive data on its socket, and when it does,
	 * echoes back whatever it receives. 
	 * @throws Exception
	 */
	public EchoRawService() throws Exception {
		super("echoraw");
		
		// Sanity check -- code below relies on this property
		if ( HEADER_STR.length() != RESPONSE_OKAY_STR.length() )
			throw new Exception("Header and response strings must be same length: '" + HEADER_STR + "' '" + RESPONSE_OKAY_STR + "'");	
		
		// The echo raw service's IP address is the ip the entire app is running under
		String serverIP = IPFinder.localIP();
		if ( serverIP == null ) throw new Exception("IPFinder isn't providing the local IP address.  Can't run.");
		
		// There is (purposefully) no config file field to define the echo raw service's ports.
		// Instead, ephemeral ports are used.  (You can run the dumpservericestate application
		// to see ports are actually allocated.)
				
		mServerSocket = new ServerSocket();
		mServerSocket.bind(new InetSocketAddress(serverIP, 0));
		mServerSocket.setSoTimeout(NetBase.theNetBase().config().getAsInt("net.timeout.granularity", 500));
		
		mDatagramSocket = new DatagramSocket(new InetSocketAddress(serverIP, 0));
		mDatagramSocket.setSoTimeout(NetBase.theNetBase().config().getAsInt("net.timeout.granularity", 500));
		
		Log.i(TAG,  "Server socket = " + mServerSocket.getLocalSocketAddress());
		Log.i(TAG,  "Datagram socket = " + mDatagramSocket.getLocalSocketAddress());
		
		// Code/thread handling the UDP socket
		Thread dgramThread = new Thread() {
								public void run() {
									byte buf[] = new byte[64*1024];
									DatagramPacket packet = new DatagramPacket(buf, buf.length);

									//	Thread termination in this code is primitive.  When shutdown() is called (by the
									//	application's main thread, so asynchronously to the threads just mentioned) it
									//	closes the sockets.  This causes an exception on any thread trying to read from
									//	it, which is what provokes thread termination.
									try {
										while ( !mAmShutdown ) {
											try {
												mDatagramSocket.receive(packet);
												if ( packet.getLength() < HEADER_STR.length() )
													throw new Exception("Bad header: length = " + packet.getLength());
												String headerStr = new String( buf, 0, HEADER_STR.length() );
												if ( ! headerStr.equalsIgnoreCase(HEADER_STR) )
													throw new Exception("Bad header: got '" + headerStr + "', wanted '" + HEADER_STR + "'");
												System.arraycopy(RESPONSE_OKAY_STR.getBytes(), 0, buf, 0, HEADER_STR.length());
												mDatagramSocket.send( new DatagramPacket(buf, packet.getLength(), packet.getAddress(), packet.getPort()));
											} catch (SocketTimeoutException e) {
												// socket timeout is normal
											} catch (Exception e) {
												Log.w(TAG,  "Dgram reading thread caught " + e.getClass().getName() + " exception: " + e.getMessage());
											}
										}
									} finally {
										if ( mDatagramSocket != null ) { mDatagramSocket.close(); mDatagramSocket = null; }
									}
								}
		};
		dgramThread.start();
		
		// Code/thread handling the TCP socket
		Thread tcpThread = new Thread() {

			public void run() {
				byte[] header = new byte[4];
				byte[] buf = new byte[1024];
				int socketTimeout = NetBase.theNetBase().config().getAsInt("net.timeout.socket", 5000);
				try {
					while ( !isShutdown() ) {
						Socket sock = null;
						try {
							// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
							// with that client.  That socket is returned.
							sock = mServerSocket.accept();
							// We're going to read from sock, to get the message to echo, but we can't risk a client mistake
							// blocking us forever.  So, arrange for the socket to give up if no data arrives for a while.
							sock.setSoTimeout(socketTimeout);
							InputStream is = sock.getInputStream();
							OutputStream os = sock.getOutputStream();
							// Read the header.  Either it gets here in one chunk or we ignore it.  (That's not exactly the
							// spec, admittedly.)
							int len = is.read(header);
							if ( len != HEADER_STR.length() )
								throw new Exception("Bad header length: got " + len + " but wanted " + HEADER_STR.length());
							String headerStr = new String(header); 
							if ( !headerStr.equalsIgnoreCase(HEADER_STR) )
								throw new Exception("Bad header: got '" + headerStr + "' but wanted '" + HEADER_STR + "'");
							os.write(RESPONSE_OKAY_STR.getBytes());
							
							// Now read and echo the payload.
							// Keep reading until the client has closed its side of the connection
							while ( (len = is.read(buf)) >= 0 ) os.write(buf, 0, len);
							
						} catch (SocketTimeoutException e) {
							// normal behavior, but we're done with the client we were talking with
						} catch (Exception e) {
							Log.i(TAG, "TCP thread caught " + e.getClass().getName() + " exception: " + e.getMessage());
						} finally {
							if ( sock != null ) try { sock.close(); sock = null;} catch (Exception e) {}
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "TCP server thread exiting due to exception: " + e.getMessage());
				} finally {
					if ( mServerSocket != null ) try { mServerSocket.close(); mServerSocket = null; } catch (Exception e) {}
				}
			}
		};
		tcpThread.start();
	}

	
	/**
	 * This method is called when the entire infrastructure
	 * wants to terminate.  We set a flag indicating all threads
	 * should terminate.  We then close the sockets.  The threads
	 * using those sockets will either timeout and see the flag set or
	 * else wake up on an IOException because the socket has been closed
	 * and notice the flag is set.  Either way, they'll terminate.
	 */
	@Override
	public void shutdown() {
		super.shutdown();
		Log.d(TAG, "Shutting down");
	}
	
	/**
	 * The NetLoadableServer interface requires a method that will return a representation of
	 * the current server state.  This server's state is its network location (IP:port).
	 */
	@Override
	public String dumpState() {
		StringBuilder sb = new StringBuilder(super.dumpState());
		sb.append("\nListening on:\n\tTCP: ");
		if ( mServerSocket != null ) sb.append(mServerSocket.toString());
		else sb.append("Not listening");
		sb.append("\n\tUDP: ");
		if ( mDatagramSocket != null ) sb.append(mDatagramSocket.getLocalSocketAddress());
		else sb.append("Not listening");
		return sb.toString();
	}

}
