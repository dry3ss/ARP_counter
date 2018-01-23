/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import arpdetox_lib.ARPDMessage.ARPDAnswer;
import arpdetox_lib.ARPDMessage.ARPDOrder;
import arpdetox_lib.ARPDMessage.MACAddress;
import static arpdetox_lib.ARPDMessage.getAllRemainingBytesFromByteBuffer;
import static arpdetox_lib.ARPDMessage.getIpv4AddressFromString;
import static arpdetox_lib.ARPDMessage.getMsgTypeFromBytes;
import arpdetox_lib.ARPDServer.*;
import arpdetox_lib.IPInfoContainers.DestIPInfo;
import arpdetox_lib.IPInfoContainers.SourceIPInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 */
public class ARPDetox_lib {

    
    protected static void testMsgInterpret()
    {
         //SEND
        byte[] bytes_sent=null;        
        Inet4Address addr_slave =null;
        byte[] password= "lala".getBytes();
        
        try {
            byte[]payload=new byte[5];
            for(int i=0;i<payload.length;i++)
                payload[i]=(byte)i;
            int order_nb=51;
            Inet4Address addr_master =ARPDMessage.getIpv4AddressFromString("192.168.1.2");
            addr_slave =ARPDMessage.getIpv4AddressFromString("192.168.1.10");
            MACAddress mac_master=new MACAddress("aa:bb:cc:dd:ee:ff");
            
            ARPDOrder order=new ARPDOrder(ARPDMessage.ARPD_MESSAGE_TYPE.ORDER_START_ARPD,true,(short)3000,addr_master,mac_master,addr_slave,order_nb,password);
            System.out.println(order.toString(0,password,System.currentTimeMillis()));
            bytes_sent=order.toBytes();
            
        } catch (UnknownHostException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        }  catch (InvalidParameterException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //RECEIVE
        try
        {
            addr_slave =ARPDMessage.getIpv4AddressFromString("192.168.1.10");
            MACAddress mac_slave=new MACAddress("aa:bb:cc:dd:ee:00");   
            ARPDMessage.ARPD_MESSAGE_TYPE type_sent=getMsgTypeFromBytes(bytes_sent);
            if(type_sent.getAssociatedClass() == ARPDOrder.class)
            {
                ARPDOrder received=ARPDMessage.fromBytes(type_sent,bytes_sent);
                System.out.println(received.toString(0,password,System.currentTimeMillis()));
                
                Inet4Address addr_dst_received=received.getIP_dst();
                if(addr_dst_received.equals(addr_slave)|| received.isEveryone_acts_or_only_dst())
                {
                    Inet4Address addr_master_received=received.getIP_src();  
                    System.out.println("answer to "+ addr_master_received);
                    int rcv_order_nb=received.getSuffix().getNoonce();
                    ARPDAnswer answer=null; 
                    switch (type_sent)
                    {
                        case ORDER_START_ARPD:
                            answer= new ARPDAnswer(ARPDMessage.ARPD_MESSAGE_TYPE.ANSWER_ACK_START,true,addr_slave,mac_slave,rcv_order_nb,password);       
                            break;
                        case ORDER_STOP_ARPD:
                            answer= new ARPDAnswer(ARPDMessage.ARPD_MESSAGE_TYPE.ANSWER_ACK_STOP,true,addr_slave,mac_slave,rcv_order_nb,password);       
                            break;
                        default:
                            return;
                    }
                    String a=answer.toString(0,password,System.currentTimeMillis());
                    System.out.println(a);
                    
                }
                
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        }  catch (InvalidParameterException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       testMsgInterpret();
       Inet4Address ip;
        try {
            ip = getIpv4AddressFromString("192.168.1.151");
            MACAddress mac=new MACAddress("aa:bb:cc:dd:ee:ff");
            MACAddress mac2=new MACAddress("aa:bb:cc:dd:ee:fe");
            ARPDetoxCountermeasure.addStaticEntry(ip,mac);
            Thread.sleep(3000);
            ARPDetoxCountermeasure.addStaticEntry(ip,mac);
            Thread.sleep(3000);
            ARPDetoxCountermeasure.addStaticEntry(ip,mac2);
            Thread.sleep(3000);
            ARPDetoxCountermeasure.removeStaticEntry(ip);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidParameterException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.SEVERE, null, ex);
        }
       
       if(true)
           System.exit(0);
        
        try {
            
            byte[] password= "lala".getBytes();
            
            int p1=2600;
            int p2=2601;
            int p3=2602;
            
            int common_port_slaves=2605;
            int[] array_actual_ports_slaves= {p2,p3 } ;
            int port_master=p1;
            
            //UDPPacketDuplicator packet_duplicator= new UDPPacketDuplicator(common_port_slaves,array_actual_ports_slaves);
            //packet_duplicator.start();
            
            
            String localhost="192.168.1.94";
            String addr_slave_1="192.168.1.100";
            String addr_slave_2="192.168.1.101";
            String addr_slave_3="192.168.1.102";
                
            SourceIPInfo s1_src_info= new SourceIPInfo(localhost,port_master);
            SourceIPInfo s2_src_info= new SourceIPInfo(addr_slave_1,common_port_slaves);
            SourceIPInfo s3_src_info= new SourceIPInfo(addr_slave_2,common_port_slaves);
            
            DestIPInfo s1_dst_info = new DestIPInfo(localhost,port_master);
            DestIPInfo s2_dst_info = new DestIPInfo("255.255.255.255",common_port_slaves);
            //DestIPInfo s2_dst_info = new DestIPInfo(addr_slave_1,common_port_slaves);
            DestIPInfo s3_dst_info = new DestIPInfo("255.255.255.255",common_port_slaves);
            //DestIPInfo s3_dst_info = new DestIPInfo(addr_slave_2,common_port_slaves);
            
            
            ARPDMasterConsumerRunnable cr1= new ARPDMasterConsumerRunnable();
            ARPDMasterConsumerRunnable cr2= new ARPDMasterConsumerRunnable();
            ARPDMasterConsumerRunnable cr3= new ARPDMasterConsumerRunnable();
            
            
            //ATTENTION : pour tester jai été forcé de changer le constructeur de UDPServer
            //de manière à ne bind qu'avec l'adresse donnée par sX_src_info
            //à la palce de nimporte quelle adresse
            //Il faudra re CHANGER ça car dans le cas du MASTER les paquets viennent
            //de plusieurs sous réseaux différents donc avec des addresses différentes
            ARPDServerMaster s1= new ARPDServerMaster(s1_src_info,cr1);
            ARPDServerMaster s2= new ARPDServerMaster(s2_src_info,cr2);
            ARPDServerMaster s3= new ARPDServerMaster(s3_src_info,cr3);
            
            s1.setPasswd(password);
            s2.setPasswd(password);
            s3.setPasswd(password);
            
            ARPDSession s1_2= new ARPDSession(s1,s2_dst_info);
            ARPDSession s1_3= new ARPDSession(s1,s3_dst_info);
            
            
            s1.getSessionTable().addARPDSession(s1_2);
            s1.getSessionTable().addARPDSession(s1_3);
            
            s1.start();
            s2.start();
            s3.start();
            
            System.out.println("servers started");
            
            ByteBuffer mess=ByteBuffer.allocate(50);
            mess.put("lala".getBytes());
            mess.flip();
            byte[] msg=getAllRemainingBytesFromByteBuffer(mess,false);
            s1.getSessionTable().sendSingleMessageToSessionAtIndex(s1_2.getSession_container_id(), msg);
            
            
            
            ByteBuffer mess2=ByteBuffer.allocate(50);
            mess2.put(new String("lola").getBytes());
            mess2.flip();
            byte[] msg2=getAllRemainingBytesFromByteBuffer(mess2,false);
            s1.getSessionTable().sendLoopMessageToSessionAtIndex(s1_3.getSession_container_id(), msg2);
            Thread.sleep(1000);
            s1.getSessionTable().stopLoopMessageOfSessionAtIndex(s1_3.getSession_container_id());
            Thread.sleep(1000);
            s1.getSessionTable().removeARPDSession(s1_2.getSession_container_id());
            s1.getSessionTable().sendLoopMessageToSessionAtIndex(s1_3.getSession_container_id(), msg);
            Thread.sleep(1000);
            s1.getSessionTable().stopLoopMessageOfSessionAtIndex(s1_3.getSession_container_id());
            
            
            UDPServer.Message_InetSocketAddress_Pair to_send3= new UDPServer.Message_InetSocketAddress_Pair( new InetSocketAddress("127.0.0.1",p2));
            ByteBuffer mess3=ByteBuffer.allocate(50);
            mess3.put(new String("lalo").getBytes());
            mess3.flip();
            to_send3.setMess(getAllRemainingBytesFromByteBuffer(mess3,false));
            
            //s3.send(to_send3);
            Thread.sleep(1000);
            
            s1.close();
            s2.close();
            s3.close();
            
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
}
