package org.openpipeline.server.launcher;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Use this class to monitor and stop Jetty server instance.
 * 
 * @see io.rhubarb.start.Start#start()
 * @see io.rhubarb.start.Stop
 * 
 */
public class StopMonitor extends Thread {
	private String stopKey;
	private Start rhubarbServer;
	private ServerSocket serverSocket;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public StopMonitor(int stopPort, String stopKey, Start server)
			throws UnknownHostException, IOException {
		if (stopPort <= 0)
			throw new IllegalStateException("Bad stop port");
		if (stopKey == null)
			throw new IllegalStateException("Bad stop key");

		this.stopKey = stopKey;
		this.rhubarbServer = server;

		setDaemon(true);
		setName("Rhubarb Stop Monitor Thread");

		String host = System.getProperty("jetty.host", "127.0.0.1");
		InetSocketAddress address = new InetSocketAddress(host, stopPort);
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		try {
			serverSocket.bind(address, 1);
		} catch (IOException e) {
			String msg = "Could not start Rhubarb Stop Monitor Thread. Error binding to stop port " + host + " : "
					+ stopPort;
			System.out.println(msg);
			logger.error(msg, e);
		}
	}

	public void run() {
		while (serverSocket != null) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				socket.setSoLinger(false, 0);
				LineNumberReader lin = new LineNumberReader(
						new InputStreamReader(socket.getInputStream()));

				String key = lin.readLine();
				if (!stopKey.equals(key))
					continue;
				String cmd = lin.readLine();
				if ("stop".equals(cmd)) {
					System.out.println();
					System.out.println("Stopping Rhubarb server...");
					rhubarbServer.stop();
					
					//TODO: This is an unclean way to shut down a server. Do it correctly in version 2.0
					System.exit(-1);
				} else
					System.out.println("Unsupported monitor operation");
			} catch (Exception e) {
				String msg = "Could not stop Rhubarb Server";
				System.out.println(msg);
				logger.error(msg, e);			
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception e) {
						String msg = "Could not close socket.";
						System.out.println(msg);
						logger.error(msg, e);			
					}
				}
				socket = null;
				
				if(serverSocket != null){
					try{
						serverSocket.close();
					}catch(Exception e){
						String msg = "Could not close serverSocket.";
						System.out.println(msg);
						logger.error(msg, e);	
					}
				}
				serverSocket = null;
			}
		}
	}
}
