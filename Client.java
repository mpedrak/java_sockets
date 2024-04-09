import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Client 
{

    static volatile boolean running = true;

    public static void main(String[] args)
    {
        System.out.println("Client started.");
        running = true;

        int serverPort = 12345;
        int multicastPort = 15432;
        String multicastAddress = "228.5.6.7";

        Socket tcpSocket = null;
        DatagramSocket udpSocket = null;
        InetSocketAddress multicastGroup = null;
        MulticastSocket multicastSocket = null;
        PrintWriter socketWriter = null;
        boolean serverStopped = false;
           
        try 
        {
            tcpSocket = new Socket("localhost", serverPort);
            udpSocket = new DatagramSocket(tcpSocket.getLocalPort());
            udpSocket.setSoTimeout(1);

            multicastGroup = new InetSocketAddress(multicastAddress, multicastPort);
            multicastSocket = new MulticastSocket(multicastPort);
            multicastSocket.joinGroup(multicastGroup, multicastSocket.getNetworkInterface());
            multicastSocket.setSoTimeout(1);
            
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

            socketWriter = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in, System.console().charset()));
            
            InetAddress address = InetAddress.getByName("localhost");
            byte[] receiveBuffer = new byte[2048];

            String multimediaData = new String("\n");
            try
            {
                File file = new File("multimedia_data.txt");
                Scanner fileReader = new Scanner(file, StandardCharsets.UTF_8);
                while (fileReader.hasNextLine()) multimediaData += fileReader.nextLine() + '\n';
                fileReader.close();
            } 
            catch (Exception e) 
            {
                System.out.println("Error while reading multimedia data file.");
                e.printStackTrace();
            }

            while (running)
            {
                if (consoleReader.ready()) 
                {
                    String msg = consoleReader.readLine();
                    if (msg == null) continue;

                    if (msg.equals("STOP")) break;
                   
                    if (msg.equals("U"))
                    {
                        byte[] sendBuffer = multimediaData.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, serverPort);
                        udpSocket.send(sendPacket);
                    }
                    else if (msg.equals("M"))
                    {
                        byte[] sendBuffer = multimediaData.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, InetAddress.getByName(multicastAddress), multicastPort);
                        multicastSocket.leaveGroup(multicastGroup, multicastSocket.getNetworkInterface());
                        udpSocket.send(sendPacket);
                        multicastSocket.joinGroup(multicastGroup, multicastSocket.getNetworkInterface());
                    }
                    else socketWriter.println(msg);
                }

                if (socketReader.ready())
                {
                    String msg = socketReader.readLine();
                    if (msg.equals("STOP")) 
                    {
                        serverStopped = true;
                        break;
                    }
                    System.out.println(msg);
                }

                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try { udpSocket.receive(receivePacket); } catch (Exception e) { ; /* non-blocking */ }
                if (receivePacket.getData()[0] != 0) System.out.println(new String(receivePacket.getData()));  
        
                Arrays.fill(receiveBuffer, (byte)0);
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try { multicastSocket.receive(receivePacket); } catch (Exception e) { ; /* non-blocking */ }
                if (receivePacket.getData()[0] != 0) System.out.println("Multicast msg" + new String(receivePacket.getData()));  
            }  
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        } 
        finally 
        {
            if (tcpSocket != null)
            {
                try 
                { 
                    if (serverStopped) System.out.println("Client and server stopped.");
                    else
                    {
                        System.out.println("Client stopped.");
                        if (socketWriter == null) socketWriter = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                        socketWriter.println("STOP");
                    }
                    
                    tcpSocket.close(); 
                    udpSocket.close();
                    multicastSocket.leaveGroup(multicastGroup, multicastSocket.getNetworkInterface());
                    multicastSocket.close();
                }
                catch (Exception e) 
                {
                    e.printStackTrace();
                }   
            }
        }
    }

}
