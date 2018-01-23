/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpd_test_slave;

import arpdetox_lib.*;
import static arpdetox_lib.ARPDMessage.getAllRemainingBytesFromByteBuffer;
import static arpdetox_lib.ARPDServer.ARDP_SLAVE_PORT;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
            
            String addr_slave_1="192.168.1.94";
            String addr_slave_2="192.168.1.101";
            String addr_slave_3="192.168.1.102";
                
            IPInfoContainers.SourceIPInfo s1_src_info= new IPInfoContainers.SourceIPInfo(addr_slave_3,common_port_slaves);
            IPInfoContainers.SourceIPInfo s2_src_info= new IPInfoContainers.SourceIPInfo(addr_slave_1,common_port_slaves);
            IPInfoContainers.SourceIPInfo s3_src_info= new IPInfoContainers.SourceIPInfo(addr_slave_2,common_port_slaves);
            
            IPInfoContainers.DestIPInfo s1_dst_info = new IPInfoContainers.DestIPInfo(addr_slave_3,common_port_slaves);
            IPInfoContainers.DestIPInfo s2_dst_info = new IPInfoContainers.DestIPInfo("255.255.255.255",common_port_slaves);
            IPInfoContainers.DestIPInfo s3_dst_info = new IPInfoContainers.DestIPInfo("255.255.255.255",common_port_slaves);
            
            
            ARPDSlaveConsumerRunnable cr1= new ARPDSlaveConsumerRunnable();
            ARPDSlaveConsumerRunnable cr2= new ARPDSlaveConsumerRunnable();
            ARPDSlaveConsumerRunnable cr3= new ARPDSlaveConsumerRunnable();
            
            
            //ATTENTION : pour tester jai été forcé de changer le constructeur de UDPServer
            //de manière à ne bind qu'avec l'adresse donnée par sX_src_info
            //à la palce de nimporte quelle adresse
            //Il faudra re CHANGER ça car dans le cas du MASTER les paquets viennent
            //de plusieurs sous réseaux différents donc avec des addresses différentes
            ARPDServer.ARPDServerSlave s1= new ARPDServer.ARPDServerSlave(s1_src_info,cr1);
            //ARPDServer.ARPDServerSlave s2= new ARPDServer.ARPDServerSlave(s2_src_info,cr2);
            //ARPDServer.ARPDServerSlave s3= new ARPDServer.ARPDServerSlave(s3_src_info,cr3);
            
            s1.setPasswd(password);
            //s2.setPasswd(password);
            //s3.setPasswd(password);
            
            s1.start();
            //s2.start();
            //s3.start();
            
            System.out.println("servers started");
            boolean a=true;
            while(a)
            {
                Thread.sleep(30);
            }
            
            //s3.send(to_send3);
            Thread.sleep(1000);
            
            s1.close();
            //s2.close();
            //s3.close();
            
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
}
