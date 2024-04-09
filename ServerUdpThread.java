import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class ServerUdpThread extends Thread 
{
    private int maxClients;
    private int[] clientIds;
    private Socket[] clientSockets;
    private ReentrantLock mutex;
    private DatagramSocket serverUdpSocket;
    
    public ServerUdpThread(int maxClients, int[] clientIds, Socket[] clientSockets, ReentrantLock mutex, DatagramSocket serverUdpSocket)
    {
        this.maxClients = maxClients;
        this.clientIds = clientIds; 
        this.clientSockets = clientSockets;
        this.mutex = mutex;
        this.serverUdpSocket = serverUdpSocket;
    }

    public void run()
    {
        try 
        {
            byte[] receiveBuffer = new byte[2048];
            InetAddress address = InetAddress.getByName("localhost");

            while (true)
            {
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverUdpSocket.receive(receivePacket);
                String msg = new String(receivePacket.getData());

                mutex.lock(); 
                int senderIndex = 0;
                while (senderIndex < maxClients)
                {
                    if (clientIds[senderIndex] != -1 && clientSockets[senderIndex].getPort() == receivePacket.getPort()) break; 
                    senderIndex++;
                }

                msg = "UDP msg from client " + clientIds[senderIndex] + msg;
                byte[] sendBuffer = msg.getBytes();
                for (int i = 0; i < maxClients; i++)
                {
                    if (i != senderIndex && clientIds[i] != -1)
                    {
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, clientSockets[i].getPort());
                        serverUdpSocket.send(sendPacket);
                    }
                }
                mutex.unlock();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace(); 
        }
        
    }
}
