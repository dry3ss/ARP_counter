/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpd_test_slave;

import arpdetox_lib.*;
import static arpdetox_lib.ARPDMessage.getAllRemainingBytesFromByteBuffer;
import static arpdetox_lib.ARPDServer.ARDP_SLAVE_PORT;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.logging.Level;
/**
 *
 * @author will
 */
public class ARPD_test_slave {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            
            byte[] password= "lala".getBytes(); 
            
            
            int common_port_slaves=ARDP_SLAVE_PORT;
            
            String my_addr="192.168.20.11";
            
            IPInfoContainers.SourceIPInfo s1_src_info= new IPInfoContainers.SourceIPInfo(my_addr,common_port_slaves);
            
            IPInfoContainers.DestIPInfo s1_dst_info = new IPInfoContainers.DestIPInfo(my_addr,common_port_slaves);
            
            
            ARPDSlaveConsumerRunnable cr1= new ARPDSlaveConsumerRunnable();
            
            ARPDServer.ARPDServerSlave s1= new ARPDServer.ARPDServerSlave(s1_src_info,cr1);
            
            s1.setPasswd(password);
            System.out.println(System.currentTimeMillis());
            s1.start();
            
            System.out.println("servers started");
            boolean a=true;
            while(a)
            {
                Thread.sleep(30);
            }
            
            Thread.sleep(1000);
            
            s1.close();
            
            
        }catch(IOException | InterruptedException | InvalidParameterException e)
        {
            e.printStackTrace();
        }
    }
    
     public static void maina(String[] args) {
    try {
            
            //ARPDLoggers.action_logger.log(Level.FINEST,"test finest");
            //ARPDLoggers.message_logger.log("test1");
            byte[] password= "lala".getBytes(); 
            
            
            int common_port_slaves=ARDP_SLAVE_PORT;
            
            String addr_slave_1="192.168.20.11";
                
            IPInfoContainers.SourceIPInfo s1_src_info= new IPInfoContainers.SourceIPInfo(addr_slave_1,common_port_slaves);
            
            IPInfoContainers.DestIPInfo s1_dst_info = new IPInfoContainers.DestIPInfo(addr_slave_1,common_port_slaves);
            
            
            ARPDSlaveConsumerRunnable cr1= new ARPDSlaveConsumerRunnable();
            
            
            //ATTENTION : pour tester jai été forcé de changer le constructeur de UDPServer
            //de manière à ne bind qu'avec l'adresse donnée par sX_src_info
            //à la palce de nimporte quelle adresse
            //Il faudra re CHANGER ça car dans le cas du MASTER les paquets viennent
            //de plusieurs sous réseaux différents donc avec des addresses différentes
            ARPDServer.ARPDServerSlave s1= new ARPDServer.ARPDServerSlave(s1_src_info,cr1);
            
            s1.setPasswd(password);
            System.out.println(System.currentTimeMillis());
            s1.start();
            
            System.out.println("servers started");
            boolean a=true;
            while(a)
            {
                Thread.sleep(30);
            }
            
            Thread.sleep(1000);
            
            s1.close();
            
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
