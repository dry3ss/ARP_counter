/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import static arpdetox_lib.ARPDMessage.getIpv4AddressFromString;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 */
public class IPInfoContainers {
    public static class SourceIPInfo
    {
        //string vars for debugging and printing
        protected final String str_ip_address_source;
        protected final String str_mac_src;
        
        //constant objects passed on to other parts:        
        protected final ARPDMessage.MACAddress mac_src;
        protected final Inet4Address ip_src;
        protected final NetworkInterface physical_interface_src;
        protected final int port_src;
        
        public SourceIPInfo(final String str_ip_address_source_,final int port_src_)
            throws InvalidParameterException
        {
            
            try {
                ip_src=ARPDMessage.getIpv4AddressFromString(str_ip_address_source_);
            } catch (UnknownHostException ex) {
                throw new InvalidParameterException("Could not create SourceIPInfo from this IP: "+str_ip_address_source_ );
            }
            try{
                 physical_interface_src=NetworkInterface.getByInetAddress(ip_src);
                 mac_src = new ARPDMessage.MACAddress(physical_interface_src.getHardwareAddress());
                 str_mac_src=mac_src.toString();
                 str_ip_address_source=ip_src.getHostAddress();
            }
            catch(SocketException | ArrayIndexOutOfBoundsException se)
            {
                throw new InvalidParameterException("Cannot create SourceIPInfo from ip_src: "+str_ip_address_source_+" because this address could not be found on any interface");
            }
            port_src=port_src_;
        }

        public String getStr_ip_address_source() {
            return str_ip_address_source;
        }

        public String getStr_mac_src() {
            return str_mac_src;
        }

        public ARPDMessage.MACAddress getMac_src() {
            return mac_src;
        }

        public Inet4Address getIp_src() {
            return ip_src;
        }

        public NetworkInterface getPhysical_interface_src() {
            return physical_interface_src;
        }

        public int getPort_src() {
            return port_src;
        }
        
    }
    
    public static class DestIPInfo
    {
        public static final Inet4Address BROADCAST_IP_ADDRESS=getBroadcastIpv4Address();
        protected static Inet4Address getBroadcastIpv4Address()
        {
            Inet4Address r= null;
            try{
                r= getIpv4AddressFromString("255.255.255.255");
            } catch (UnknownHostException | InvalidParameterException ex) {
                Logger.getLogger(ARPDSession.class.getName()).log(Level.SEVERE, null, ex);
            }
            return r;
        }
        
        public static DestIPInfo getBroadcastDestIPInfoForPort(int port)
        {
            //hopefully 255.255.255.255 shoudl do the trick and no need for our
            //network's actual broadcast address 
            return new DestIPInfo(BROADCAST_IP_ADDRESS,port);
        }
        
        //string vars for debugging and printing
        protected final String str_ip_address_dst;
        
        //constant objects passed on to other parts:
        protected final InetSocketAddress socket_dst; 
        protected final Inet4Address address_dst;
        protected final int port_dst;
        
        public DestIPInfo(final String str_ip_address_dst_,final int port_dst_)
            throws InvalidParameterException
        {
            try{
                address_dst=getIpv4AddressFromString(str_ip_address_dst_);
                socket_dst= new InetSocketAddress(address_dst,port_dst_);
                port_dst=port_dst_;
                str_ip_address_dst=str_ip_address_dst_;
           }
           catch( ArrayIndexOutOfBoundsException se)
           {
               throw new InvalidParameterException("Cannot create DestIPInfo from ip_src: "+str_ip_address_dst_+" because this address could not be found on any interface");
           } catch (UnknownHostException ex) {
                throw new InvalidParameterException("Cannot create DestIPInfo from ip_src: "+str_ip_address_dst_+" cause::UnknownHostException");
           }
        }
        public DestIPInfo(final Inet4Address ip_address_dst_,final int port_dst_)
            throws InvalidParameterException
        {
            try{
                address_dst=ip_address_dst_;
                socket_dst= new InetSocketAddress(address_dst,port_dst_);
                port_dst=port_dst_;
                str_ip_address_dst=ip_address_dst_.getHostAddress();
           }
           catch( ArrayIndexOutOfBoundsException se)
           {
               throw new InvalidParameterException("Cannot create DestIPInfo from ip_src: "+ip_address_dst_.getHostAddress()+" because this address could not be found on any interface");
           }
        }

        public String getStr_ip_address_dst() {
            return str_ip_address_dst;
        }

        public Inet4Address getAddress_dst() {
            return address_dst;
        }

        public InetSocketAddress getSocket_dst() {
            return socket_dst;
        }

        public int getPort_dst() {
            return port_dst;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.address_dst);
            hash = 97 * hash + this.port_dst;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DestIPInfo other = (DestIPInfo) obj;
            if (this.port_dst != other.port_dst) {
                return false;
            }
            if (!Objects.equals(this.address_dst, other.address_dst)) {
                return false;
            }
            return true;
        }
    
        public boolean hasIPInRange(Inet4Address other_addr)
        {
            //TODO add network specific address and not just 255.255.255.255
            //if other is the broadcast address 255.255.255.255
            if(other_addr.equals(BROADCAST_IP_ADDRESS))
                return true;
            return false;
        }
        
        public boolean isMultipleAddress()
        {
            if(this.getAddress_dst().equals(BROADCAST_IP_ADDRESS))
                return true;
            return false;
        }
    }
}
