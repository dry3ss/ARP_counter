/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpd_test_master;

import arpdetox_lib.*;
import static arpdetox_lib.ARPDMessage.getAllRemainingBytesFromByteBuffer;
import java.net.InetSocketAddress;
import static arpdetox_lib.ARPDServer.ARDP_MASTER_PORT;
import static arpdetox_lib.ARPDServer.ARDP_SLAVE_PORT;
import java.nio.ByteBuffer;
/**
 *
 * @author will
 */
public class ARPD_test_master {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    try {
            
            byte[] password= "lala".getBytes();
            
            String localhost="192.168.1.94";            
            int port_master=ARDP_MASTER_PORT;         
            
            
            int common_port_slaves=ARDP_SLAVE_PORT;
            String addr_slave_1="192.168.1.100";
            String addr_slave_2="192.168.1.101";
            String addr_slave_3="192.168.1.102";
	    String addr_broadcast="192.168.1.255";
	    
                
            IPInfoContainers.SourceIPInfo s1_src_info= new IPInfoContainers.SourceIPInfo(localhost,port_master);
            
            
            ARPDMasterConsumerRunnable cr1= new ARPDMasterConsumerRunnable();
            
            
            //ATTENTION : pour tester jai été forcé de changer le constructeur de UDPServer
            //de manière à ne bind qu'avec l'adresse donnée par sX_src_info
            //à la palce de nimporte quelle adresse
            //Il faudra re CHANGER ça car dans le cas du MASTER les paquets viennent
            //de plusieurs sous réseaux différents donc avec des addresses différentes
            ARPDServer.ARPDServerMaster s1= new ARPDServer.ARPDServerMaster(s1_src_info,cr1);
            s1.setPasswd(password);
	    s1.start();
	    System.out.println("started");
	    Thread.sleep(1000);
	    
	    //##########################
	    /*
	    IPInfoContainers.DestIPInfo s2_dst_info = new IPInfoContainers.DestIPInfo(addr_broadcast,common_port_slaves);
	    ARPDSession s1_2= new ARPDSession(s1,s2_dst_info);
            s1.getSessionTable().addARPDSession(s1_2);
	    
	    System.out.println("servers started");
            
            ByteBuffer mess=ByteBuffer.allocate(50);
            mess.put("lala".getBytes());
            mess.flip();
            byte[] msg=getAllRemainingBytesFromByteBuffer(mess,false);
            s1.getSessionTable().sendSingleMessageToSessionAtIndex(s1_2.getSession_container_id(), msg);
	    
	    Thread.sleep(1000);
            
            s1.close();
	    
	    if(true)
		System.exit(0);
	    //############################""""
*/
	    System.out.println("sending");
            s1.sendStartARPD("192.168.1.100", (short)1000, false);
            
            Thread.sleep(10000);
            
	    System.out.println("closing");
            s1.close();
            
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
}
