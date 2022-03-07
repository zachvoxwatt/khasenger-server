package main;

import java.awt.EventQueue;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import res.ClientUser;
import res.Message;

public class Server
{
	private boolean mainThreadRunning = false, consoleInputRunning = false;
	
	private String securityKey = "";
	private Scanner scr;
	private Thread recv, cmdListener;
	private Server self;
	private Runnable listener, consoleInput, clientDisconnect, serverStopper;
	private ServerSocket sSock;
	private List<ServerThread> activeConnections;
	private Map<Socket, ClientUser> usersMap;
	private Queue<Message> msgQueue;
	private ScheduledExecutorService dcpus;
	
	public Server()
	{
		this.securityKey = "a";
		this.self = this;
		this.dcpus = Executors.newScheduledThreadPool(5, new ThreadNames());
		this.activeConnections = new ArrayList<>();
		this.msgQueue = new LinkedList<>();
		this.usersMap = new HashMap<>();
		try { this.sSock = new ServerSocket(1765); } 
		
		catch (Exception e) 
		{ 
			System.out.println("Another instance of the server is running! Exiting this"); 
			System.exit(0); 
		}
		
		this.listener = new Runnable()
		{
			public void run()
			{
				System.out.println("**Khasenger Dedicated Server by ZachWattz**\nUp and running at port 1765\n-------------------------------------------\n\n");
				while (mainThreadRunning)
				{
					try
					{
						Socket cSock = sSock.accept();
						if (mainThreadRunning)
						{
							System.out.printf("[Server / INFO] Client @ %s has connected\n", cSock.getInetAddress().getHostAddress());
							ServerThread sThrd = new ServerThread(self, cSock);
							
							activeConnections.add(sThrd);
							dcpus.execute(sThrd);
						}
					}
					catch (Exception e) { e.printStackTrace(); }
				}
			}
		};
		
		this.consoleInput = new Runnable()
		{
			public void run()
			{
				while (consoleInputRunning)
				{
					String cmd = scr.nextLine();
					
					switch (cmd)
					{
						case "exit":
							consoleInputRunning = false;
							mainThreadRunning = false;
							dcpus.schedule(clientDisconnect, 500, TimeUnit.MILLISECONDS);
							break;
							
						case "list users":
							if (usersMap.size() != 0)
							{
								if (usersMap.size() == 1)
									System.out.printf("\t-->There is 1 user connected:\n%s", toStringListOnlineUsers());
								else 
									System.out.printf("\t-->There are %d users connected:\n%s", usersMap.size(), toStringListOnlineUsers());
							}
							else System.out.printf("\t-->There is no user connected at the moment\n\n");
							break;
					}
				}
			}
		};
		
		this.clientDisconnect = new Runnable()
		{
			public void run()
			{
				System.out.printf("\n[Server / INFO] Shutdown protocol started\n");
				for (ServerThread itor: activeConnections) itor.terminateConnection();
				dcpus.schedule(serverStopper, 2, TimeUnit.SECONDS);
			}
		};
		
		this.serverStopper = new Runnable()
		{
			public void run()
			{
				securityKey = null;
				scr.close();
				sSock = null;
				dcpus.shutdown();
				activeConnections.clear();
				usersMap.clear();
				self = null;
				System.out.printf("[Server / INFO] Shutdown completed. Exiting...\n");
				System.gc();
				System.exit(0);
			}
		};
		
		this.scr = new Scanner(System.in);
		
		this.recv = new Thread(this.listener, "Connection Receptionist");
		this.cmdListener = new Thread(this.consoleInput, "Console Input Listener");
		
		startServer();
	}
	
	public void startServer()
	{
		this.mainThreadRunning = true;
		this.consoleInputRunning = true;
		this.recv.start();
		this.cmdListener.start();
	}
	public void sendAllClient(String content) 
	{ 
		for (ServerThread itor: this.activeConnections) 
			itor.sendMessage(content);
		
		pushMessage(content);
	}
	public void pushMessage(String text) { this.msgQueue.add(new Message(text)); }
	public void notifyJoin(String content)
	{ 
		for (ServerThread itor: this.activeConnections) 
			itor.notifyJoin(content);
		
		pushMessage(content);
	}
	
	public void notifyLeave(String content, ServerThread sender) 
	{ 
		if (this.activeConnections.size() != 1)
			for (ServerThread itor: this.activeConnections)
				if (!itor.equals(sender)) itor.notifyLeave(content); 
		pushMessage(content);
	}
	
	public void terminate(ServerThread svThrd) { this.activeConnections.remove(svThrd); }
	
	public boolean validateName(String givenName)
	{
		boolean success = true;
		if (this.usersMap.size() == 0) return success;
		
		else
		{
			for (Map.Entry<Socket, ClientUser> itor: this.usersMap.entrySet())
				if (itor.getValue().getName().equals(givenName)) { success = false; break; }
		}
		return success;
	}
	
	public void addToMap(Socket sck, String name)
	{
		ClientUser clusr = new ClientUser(name, createUUID());
		this.usersMap.put(sck, clusr);
	}
	
	public void removeFromMap(Socket sck) { this.usersMap.remove(sck); }
	
	public String toStringListOnlineUsers()
	{
		String returner = "";
		
		for (Map.Entry<Socket, ClientUser> itor: this.usersMap.entrySet())
			returner += "  \t\t- " + itor.getValue().getName() + "\n";
		
		returner += "\n\tEnd of Report\n\n";
		return returner;
	}
	
	protected UUID createUUID() { return UUID.randomUUID(); }
	public String getSecKey() { return this.securityKey; }
	public Queue<Message> getMessageQueue() { return this.msgQueue; }
	public Map<Socket, ClientUser> getUserMap() { return this.usersMap; }
	public ScheduledExecutorService requestDCPUs() { return this.dcpus; }
	
	class ThreadNames implements ThreadFactory
	{
		private int count = 0;
		@Override
		public Thread newThread(Runnable r) 
		{
			String name = "Operative Thread #" + ++count;
			return new Thread(r, name);
		}
	}
	
	/*
	public static void main(String[] args) 
	{ 
		EventQueue.invokeLater(() 
		->	{
				Server sv = new Server(args[0]);
					sv.startServer();
			}
		);
	}
	*/
	public static void main(String[] args) { EventQueue.invokeLater(Server::new); }
	
}