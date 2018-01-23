/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import arpdetox_lib.ARPDMessage.*;
import static arpdetox_lib.ARPDMessage.ARPD_MESSAGE_TYPE.*;
import arpdetox_lib.ARPDSession.*;
import static arpdetox_lib.ARPDSession.ARPDSessionState.*;
import arpdetox_lib.ARPDSessionContainer.*;
import static arpdetox_lib.ConsumerRunnable.TIMEOUT_CONSUMER_THREAD;
import arpdetox_lib.IPInfoContainers.DestIPInfo;
import static arpdetox_lib.IPInfoContainers.DestIPInfo.BROADCAST_IP_ADDRESS;
import arpdetox_lib.IPInfoContainers.SourceIPInfo;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 */
public class ARPDServer
{
    public static final int LOOP_MESSAGE_SLEEP_MS = 500;
    public static final int THRESHOLD_SEND_AGAIN=LOOP_MESSAGE_SLEEP_MS / TIMEOUT_CONSUMER_THREAD;
    
    public static final int ARDP_MASTER_PORT=2600;
    public static final int ARDP_SLAVE_PORT=2605;
    
    public static class ARPDServerMaster extends UDPServer<ARPDServerMaster>
    {
        private final ARPDSessionTableContainer session_table= new ARPDSessionTableContainer();
        private final ARPDBroadcastSession broadcast_session;
        protected byte[] passwd=null;

        public ARPDServerMaster(final SourceIPInfo src_ip_info_, final RunnableUDPServerInterface<ARPDServerMaster> consumer_thread_runnable)
            throws ClosedChannelException, SocketException, IOException 
        {
            super(src_ip_info_,consumer_thread_runnable);
            broadcast_session= new ARPDBroadcastSession(this,ARDP_SLAVE_PORT);
        }

        
        public ARPDSessionTableContainer getSessionTable()
        {
            return session_table;
        }
        public ARPDBroadcastSession getBroadcastSession()
        {
            return broadcast_session;
        }
        
        
        public void sendStartARPD(final String ip_dst_,final  short action_delay,final boolean everyone_acts_or_only_dst_)
        {
            sendARPDOrder(true,ip_dst_,action_delay,everyone_acts_or_only_dst_);
        }
        public void sendStopARPD(final String ip_dst_,final  short action_delay,final boolean everyone_acts_or_only_dst_)
        {
            sendARPDOrder(false,ip_dst_,action_delay,everyone_acts_or_only_dst_);
        }
        protected void sendARPDOrder(final boolean start_is_1_stop_is_0,final String ip_dst_,final  short action_delay,final boolean everyone_acts_or_only_dst_)
        {
            
            try {
                Inet4Address addr_primary_slave_dst =ARPDMessage.getIpv4AddressFromString(ip_dst_);
                ARPDMessage.ARPD_MESSAGE_TYPE msg_type=null;
                ARPDSessionState state_after_sent=null;
                if(start_is_1_stop_is_0)
                {
                    msg_type=ORDER_START_ARPD;
                    state_after_sent=START_ORDERED;
                }
                else
                {
                    msg_type=ORDER_STOP_ARPD;
                    state_after_sent=STOP_ORDERED;
                }
                int order_nb=getBroadcastSession().getNextOrderId();
                ARPDOrder order=new ARPDOrder(msg_type,true,action_delay,getSrc_ip_info().getIp_src(),getSrc_ip_info().getMac_src(),addr_primary_slave_dst,order_nb,passwd);
                setupSessions(order_nb,everyone_acts_or_only_dst_,addr_primary_slave_dst,state_after_sent);
		logger.log(Level.INFO,order.toString(0, passwd));
                getBroadcastSession().sendLoopMessage(order.toBytes());
            } catch (UnknownHostException | InvalidParameterException ex) {
                Logger.getLogger(ARPDServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        protected void setupSessions(final int new_order_nb,final boolean everyone_acts_or_only_dst_,Inet4Address primary_dst,ARPDSessionState state_after_sent)
        {
            if(primary_dst==null || state_after_sent==null)
                return;
            //setup the broadcast session
            getBroadcastSession().setEveryone_acts_or_only_dst_(everyone_acts_or_only_dst_);
            getBroadcastSession().setPrimary_ip_dst(primary_dst);
            getBroadcastSession().setCurrentState(state_after_sent);
            //setup all the others
            boolean found_primary_dst=false;
            getSessionTable().getLock().readLock().lock();
            try
            {
                boolean affect_everyone=everyone_acts_or_only_dst_ || primary_dst.equals(BROADCAST_IP_ADDRESS) ;
                for( ARPDSession s :getSessionTable().getContent() )
                {
                    if(affect_everyone || s.getDst_ip_info().hasIPInRange(primary_dst))
                    {
                        s.resetSession();
                        s.setCurrentOrderId(new_order_nb);
                        s.setCurrentState(state_after_sent); 
                        if(s.getDst_ip_info().getAddress_dst().equals(primary_dst))
                            found_primary_dst=true;
                    }
                }
            }finally
            {
                getSessionTable().getLock().readLock().unlock();
            }
            if(!found_primary_dst) //add a single_session_container !
            {
                //if you can broadcast to it it means the broadcast is on the right port
                //and in the end, all the slaves are supposed to be on the same port
                int port_dst=broadcast_session.getDst_ip_info().getPort_dst();
                ARPDSession to_add = new ARPDSession(this, new DestIPInfo(primary_dst,port_dst));
                to_add.setCurrentOrderId(new_order_nb);
                to_add.setCurrentState(state_after_sent);
                //this will request the write lock
                getSessionTable().addARPDSession(to_add);
            }
        }

        public void setPasswd(byte[] passwd) {
            this.passwd = passwd;
        }
        
        
    }
        
    
    public static class ARPDServerSlave extends UDPServer<ARPDServerSlave>
    {
        private final ARPDSingleSessionContainer single_session_container= new ARPDSingleSessionContainer();
        protected byte[] passwd=null;


        public ARPDServerSlave(final SourceIPInfo src_ip_info_, final RunnableUDPServerInterface<ARPDServerSlave> consumer_thread_runnable)
            throws ClosedChannelException, SocketException, IOException 
        {
            super(src_ip_info_,consumer_thread_runnable);
        }


        public ARPDSingleSessionContainer getSingleSessionContainer()
        {
            return single_session_container;
        }

        
        public void setPasswd(byte[] passwd) {
            this.passwd = passwd;
        }
        
    }
    
   
    
}
    
