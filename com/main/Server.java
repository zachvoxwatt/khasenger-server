package main;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class Server
{
	private boolean isRunning = false;
	
	private String securityKey = "";
	private Thread recv, cmdListener;
	private Server self;
	private Runnable listener, consoleInput;
	private ServerSocket sSock;
	private Scanner scr;
	private ScheduledExecutorService dcpus;
	private List<ServerThread> activeConnections;
	
	public Server(String seckey)
	{
		this.securityKey = seckey;
		this.self = this;
		this.dcpus = Executors.newScheduledThreadPool(10, new ThreadNames());
		this.activeConnections = new ArrayList<>();
		try { this.sSock = new ServerSocket(1765); } catch (IOException e) { e.printStackTrace(); }
		
		this.listener = new Runnable()
		{
			public void run()
			{
				System.out.println("Server is OPERATIONAL and running at port 1765!");
				while (isRunning)
				{
					try
					{
						Socket cSock = sSock.accept();
						System.out.printf("[Server / Incoming] Client at %s has connected\n\n", cSock.getInetAddress().getHostAddress());
						ServerThread sThrd = new ServerThread(self, cSock);
						
						activeConnections.add(sThrd);
						dcpus.execute(sThrd);
					}
					catch (Exception e) { e.printStackTrace(); }
				}
			}
		};
		
		this.consoleInput = new Runnable()
		{
			public void run()
			{
				while (isRunning)
				{
					System.out.print(">");
					String cmd = scr.nextLine();
					
					switch (cmd)
					{
						case "exit":
							stopServer();
							break;
							
						case "list users":
							System.out.printf("Here are list of users...\n\n");
							break;
					}
				}
			}
		};
		
		this.scr = new Scanner(System.in);
		
		this.recv = new Thread(this.listener, "Connection Receptionist");
		this.cmdListener = new Thread(this.consoleInput, "Console Input Listener");
	}
	
	public void sendAllClient(String content)
	{
		if (this.activeConnections.size() != 1) for (ServerThread itor: this.activeConnections) itor.sendMessage(content);
	}
	
	public void startServer()
	{
		this.isRunning = true;
		this.recv.start();
		this.cmdListener.start();
	}
	
	public void stopServer() 
	{
		System.out.printf("\n[Server / Info] Shutdown protocol started\n\n");
		this.isRunning = false;
		this.sSock = null;
		for (ServerThread itor: this.activeConnections) itor.requestClientTerminateConnection();
		this.dcpus.shutdownNow();
		this.scr.close();
		this.securityKey = null;
		System.out.println("[Server / Info] Shutdown complete. Exiting...\n\n");
		System.exit(0);
	}
	public void terminate(ServerThread svThrd) { this.activeConnections.remove(svThrd); }
	
	public String getSecKey() { return this.securityKey; }
	
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
	
	public static void main(String[] args) 
	{ 
		EventQueue.invokeLater(() 
		->	{
				Server sv = new Server(args[0]);
					sv.startServer();
			}
		);
	}
}