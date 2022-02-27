package main;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class Server
{
	private boolean isRunning = false;
	
	private String securityKey = "";
	private Thread recv;
	private Server self;
	private Runnable listener;
	private ServerSocket sSock;
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
		
		this.recv = new Thread(this.listener, "Connection Receptionist");
	}
	
	public void sendAllClient(String content)
	{
		for (ServerThread itor: this.activeConnections) itor.sendMessage(content);
	}
	
	public void startServer()
	{
		this.isRunning = true;
		this.recv.start();
	}
	
	public void stopServer() { this.isRunning = false; }
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