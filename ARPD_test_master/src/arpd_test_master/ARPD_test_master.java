/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpd_test_master;

import arpdetox_lib.*;
import static arpdetox_lib.ARPDServer.ARDP_MASTER_PORT;
import static arpdetox_lib.ARPDServer.ARDP_SLAVE_PORT;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
            //Graphic Interface
            JFrame frame = new JFrame("Dashboard");
            frame.setSize(500, 500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(6, 1));
            frame.setVisible(true);
            
            
            
            
            byte[] password= "lala".getBytes();
            
            String localhost="192.168.20.10";            
            int port_master=ARDP_MASTER_PORT;         
            
            
            int common_port_slaves=ARDP_SLAVE_PORT;
            String addr_slave_1="192.168.20.11";
	    
                
            IPInfoContainers.SourceIPInfo s1_src_info= new IPInfoContainers.SourceIPInfo(localhost,port_master);
            
            
            ARPDMasterConsumerRunnable cr1= new ARPDMasterConsumerRunnable();
            
            ARPDServer.ARPDServerMaster s1= new ARPDServer.ARPDServerMaster(s1_src_info,cr1);
            s1.setPasswd(password);
            
            
            //"ON" Button
            JButton b_on = new JButton("Start ARPD");
            b_on.addActionListener(new ActionListener(){
                public void actionPerformed(java.awt.event.ActionEvent e){
                    s1.sendStartARPD(addr_slave_1, (short)1000, false);
                }	
            });

            //"OFF" Button
            JButton b_off = new JButton("Stop ARPD");
            b_off.addActionListener(new ActionListener(){
                public void actionPerformed(java.awt.event.ActionEvent e){
                    s1.sendStopARPD(addr_slave_1, (short)1000, false);
                }	
            });
	
    	
            //Add buttons on the dashboard
	
            panel.add(b_on);
            panel.add(b_off);
            frame.add(panel, BorderLayout.CENTER); 
            //refresh interface
            frame.pack();
            frame.setVisible(true);
            
            
	    s1.start();
	    System.out.println("started");
	    Thread.sleep(1000);
            
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() 
                {

                    System.out.println("closing");
                    try {		
                        s1.close();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ARPD_test_master.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ARPD_test_master.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public static void maina(String[] args) {
    try {
            byte[] password= "lala".getBytes();
            
            String localhost="192.168.20.10";            
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
	    System.out.println("sending start");
            s1.sendStartARPD("192.168.20.11", (short)1000, false);
            
            Thread.sleep(5000);
	    System.out.println("sending stop");
            s1.sendStopARPD("192.168.20.11", (short)1000, false);
            Thread.sleep(5000);
            
            
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() 
                {

                    System.out.println("closing");
                    try {		
                        s1.close();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ARPD_test_master.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(ARPD_test_master.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
}
