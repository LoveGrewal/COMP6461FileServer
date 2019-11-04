package com.computerNetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * * HTTP server in java ServerSocket and Socket class.
 */
public class Https extends Thread {
	// server parameters
	private static Boolean debugMode = false;
	private static int port = 8080;
	private static String fileDirectory = "D:\\Study\\Concordia\\NetworksFiles";

	// client parameters
	private Socket clientSocket;
	private String responseStatus = "HTTP/1.1 200 OK\r\n";
	private String fileUrl = "D:\\Study\\Concordia\\NetworksFiles";
	private String fileNames = "";
	private String fileContent = "";
	private String headerContentType = "Content-Type:text/html";
	private String headerContentDisposition = "";

	private Https(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	private void printlog(String message) {
		if (debugMode) {
			System.out.println(message);
		}
	}

	public void response() {
		Date d = new Date();
		try {
			OutputStream os = this.clientSocket.getOutputStream();
			DataOutputStream outputStream = new DataOutputStream(os);
			outputStream.writeBytes(this.responseStatus);
			if (this.headerContentType != null && this.headerContentType != "") {
				outputStream.writeBytes(this.headerContentType + "\r\n");
			}
			if (this.headerContentDisposition != null && this.headerContentDisposition != "") {
				outputStream.writeBytes(this.headerContentDisposition + "\r\n");
			}
			outputStream.writeBytes("\r\n");
			if (this.fileNames != null && this.fileNames != "") {
				outputStream.writeBytes(this.fileNames + "\r\n");
			}
			if (this.fileContent != null && this.fileContent != "") {
				outputStream.writeBytes(this.fileContent + "\r\n");
			}
			clientSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean validateUrl(String url) {
		String[] locs = url.trim().split("/");
		for (String loc : locs) {
			if ("..".equalsIgnoreCase(loc)) {
				fileUrl = fileUrl.substring(0, fileUrl.lastIndexOf("\\"));
			} else {
				fileUrl = fileUrl + "\\" + loc;
			}
		}
		fileUrl = fileUrl.replace('/', '\\');
		if (fileUrl.contains(fileDirectory)) {
			return true;
		} else {
			responseStatus = "HTTP/1.1 401 UNAUTHORIZED ACCESS\r\n";
			return false;
		}
	}

	private void listFilesName() {
		File folder = new File(this.fileUrl);
		File[] files = folder.listFiles();
		if (files != null) {
			if (files.length > 0) {
				for (final File fileEntry : files) {
					if (!fileEntry.isDirectory()) {
						this.fileNames = this.fileNames + fileEntry.getName() + "\r\n";
					}
				}
			} else {
				this.fileNames = "No files in the directory\r\n";
			}

		} else {
			responseStatus = "HTTP/1.1 404 DIRECTORY NOT FOUND\r\n";
		}
	}

	public synchronized void saveFileContent(String inlineData) {
		// writes the contents
		try {
			FileWriter file = new FileWriter(this.fileUrl, true);
			BufferedWriter bufferedWriter = new BufferedWriter(file);
			bufferedWriter.append(System.lineSeparator() + inlineData);
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void getFileContent() {
		// This will reference one line at a time
		String line = null;
		// String inlineData = null;
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(this.fileUrl);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				if (this.fileContent == null)
					this.fileContent = line + "\r\n";
				else
					this.fileContent = this.fileContent + line + "\r\n";
			}
			// Always close files.
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			responseStatus = "HTTP/1.1 404 FILE NOT FOUND\r\n";
			e.printStackTrace();
		} catch (IOException e) {
			responseStatus = "HTTP/1.1 410 FILE READING ERROR\r\n";
			e.printStackTrace();
		}
	}

	public void run() {
		System.out.println("The Client details: InetAddress - " + clientSocket.getInetAddress() + " , port - "
				+ clientSocket.getPort());
		Date d = new Date();
		String requestType;
		String url;
		try {
			BufferedReader inputBufferedReader = new BufferedReader(
					new InputStreamReader(this.clientSocket.getInputStream()));
			String[] request = inputBufferedReader.readLine().trim().split(" ");
			requestType = request[0];
			url = request[1];
			url = url.replace("https://", " ");
			url = url.replace("http://", " ");
			if (!url.contains("/")) {
				url = url + "/";
			}
			url = url.substring(url.indexOf("/"), url.length());

			// Check for unauthorized access
			boolean validation = validateUrl(url);

			// code to read headers line
			// code to read and print headers
			String headerLine = null;
			while ((headerLine = inputBufferedReader.readLine()).length() != 0) {
				if (headerLine.contains("Content-Type")) {
					this.headerContentType = headerLine.trim();
				}
				if (headerLine.contains("Content-Disposition")) {
					this.headerContentDisposition = headerLine.trim();
				}
			}

			// fetch other details from request
			StringBuilder payload = new StringBuilder();
			while (inputBufferedReader.ready()) {
				payload.append((char) inputBufferedReader.read());
			}
			System.out.println("Payload data is: " + payload.toString());

			if (validation) {
				switch (requestType.trim().toUpperCase()) {
				case "GET":
					if (!url.trim().contains(".txt")) {
						listFilesName();
					} else {
						getFileContent();
					}
					break;
				case "POST":
					saveFileContent(payload.toString());
					break;
				}
			}
			response();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Some error occured at Server");
		}
	}

	public static void main(String args[]) throws IOException {

		// set servers parameters
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-v")) {
				debugMode = true;
				break;
			}
			if (args[i].equalsIgnoreCase("-p")) {
				port = Integer.parseInt(args[i + 1]);
				break;
			}
			if (args[i].equalsIgnoreCase("-d")) {
				fileDirectory = args[i + 1];
				break;
			}
		}

		// start server socket
		ServerSocket server = new ServerSocket(port, 10000, InetAddress.getByName("127.0.0.1"));
		System.out.println("Server start listening for connection on port " + port + " ....");

		// start threads for each client connection
		while (true) {
			Socket clientSocket = server.accept();
			if (clientSocket != null) {
				// new thread for each connection
				new Https(clientSocket).start();
			}
		}
	}
}
