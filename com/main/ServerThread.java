package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
public class ServerThread implements Runnable
{
	private boolean isActive = true;
	
	private String clientIP, clientInet;
	private Socket cSock;
	private Server sv;
	private DataOutputStream sendToClient;
	private DataInputStream getFromClient;
	
	public ServerThread(Server srvr, Socket cSock)
	{
		this.sv = srvr;
		this.cSock = cSock;
		this.clientIP = this.cSock.getInetAddress().getHostAddress();
		this.clientInet = this.cSock.getInetAddress().toString();
		
		try
		{
			this.sendToClient = new DataOutputStream(this.cSock.getOutputStream());
			this.getFromClient = new DataInputStream(this.cSock.getInputStream());
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	@Override
	public void run() 
	{
		try
		{
			while (this.isActive)
			{
				String msgType = this.getFromClient.readUTF();
				
				switch (msgType)
				{
					case "requestAuth":
						String givenKey = this.getFromClient.readUTF();
						
						String authProgressLog = String.format("[Server / INFO] Client %s @ %s is authenticating...\n", this.clientInet, this.clientIP);
						logConsole(authProgressLog);
								
						if (givenKey.equals(this.sv.getSecKey()))
						{
							this.sendToClient.writeUTF("accepted");
							this.sendToClient.flush();
							
							String authStatusLog = String.format("[Server / INFO] Client %s @ %s successfully authenticated\n", this.clientInet, this.clientIP);
							logConsole(authStatusLog);
						}
						
						else
						{
							this.sendToClient.writeUTF("unauthorized");
							this.sendToClient.flush();
							
							String authStatusLog = String.format("[Server / INFO] Client %s @ %s failed authentication. Reason: Invalid Security Key '%s'\n", this.clientInet, this.clientIP, givenKey);
							logConsole(authStatusLog);
							
							this.isActive = false;
						}
						break;
						
					case "requestValidateName":
						String sampleName = this.getFromClient.readUTF();
						
						String nameValidateLog = String.format("[Server / INFO] User '%s' of client @ %s requested for name validation\n", sampleName, this.clientIP);
						logConsole(nameValidateLog);
						
						if (this.sv.validateName(sampleName))
						{
							this.sendToClient.writeUTF("accepted");
							this.sendToClient.flush();
							this.sv.addToMap(this.cSock, sampleName);
							
							String nameValidateStatusLog = String.format("[Server / INFO] Server accepted name '%s' for client @ %s\n", sampleName, this.clientIP);
							logConsole(nameValidateStatusLog);
							
							String welcomeMsg = String.format("\t>>> %s has joined!\n\n", sampleName);
							this.sv.notifyJoin(welcomeMsg);
						}
						
						else
						{
							this.sendToClient.writeUTF("unavailable");
							this.sendToClient.flush();
							
							String nameValidateStatusLog = String.format("[Server / INFO] Server rejected name '%s' for client @ %s - Reason: Name was taken\n", sampleName, this.clientIP);
							logConsole(nameValidateStatusLog);
							
							this.isActive = false;
						}
						break;
						
					case "requestPostMSG":
						String comingText = this.getFromClient.readUTF();
						
						String postMessageRequest = String.format("[Server / INFO] Client %s @ %s requested to post a message\n", this.clientInet, this.clientIP);
						logConsole(postMessageRequest);
						
						this.sv.sendAllClient(comingText);
						break;
						
					case "ping":
						this.sendToClient.writeUTF("pong");
						
						String iAmAlive = String.format("[Server / INFO] Client %s @ %s pinged server. Replying with a 'pong'...\n", this.clientInet, this.clientIP);
						logConsole(iAmAlive);
						
						break;
						
					case "userLeave":
						String leavingName = this.getFromClient.readUTF();
						String leavingMsg = String.format("\t>>> %s left\n\n", leavingName);
						this.sv.notifyLeave(leavingMsg, this);
						this.sv.removeFromMap(this.cSock);
						
						String onLeave = String.format("[Server / INFO] Client %s @ %s disconnected\n", this.clientInet, this.clientIP);
						logConsole(onLeave);
						
						this.isActive = false;
						break;
						
					case "userLeaveUnexpected":
						String unexLeavingName = this.getFromClient.readUTF();
						String unexLeavingMsg = String.format("\t>>> %s left unexpectedly\n\n", unexLeavingName);
						this.sv.notifyLeave(unexLeavingMsg, this);
						this.sv.removeFromMap(this.cSock);
						
						String onAbruptLeave = String.format("[Server / INFO] Client %s @ %s disconnected unexpectedly\n", this.clientInet, this.clientIP);
						logConsole(onAbruptLeave);
						
						this.isActive = false;
						break;
						
					case "confirmShutdown":
						this.sv.removeFromMap(this.cSock);
						this.isActive = false;
						break;
				}
			}
			
			closeConnection();
		}
		catch (Exception e) {}
	}
	
	void closeConnection()
	{
		try
		{
			this.sv.terminate(this);
			
			this.cSock.close();
			this.getFromClient.close();
			this.sendToClient.close();

			this.cSock = null;
			this.getFromClient = null;
			this.sendToClient = null;
			
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public void sendMessage(String text)
	{
		try
		{
			this.sendToClient.writeUTF("newMessage");
			this.sendToClient.writeUTF(text);
			this.sendToClient.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public void notifyJoin(String text)
	{
		try
		{
			this.sendToClient.writeUTF("newJoinedUser");
			this.sendToClient.writeUTF(text);
			this.sendToClient.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public void notifyLeave(String text)
	{
		try
		{
			this.sendToClient.writeUTF("userLeaving");
			this.sendToClient.writeUTF(text);
			this.sendToClient.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public void terminateConnection()
	{
		try
		{
			this.sendToClient.writeUTF("svShutdown");
			this.sendToClient.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	void logConsole(String s) { System.out.printf(s); }
	
	public boolean isActive() { return this.isActive; }
}