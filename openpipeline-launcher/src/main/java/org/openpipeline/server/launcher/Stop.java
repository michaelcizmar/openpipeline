package org.openpipeline.server.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
/**
 *	Use this class to stop a running instance of the Rhubarb Server.
 *	@see io.rhubarb.start.StopMonitor
 */
public class Stop {

	public static void main(String[] args) throws IOException {
		String stopPortStr = System.getProperty("stop.port");
		if (stopPortStr == null || stopPortStr.length() == 0) {
			throw new RuntimeException("Please specify a valid stop port (-Dstop.port=####)");
		}
		String stopKey = System.getProperty("stop.key");

		if (stopKey == null || stopKey.length() == 0)
			throw new RuntimeException("Please specify a valid stop key (-Dstop.key=####)");

		try {
			String host = System.getProperty("jetty.host", "127.0.0.1");
			int stopPort = Integer.parseInt(stopPortStr);
			Socket s = new Socket(InetAddress.getByName(host), stopPort);
			s.setSoLinger(false, 0);

			OutputStream out = s.getOutputStream();
			out.write((stopKey + System.lineSeparator() + "stop" + System.lineSeparator()).getBytes());
			out.flush();
			s.close();
		} catch (ConnectException e) {
			System.out.println("Rhubarb Server is not running!");
		}
	}
}
