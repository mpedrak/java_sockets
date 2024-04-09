import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

public class ServerTcpThread extends Thread 
{
    private int maxClients;
    private int myIndex;
    private int[] clientIds;
    private PrintWriter[] clientWriters;
    private Socket[] clientSockets;
    private ReentrantLock mutex;
    
    public ServerTcpThread(int maxClients, int myIndex, int[] clientIds, PrintWriter[] clientWriters, Socket[] clientSockets, ReentrantLock mutex)
    {
        this.maxClients = maxClients;
        this.myIndex = myIndex; 
        this.clientIds = clientIds; 
        this.clientWriters = clientWriters;
        this.clientSockets = clientSockets;
        this.mutex = mutex;
    }

    public void run()
    {
        try 
        {
            System.out.println("Client " + clientIds[myIndex] + " connected.");
            
            Socket clientSocket = clientSockets[myIndex];
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            while (true)
            {
                if (reader.ready())
                {
                    String msg = reader.readLine();
                    if (msg.equals("STOP"))
                    {
                        mutex.lock();
                        System.out.println("Client " + clientIds[myIndex] + " disconnected.");
                        clientIds[myIndex] = -1;
                        mutex.unlock();
                        break;
                    }   

                    mutex.lock();
                    msg = "TCP msg from client " + clientIds[myIndex] + " : " + msg;  
                    for (int i = 0; i < maxClients; i++)
                    {
                        if (i != myIndex && clientIds[i] != -1) clientWriters[i].println(msg);
                    }
                    mutex.unlock();
                }   
            }
        }
        catch(Exception e)
        {
            e.printStackTrace(); 
        }
    }
}
