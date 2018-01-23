/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;


import arpdetox_lib.IPInfoContainers.SourceIPInfo;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 * @param <T>
 */
public class UDPServer<T extends UDPServer>
{
    
    public static final int MAX_SIZE_PACKET=60;//60 bytes max
    public static final int MAX_MSG_STORED=30;
    public static final int DEFAULT_TIMEOUT_MS=30;//we will timeout after 30 ms by default
    
    protected final static Logger logger=Logger.getLogger(ARPDetox_lib.class.getName());
    
    
    public static class Message_InetSocketAddress_Pair
    {
        private ByteBuffer mess= ByteBuffer.allocate(MAX_SIZE_PACKET);
        private final InetSocketAddress socket_address;
        private boolean has_msg=false;
        private boolean is_full_broadcast=false;
        
        public Message_InetSocketAddress_Pair(InetSocketAddress socket_address_)
        {
            socket_address=socket_address_;
            mess.clear();
        }
        public Message_InetSocketAddress_Pair(Message_InetSocketAddress_Pair other)
        {
            socket_address=other.socket_address;
            setMess(other.mess.array());
            is_full_broadcast=other.is_full_broadcast;
        }

        public ByteBuffer getMess() {
            return mess;
        }

        public void setMess(byte[] data)   throws InvalidParameterException
        {
            if (data.length > mess.capacity())//let's throw instead of putting mess into an unknown state
                throw new InvalidParameterException("Want to fill the msg with too much data");
            mess.clear();
            mess.put(data);
            mess.flip();
            has_msg=true;
        }
        public void setMess(ByteBuffer other)   throws InvalidParameterException
        {
            ByteBuffer data = other.duplicate();
            if (data.limit() > mess.capacity())//let's throw instead of putting mess into an unknown state
                throw new InvalidParameterException("Want to fill the msg with too much data");
            mess.clear();
            mess.put(data);
            mess.flip();
            has_msg=true;
        }

        public void resetMess()
        {
            has_msg=false;
        }

        public String getStringIPAddress() {
            return socket_address.getHostString();
        }

        public InetSocketAddress getSocketAddress()
        {
            return socket_address;
        }
        public int getPort() {
            return socket_address.getPort();
        }
        
        public boolean hasMsg()
        {
            return has_msg;
        }
        
        public boolean getIsFullBroadcast()
        {
            return is_full_broadcast;
        }
        
        public void setFullBroadcast()
        {
            is_full_broadcast=true;
        }
        public void unsetFullBroadcast()
        {
            is_full_broadcast=false;
        }
        
        
    }
    
    public  static abstract class RunnableUDPServerInterface<T extends UDPServer> implements Runnable
    {
        protected T server=null;      
        protected SourceIPInfo src_ip_info=null;
        
        
        @Override
        abstract public void run();         
    
        protected void setServerOnceAndForAll(T server_)
        {
            if(server==null)
            {
                server=server_;                
                src_ip_info=server.getSrc_ip_info();
            }
        }
    }
    
    public static class RunnableProvider<T extends UDPServer> extends RunnableUDPServerInterface<T>
    {
        @Override
        public void run()
        {
            try{
                server.run();
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        
         
    }
    
    
    
    
    
    protected final SourceIPInfo src_ip_info;
    protected final DatagramChannel channel= DatagramChannel.open();
    protected final int port_number;
    protected final Selector selector_read= Selector.open();
    protected final Selector selector_write= Selector.open();
    protected SelectionKey read_key;
    protected SelectionKey write_key;
    
    
    
    //let's setup the queues used by the different threads
    //the queues will be able to hold 30 messages for now
    //and they are "fair" meaning FIFO order for the accessing threads
    protected ArrayBlockingQueue<byte[]> queue_read= new ArrayBlockingQueue<byte[]>(MAX_MSG_STORED,true);
    protected ArrayBlockingQueue<Message_InetSocketAddress_Pair> queue_write= new ArrayBlockingQueue<Message_InetSocketAddress_Pair>(MAX_MSG_STORED,true);
    
    
    protected long number_ms_timeout_read=DEFAULT_TIMEOUT_MS;
    protected long number_ms_timeout_write=DEFAULT_TIMEOUT_MS;
    
    protected final Thread provider_thread;
    protected final Thread consumer_thread;
    
    protected boolean is_running=false;
    
    public ArrayList<Inet4Address> getBroadcastAddressesFromAllInterfaces()
    {
            ArrayList<Inet4Address> res= new ArrayList<Inet4Address>();
        try {
            
            Enumeration<NetworkInterface> physical_network_interfaces = NetworkInterface.getNetworkInterfaces();
            while (physical_network_interfaces.hasMoreElements())
            {
                NetworkInterface ni = physical_network_interfaces.nextElement();
                List<InterfaceAddress> list_interface_address = ni.getInterfaceAddresses();
                for(InterfaceAddress ia : list_interface_address)
                {
                    InetAddress broadcast_address=ia.getBroadcast();
                    if(broadcast_address!=null && broadcast_address.getClass() == Inet4Address.class)
		    {
			if(! broadcast_address.isLoopbackAddress() && ! broadcast_address.isLinkLocalAddress() )
			    res.add((Inet4Address) broadcast_address);
		    }
                }
            }
            
        } catch (SocketException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    
    
    public  UDPServer(final SourceIPInfo src_ip_info_, final RunnableUDPServerInterface<T> consumer_thread_runnable) 
        throws ClosedChannelException, SocketException, IOException ,IllegalArgumentException
    {
        if(src_ip_info_==null || consumer_thread_runnable==null)
            throw new IllegalArgumentException("null src_ip_info_ or consumer_thread_runnable");
        src_ip_info=src_ip_info_;
        port_number=src_ip_info.getPort_src();
        channel.configureBlocking(false);
        //allow broadcasting on our socket
        channel.setOption(StandardSocketOptions.SO_BROADCAST, true);
        
        //old:
        //channel.socket().bind(new InetSocketAddress(src_ip_info_.getIp_src(),port_number));
        // replaced by : 
        channel.socket().bind(new InetSocketAddress(port_number));
            //to be able to receive packets from whatever address
            //the current change was needed for testing
        
        
        //two selectors because the channel will be ready to write most of the time
        //but we usually won't have anything to write most of the time and it
        //might not be ready to read most of the time which is usually what we
        //want to do
        
        // a selector to read data from the socket 
        read_key=channel.register(selector_read, SelectionKey.OP_READ);
        //a second selector to write
        write_key=channel.register(selector_write, SelectionKey.OP_WRITE);
        
        //let's setup the thread that will read & write in the socket 
        RunnableProvider<T> provider_thread_runnable =new RunnableProvider();
        provider_thread_runnable.setServerOnceAndForAll((T)this);
        provider_thread= new Thread(provider_thread_runnable);
        //the cast should not be an issue here
        consumer_thread_runnable.setServerOnceAndForAll((T)this);
        consumer_thread= new Thread(consumer_thread_runnable);
        
    }
    
    
    protected void run() throws IOException
    {
        while (is_running)
        {
            //if we have something waiting to be written, let's write it
            while (!queue_write.isEmpty())
            {
                int a=selector_write.select(number_ms_timeout_write);
                if(a == 0) 
                {
                    //debug info:
                    //System.out.println(getNameIpPort()+":"+"selector_write.select() returned with 0");
                    //channel not writeable we will see if we can read and come
                    //back afterwards
                    break;
                }
                else if(write_key.isWritable())
                {
                    //we just got a handle on the writing channel, let's write
                    Message_InetSocketAddress_Pair mess_to_write=queue_write.poll();
                    if(mess_to_write==null)
                        continue;
                    int bytesSent =0;
                    if(!mess_to_write.getIsFullBroadcast())
		    {
			bytesSent = channel.send(mess_to_write.getMess(), mess_to_write.getSocketAddress());
			if(bytesSent!= mess_to_write.getMess().limit())
			{
			    System.err.println("Nb of bytes written mismatch : bytesSent= "+bytesSent+" position()= "+mess_to_write.getMess().limit());
			}
			else
			    System.out.println(getNameIpPort()+":"+"Message sent to "+mess_to_write.getSocketAddress().getAddress().getHostAddress()+"::"+mess_to_write.getPort());
                
		    }
                    else
                    {
                        ArrayList<Inet4Address> broadcast_addresses=getBroadcastAddressesFromAllInterfaces();
                        for(Inet4Address broadcast_address : broadcast_addresses)
                        {
			    //logger.log(Level.INFO,broadcast_address.toString());
                            bytesSent = channel.send(mess_to_write.getMess(), new InetSocketAddress(broadcast_address,mess_to_write.getPort()));
                            if(bytesSent!= mess_to_write.getMess().limit())
                            {
                                System.err.println("Nb of bytes written mismatch : bytesSent= "+bytesSent+" position()= "+mess_to_write.getMess().limit());
                            } 
			    else
				System.out.println(getNameIpPort()+":"+"Message sent to "+broadcast_address.getHostAddress()+"::"+mess_to_write.getPort());
                        }
                    }
                }
                else
                    break;
            }
            
            //now see if we have something to read on the channel
            //since we will block if the array cannot contain anymore, let's verify
            //we still have room for potential messages as well
            while (selector_read.select(number_ms_timeout_read) != 0 && queue_read.remainingCapacity() >1)
            {
                if(read_key.isReadable())
                {
                    //let's store the content and wait for our read thread to 
                    //pick it up and callback everything       
                    ByteBuffer received=ByteBuffer.allocate(MAX_SIZE_PACKET);
                    //this check doesn't cost us much eve though it should never happen
                    if(channel.receive(received)==null)
                    {
                        System.err.println("The selector told us we had a message pending to be read but nothing is present here");
                        System.err.println("Maybe a race condition lead to there being two provider threads");
                        continue;
                    }
                    received.flip();
                    byte[] r= new byte[received.limit()];
                    received.get(r);
                    //once again this shouldn't happen but you never know...
                    if (!queue_read.offer(r))
                    {
                        System.err.println("The message received was not added to the queue_read even though remainingCapacity()>1 and only one 'provider' thread ");
                        System.err.println("Maybe a race condition lead to there being two provider threads");
                        continue;
                    }
                }
                else
                    break;
            }
            selector_write.selectedKeys().clear();
            selector_read.selectedKeys().clear();
        } 
    }
    
    public void stop()
    {
        is_running=false;
    }

    public void start() throws IOException
    {
        if(is_running)
        {
            System.err.println("tried to launch the UDPserver a second time while it is running");
            return;
        }
        is_running=true;
        provider_thread.start();
        //for now only one thread, we can manage it that way
        consumer_thread.start();
    }
    
    public void send(Message_InetSocketAddress_Pair to_send) throws IOException,InterruptedException
    {
        if(!is_running)
        {
            System.err.println(getNameIpPort()+":"+"tried to send a message while the server is not running");
            return;
        }
        //ATTENTION :will block until it can put the message in the queue
        queue_write.put(to_send);        
    }
    public boolean sendIfPossible(Message_InetSocketAddress_Pair to_send,long timeout_,TimeUnit timeunit_)
    {
        if(!is_running)
        {
            System.err.println(getNameIpPort()+":"+"tried to silently send a message while the server is not running");
            return false;
        }
        //ATTENTION : no garanties it will actually be sent !
        if(timeunit_!=null)
        {
            try {
                    return queue_write.offer(to_send,timeout_,timeunit_);
            } catch (InterruptedException ex) {}
        }
        return queue_write.offer(to_send);
    }
    public boolean sendIfPossible(Message_InetSocketAddress_Pair to_send)
    {
        if(!is_running)
        {
            System.err.println(getNameIpPort()+":"+"tried to silently send a message while the server is not running");
            return false;
        }
        //ATTENTION : no garanties it will actually be sent !
        return queue_write.offer(to_send);
    }
    
    
    
    public void close() throws InterruptedException, IOException
    {
        stop();
        //wait for this thread to end
        provider_thread.join();
        consumer_thread.join();
        selector_read.close();
        selector_write.close();
        
    }
    
    public long getNumber_ms_timeout_read() {
        return number_ms_timeout_read;
    }

    public void setNumber_ms_timeout_read(long number_ms_timeout_read) {
        this.number_ms_timeout_read = number_ms_timeout_read;
    }

    public long getNumber_ms_timeout_write() {
        return number_ms_timeout_write;
    }

    public void setNumber_ms_timeout_write(long number_ms_timeout_write) {
        this.number_ms_timeout_write = number_ms_timeout_write;
    }

    public int getPort_number() {
        return port_number;
    }

    public ArrayBlockingQueue<byte[]> getQueue_read() {
        return queue_read;
    }

    public boolean isRunning() {
        return is_running;
    }
    
    
    public String getNameIpPort()
    {
        try{
            SocketAddress local_addr=channel.getLocalAddress();
            return local_addr.toString();
        }
        catch (IOException e )
        {
            return "Address not found, IOEception";
        }
    }
    
    public SourceIPInfo getSrc_ip_info()
    {
        return src_ip_info;
    }
}
