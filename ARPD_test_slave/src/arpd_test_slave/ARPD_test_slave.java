/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpd_test_slave;

import arpdetox_lib.*;
import static arpdetox_lib.ARPDServer.ARDP_SLAVE_PORT;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author will
 */
public class ARPD_test_slave {

    
    public static Inet4Address getMyIPv4AddressIfOnlyOneConnectedInterface()
    {
        Inet4Address r=null;
        try {

            Enumeration<NetworkInterface> physical_network_interfaces = NetworkInterface.getNetworkInterfaces();
            while (physical_network_interfaces.hasMoreElements())
            {
                NetworkInterface ni = physical_network_interfaces.nextElement();
                List<InterfaceAddress> list_interface_address = ni.getInterfaceAddresses();
                for(InterfaceAddress ia : list_interface_address)
                {
                    InetAddress potential_address=ia.getAddress();
                    if(potential_address!=null && potential_address.getClass() == Inet4Address.class)
                    {
                        if(! potential_address.isLoopbackAddress() && ! potential_address.isLinkLocalAddress() )
                            r= (Inet4Address) potential_address;
                    }
                }
            }

        } catch (SocketException ex) {
            Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return r;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            
            byte[] password= "lala".getBytes(); 
            
            
            int common_port_slaves=ARDP_SLAVE_PORT;
            
            //ATTENTION : as specified in the function's name, this will only
            //work for IPv4 and only if there is one and only one IPv4 network
            //that we are currently connected to
            String my_addr=getMyIPv4AddressIfOnlyOneConnectedInterface().getHostAddress();// or my_addr="192.168.20.11";
            
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
