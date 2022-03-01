package main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ServerThread implements Runnable
{
	private boolean isActive = true;
	
	private Socket cSock;
	private Server sv;
	private DataOutputStream sendToClient;
	private DataInputStream getFromClient;
	
	public ServerThread(Server srvr, Socket cSock)
	{
		this.cSock = cSock;
		this.sv = srvr;
		
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
				byte b = this.getFromClient.readByte();
				
				switch (b)
				{
				//authentication request
					case 1:
						String sec = this.getFromClient.readUTF();
						String name = this.getFromClient.readUTF();
						String info = String.format("[Server / AUTH] Client @ %s is authenticating...\n", this.cSock.getInetAddress().getHostAddress());
						logConsole(info);
						
						if (sec.equals(this.sv.getSecKey()))
						{
							this.sendToClient.writeByte(7);
							this.sendToClient.flush();
							String success = String.format("[Server / AUTH] Client @ %s authenticated successfully\n", this.cSock.getInetAddress().getHostAddress());
							logConsole(success);
							
							String announce = String.format("\t>>>> %s has joined the chat!\n\n", name);
							this.sv.sendAllClient(announce);
							break;
						}
						else
						{
							this.sendToClient.writeByte(-1);
							String fail = String.format("[Server / AUTH] Client @ %s failed authentication. Invalid Token: '%s'\n", this.cSock.getInetAddress().getHostAddress(), sec);
							logConsole(fail);
						}
						break;
						
				//'i am still alive' request
					case 2:
						this.sendToClient.writeByte(20);
						this.sendToClient.flush();
						
						String still_live = String.format("[Server / INFO] Client @ %s told that they are still alive. Accepting request...\n", this.cSock.getInetAddress().getHostAddress());
						logConsole(still_live);
						
						break;
						
				//post message request
					case 17:
						String sender = this.getFromClient.readUTF();
						String content = this.getFromClient.readUTF();
						
						String sendinfo = String.format("[Server / INFO] User '%s' of client @ %s posted a message\n", sender, this.cSock.getInetAddress().getHostAddress());
						logConsole(sendinfo);
						this.sv.sendAllClient(content);
						break;
						
				//departure request
					case -69:
						String username = this.getFromClient.readUTF();
						String onleaveconsole = String.format("[Server / INFO] User '%s' of client @ %s has left the chat...\n", username, this.cSock.getInetAddress().getHostAddress());
						logConsole(onleaveconsole);
						break;
						
				//unexpect departure request
					case -127:
						String onleftconsole = String.format("[Server / INFO] Client @ %s has left the chat...\n", this.cSock.getInetAddress().getHostAddress());
						logConsole(onleftconsole);
						this.isActive = false;
						break;
						
				//disconnect request
					default:
						String leaveName = this.getFromClient.readUTF();
						String dis = String.format("[Server / INFO] Client @ %s disconnected\n", this.cSock.getInetAddress().getHostAddress());
						logConsole(dis);
						
						String onleave = String.format("\t>>>> %s has left the chat\n\n", leaveName);
						this.sv.notifyLeaveAllClient(onleave);
						
						this.isActive = false;
						break;
				}
			}
			
			closeConnection();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	void closeConnection()
	{
		try
		{
			this.isActive = false;
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
	
	public void requestClientTerminateConnection()
	{
		try
		{
			this.sendToClient.writeByte(-127);
			this.sendToClient.flush();
		}
		
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public void sendMessage(String text)
	{
		try
		{
			this.sendToClient.writeByte(71);
			this.sendToClient.writeUTF(text);
			this.sendToClient.flush();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	void logConsole(String s) { System.out.printf(s); }
}