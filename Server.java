import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

public class Server 
{
    
    static volatile Boolean running = true;
    
    public static void main(String[] args)
    {
        System.out.println("Server started.");
        running = true;

        int serverPort = 12345;
        int maxClients = 20;
        int newClientId = 0;
    
        Socket[] clientSockets = new Socket[maxClients];
        ReentrantLock mutex = new ReentrantLock();
        PrintWriter[] clientWriters = new PrintWriter[maxClients];
        int[] clientIds = new int[maxClients];
        for (int i = 0; i < maxClients; i++) clientIds[i] = -1;
       
        ServerSocket serverTcpSocket = null; 
        DatagramSocket serverUdpSocket = null;
        
        try 
        {
            serverTcpSocket = new ServerSocket(serverPort);
            serverTcpSocket.setSoTimeout(1);
            serverUdpSocket = new DatagramSocket(serverPort);   
            
            ServerUdpThread serverUdpThread = new ServerUdpThread(maxClients, clientIds, clientSockets, mutex, serverUdpSocket);
            serverUdpThread.setDaemon(true);
            serverUdpThread.start();

            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread() 
            {
                public void run() 
                {
                    running = false;
                    try 
                    { 
                        mainThread.join();
                    }
                    catch (Exception e) 
                    {
                        e.printStackTrace();
                    } 
                }
            });

            while(running)
            {
                Socket clientSocket = null;
                try { clientSocket = serverTcpSocket.accept(); } catch (Exception e) { ; /* non-blocking */ }
                if (clientSocket == null) continue;
 
                mutex.lock();
                int i = 0;
                while (i < maxClients && clientIds[i] != -1) i++;
                
                if (i == maxClients)
                {
                    System.out.println("Cannot serve more concurrent clients.");
                    PrintWriter clientWriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                    clientWriter.println("STOP");
                    mutex.unlock();
                    continue;
                }
                
                clientSockets[i] = clientSocket;
                clientIds[i] = newClientId;
                newClientId++;
                clientWriters[i] = new PrintWriter(new OutputStreamWriter(clientSockets[i].getOutputStream(), StandardCharsets.UTF_8), true);

                ServerTcpThread ServerTcpThread = new ServerTcpThread(maxClients, i, clientIds, clientWriters, clientSockets, mutex);
                ServerTcpThread.setDaemon(true);
                ServerTcpThread.start();
                mutex.unlock();
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            if (serverTcpSocket != null)
            {
                try 
                { 
                    System.out.println("Server stopped.");

                    mutex.lock();
                    String msg = "STOP";
                    for (int i = 0; i < maxClients; i++)
                    {
                        if (clientIds[i] != -1) clientWriters[i].println(msg);
                    }
                    mutex.unlock();

                    serverTcpSocket.close(); 
                    serverUdpSocket.close();
                }
                catch (Exception e) 
                {
                    e.printStackTrace();
                } 
            }
        }
    }

}
