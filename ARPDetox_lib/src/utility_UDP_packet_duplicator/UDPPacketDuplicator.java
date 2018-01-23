/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utility_UDP_packet_duplicator;

import static arpdetox_lib.UDPServer.MAX_SIZE_PACKET;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used for local test when you want different applications to listen on the same port:
 * Instead of this you bind them on different ports, then create an instance of this class
 * and it will duplicate&forward everything it gets to the designed portS
 * @author will
 */
public class UDPPacketDuplicator {
    //parameters
    protected final int[] dst_ports;
    protected final int src_port;
    
    //backend
    protected final DuplicatorRunnable duplicator_runnnable;
    protected final Thread duplicator_thread;
    
    
    
    public static class DuplicatorRunnable implements Runnable
    {
        
        //backend
        protected boolean is_running=false;
        protected final InetSocketAddress[] dst_sockets;
        final int dst_length;
        
        protected final DatagramChannel channel= DatagramChannel.open();
        protected final Selector selector_read= Selector.open();
        protected final SelectionKey read_key;
        
        
        public DuplicatorRunnable( final int src_port_,final int[] dst_ports_)
        throws ClosedChannelException, SocketException, IOException ,IllegalArgumentException
        {
            dst_length=dst_ports_.length;
            dst_sockets= new InetSocketAddress[dst_length];
            for(int i=0;i< dst_length;i++)
            {
                dst_sockets[i]=new InetSocketAddress("127.0.0.1",dst_ports_[i]);
            }
        
            //backend
            channel.socket().bind(new InetSocketAddress(src_port_));
            read_key=channel.register(selector_read, SelectionKey.OP_READ);
        }
        
        @Override
        public void run() 
        {
            is_running=true;
            while(is_running)
            {
                try 
                {
                    //read a message
                    selector_read.select();
                    if(read_key.isReadable())
                    {
                        ByteBuffer received=ByteBuffer.allocate(MAX_SIZE_PACKET);
                        if(channel.receive(received)==null)
                        {
                            System.err.println("The selector told us we had a message pending to be read but nothing is present here");
                            continue;
                        }
                        received.flip();

                        //let's send it to all the dst ports
                        for(int i=0; i< dst_length;i++)
                        {
                            int bytesSent = channel.send(received, dst_sockets[i]);
                            if(bytesSent!= received.limit())
                            {
                                System.err.println("Nb of bytes written mismatch : bytesSent= "+bytesSent+" position()= "+received.limit());
                            }

                        }
                    }
                    selector_read.selectedKeys().clear();


                } catch (IOException ex) {
                    //Logger.getLogger(UDPPacketDuplicator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    
    public UDPPacketDuplicator( final int src_port_,final int[] dst_ports_)
        throws ClosedChannelException, SocketException, IOException ,IllegalArgumentException
    {
        //the params
        int dst_length=dst_ports_.length;
        src_port=src_port_;
        dst_ports= new int[dst_length];
        System.arraycopy(dst_ports_, 0, dst_ports, 0, dst_length);
        
        duplicator_runnnable= new DuplicatorRunnable(src_port_,dst_ports_);
        duplicator_thread=new Thread(duplicator_runnnable);
        
    }
    
    public void start()
    {
        if(duplicator_runnnable.is_running)
        {
            System.err.println("tried to launch the UDPPacketDuplicator a second time while it is running");
            return;
        }
        duplicator_runnnable.is_running=true;
        duplicator_thread.start();
    }
    public void stop()
    {
        duplicator_runnnable.is_running=false;
    }
    
    
}
