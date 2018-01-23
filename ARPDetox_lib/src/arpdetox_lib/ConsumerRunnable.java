/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import static arpdetox_lib.ARPDMessage.bytesToHex;
import static arpdetox_lib.ARPDMessage.getMsgTypeFromBytes;
import arpdetox_lib.UDPServer.RunnableUDPServerInterface;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 * @param <T>
 */
public class ConsumerRunnable<T extends UDPServer> extends RunnableUDPServerInterface<T>
{
    protected static int TIMEOUT_CONSUMER_THREAD=UDPServer.DEFAULT_TIMEOUT_MS*2;
    protected  TimeUnit TIMEOUT_UNIT=TimeUnit.MILLISECONDS;
    protected final static Logger logger=Logger.getLogger(ARPDetox_lib.class.getName());
    
    @Override
    public void run()
    {
        //read messages and callback()
        ArrayBlockingQueue<byte[]> queue=server.getQueue_read();
        while(server.isRunning())
        {
            try
            {
                byte[] buff=queue.poll(TIMEOUT_CONSUMER_THREAD, TIMEOUT_UNIT);
                if(buff==null || buff.length<1)
                    continue;
                callback(buff);
            }catch(InterruptedException ie){}
        }
    }
    
    public void callback(byte[] buff)
    {
        if(buff==null || buff.length<1)
                return;
        System.out.println(server.getNameIpPort()+" received:"+new String(buff));
    }
    
    
    public static void dumpMsg(byte[] bytes_received)
    {
         if(bytes_received==null ||  bytes_received.length<1)
        {
            logger.log(Level.WARNING, "Null or empty message received");
            return;
        }
        //Try to cast it as an ARPDMessage
        try
        {
            ARPDMessage.ARPD_MESSAGE_TYPE type_sent=getMsgTypeFromBytes(bytes_received);
            ARPDMessage mess=ARPDMessage.fromBytes(type_sent,bytes_received);
            logger.log(Level.WARNING, "Could not handle the following ARPDMessage message :\n{0}", mess.toString(0));
        } catch (UnknownHostException | InvalidParameterException ex) 
        {
            logger.log(Level.WARNING, "Could not cast the following as ARPDMessage, dumping :\n{0}\nReason:\n", bytesToHex(bytes_received));
            logger.log(Level.WARNING, null, ex);
        }
    }
}
