/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import arpdetox_lib.ARPDMessage.MACAddress;
import static arpdetox_lib.ARPDMessage.getIpv4AddressFromString;
import arpdetox_lib.IPInfoContainers.DestIPInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 */
public class ARPDetoxCountermeasure {
    
    public static class IP_MAC_Pair
    {
        protected final InetAddress ip;
        protected final MACAddress mac;

        IP_MAC_Pair(InetAddress ip_,MACAddress mac_)
        {
            ip=ip_;
            mac=mac_;
        }

        public InetAddress getIp() {
            return ip;
        }

        public MACAddress getMac() {
            return mac;
        }

        @Override
        public String toString() {
            return "IP_MAC_Pair{" + "ip=" + ip.getHostAddress() + ", mac=" + mac + '}';
        }


    }
    
    public static class ARPTable
    {
        protected final ArrayList<IP_MAC_Pair> ARP_table= new ArrayList<IP_MAC_Pair>();
        protected final ReentrantLock lock= new ReentrantLock(true);//fair mode
    }
    
    
    protected final static Logger logger=Logger.getLogger(ARPDetox_lib.class.getName());
    protected final static ARPTable arp_table_container=new ARPTable();
    
    public static void addStaticEntry(InetAddress ip,MACAddress mac)
    {
        if(ip==null || mac==null)
            throw new IllegalArgumentException("Cannot add a static ARP entry from a null ip or mac");
        
        boolean found_ip=false;
        //update the table
        updateARPTable();
        //get it & check whether it already has this IP and MAC inside
        arp_table_container.lock.lock();
        try{
        int i=0;
        for(IP_MAC_Pair pair:arp_table_container.ARP_table )
        {
            if(pair.ip.equals(ip) && pair.mac.equals(mac))
            {
                return;//a line with the same IP/MAC is already present, no need to add it
            }
            if(pair.ip.equals(ip))
            {
                found_ip=true;
                break;
            }       
            i++;
        }
        //
        }finally
        {
            arp_table_container.lock.unlock();
        }
        if(found_ip)
        {
            //the same ip but with a different mac has been found, will delete the
            //old entry before adding this one
            logger.log(Level.WARNING,"The same ip but with a different mac has been found in the ARPTable, will delete the old entry before adding this one");
            removeStaticEntry(ip);
        }
        String command="sudo arp -s "+ip.getHostAddress()+" "+mac.toString();
        String a=executeCommand(command);
        //a should be empty string if there was no pb when adding the new entry
        if(! a.equals(""))
            throw new IllegalArgumentException("There was a problem add the entry: "+ip.getHostAddress()+" "+mac.toString()+" to the ARP Table");
    }
    public static void removeStaticEntry(InetAddress ip)
    {
        if(ip==null)
            throw new IllegalArgumentException("Cannot add a static ARP entry from a null ip");
        boolean found_ip=false;
        //update the table
        updateARPTable();
        //get it & check whether it already has this IP inside
        arp_table_container.lock.lock();
        try{
        int i=0;
        for(IP_MAC_Pair pair:arp_table_container.ARP_table )
        {
            if(pair.ip.equals(ip))
            {
                found_ip=true;
                break;
            }       
            i++;
        }
        //
        }finally
        {
            arp_table_container.lock.unlock();
        }
        if(found_ip)
        {
            String command="sudo arp -d "+ip.getHostAddress();
            String a=executeCommand(command);
            //a should be empty string if there was no pb when adding the new entry
            if(! a.equals(""))
                throw new IllegalArgumentException("There was a problem removing the entry: "+ip.getHostAddress()+" from the ARP Table");
        }
    }
    protected static void updateARPTable() throws IllegalArgumentException
    {
        arp_table_container.lock.lock();
        try{
            String result_text=executeCommand("sudo arp -n");
            
            BufferedReader bufReader = new BufferedReader(new StringReader(result_text));
            String line=null;
            line=bufReader.readLine();
            if(line==null )//no line to be read
                throw new IllegalArgumentException("the updateARPTable command "
                + "returned a null String, probably need to use visudo to "
                + "change the /etc/sudoers file and add a line similar to the following:\n"
                + "username ALL = (root:root) NOPASSWD: /usr/sbin/arp");
            String[] parts = line.split("\\s+",2);//only split once
            if( ! parts[0].equals("Address"))
                throw new IllegalArgumentException("the updateARPTable command "
                + "didn't return the a String starting with \"Address\", probably need to use visudo to "
                + "change the /etc/sudoers file and add a line similar to the following:\n"
                + "username ALL = (root:root) NOPASSWD: /usr/sbin/arp");
            //the first line is thrown away after we verified that it started with Address
            
            //Not effective in the slittest but we will just clear() the current table 
            //then rebuild it from scratch from the output
            arp_table_container.ARP_table.clear();
            while( (line=bufReader.readLine()) != null )
            {
                //example line: 
                // 192.168.1.1 ether 24:95:04:b1:2d:40 C wlp3s0
                parts = line.split("\\s+");
                if(parts.length != 5)
                    continue;
                InetAddress ip=InetAddress.getByName(parts[0]);
                MACAddress mac=new MACAddress(parts[2]);
                arp_table_container.ARP_table.add(new IP_MAC_Pair(ip,mac));
            }
            //logger.log(Level.INFO,"updateARPTable\n"+result_text);
        } catch (IOException ex) {
            Logger.getLogger(ARPDetoxCountermeasure.class.getName()).log(Level.SEVERE, null, ex);
        }finally
        {
            arp_table_container.lock.unlock();
        }
    }
    
    private static String executeCommand(String command) {

		StringBuilder output = new StringBuilder();

		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader =
                            new BufferedReader(new InputStreamReader(p.getInputStream()));

                        String line = "";
			while ((line = reader.readLine())!= null) {
				output.append(line).append("\n");
			}

		} catch (IOException | InterruptedException e) {
			logger.log(Level.FINER, null, e);
		}
		return output.toString();
	}
    
}
