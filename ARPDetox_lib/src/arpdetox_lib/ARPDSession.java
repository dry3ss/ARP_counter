/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import arpdetox_lib.ARPDMessage.MACAddress;
import arpdetox_lib.ARPDServer.ARPDServerMaster;
import static arpdetox_lib.ARPDServer.THRESHOLD_SEND_AGAIN;
import static arpdetox_lib.ARPDSession.ARPDSessionState.*;
import arpdetox_lib.ARPDSessionContainer.ARPDSessionTableContainer;
import arpdetox_lib.IPInfoContainers.*;
import arpdetox_lib.UDPServer.Message_InetSocketAddress_Pair;
import java.net.Inet4Address;
import java.security.InvalidParameterException;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author will
 * @param <T>
 */
public class ARPDSession<T extends UDPServer> {
    
    public static enum ARPDSessionState
    {
        START_ORDERED,START_ANSWERED,START_CONFIRMED,
        STOP_ORDERED,STOP_ANSWERED,STOP_CONFIRMED, NOSTATE;
        
        public ARPDSessionState getConfirmedState()
        {
            ARPDSessionState r =null;
            //If you're the master:
                //your only 2 states are ordered and confirmed
            //If you're the slave:
                //you're only two states are answered and confirmed
            switch(this)
            {
                case START_ORDERED:
                    r=START_CONFIRMED;
                    break;
                case START_CONFIRMED:
                    r=START_CONFIRMED;
                    break;
                case START_ANSWERED:
                    r=START_CONFIRMED;
                    break;
                case STOP_ORDERED:
                    r=STOP_CONFIRMED;
                    break;
                case STOP_ANSWERED:
                    r=STOP_CONFIRMED;
                    break;
                case STOP_CONFIRMED:
                    r=STOP_CONFIRMED;
                    break;
                default:
                    throw new IllegalArgumentException("ARPDSessionState was not among the possible states, was not supposed to happend");
            }
            return r;
        }
        
        public boolean isInUnconfirmedButValidState()
        {
            switch(this)
            {
                case START_ORDERED:
                    break;
                case START_ANSWERED:
                    break;
                case STOP_ORDERED:
                    break;
                case STOP_ANSWERED:
                    break;
                default:
                    return false;
            }
            return true;
        }
        
        public boolean isValidState()
        {
            return this != NOSTATE;
        }
        
        public boolean isStart()
        {
             switch(this)
            {
                case START_ORDERED:
                    break;
                case START_ANSWERED:
                    break;
                case START_CONFIRMED:
                    break;
                default:
                    return false;
            }
            return true;
        }
        public boolean isStop()
        {
             switch(this)
            {
                case STOP_ORDERED:
                    break;
                case STOP_ANSWERED:
                    break;
                case STOP_CONFIRMED:
                    break;
                default:
                    return false;
            }
            return true;
        }
        
        public static boolean areBothStartOrBothStop(ARPDSessionState a, ARPDSessionState b)
        {
            return( ( a.isStart() && b.isStart() ) || ( a.isStop() && b.isStop() ) );
        }
    };
    public static enum ARPDMasterSessionState
    {
        START_ORDER_SENT,START_CONFIRMATION_SENT,
        STOP_ORDER_SENT,STOP_CONFIRMATION_SENT;
        
        public ARPDMasterSessionState getConfirmedState()
        {
            if(this==START_ORDER_SENT || this ==START_CONFIRMATION_SENT)
                return START_CONFIRMATION_SENT;
            else
                return STOP_CONFIRMATION_SENT;
        }
    };
    public static enum ARPDSlaveSessionState
    {
        START_ANSWER_SENT,START_CONFIRMATION_RECEIVED,
        STOP_ANSWER_SENT,STOP_CONFIRMATION_RECEIVED;
        
        public ARPDSlaveSessionState getConfirmedState()
        {
            if(this==START_ANSWER_SENT || this ==START_CONFIRMATION_RECEIVED)
                return START_CONFIRMATION_RECEIVED;
            else
                return STOP_CONFIRMATION_RECEIVED;
        }
        
    };
       
    
    protected final SourceIPInfo src_ip_info;
    protected final DestIPInfo   dst_ip_info;    
    //objects resulting from the interpretation of the received messages:
    //ip dest as found in the content of the ARPD message, not from the socket
    protected Inet4Address ip_dst_from_content=null;  
    protected MACAddress mac_dst_from_content=null; //same concept
    
    public final ReentrantLock session_reentrant_lock= new ReentrantLock(true);//fair mode
    protected volatile boolean is_loop_send = false;
    protected final Message_InetSocketAddress_Pair msg_to_send;
    protected int counter_since_last_sent=0;
    
    protected int session_container_id=-1;
    protected T server;
    protected ARPDSessionState current_state=NOSTATE;
    protected int current_order_id=-1;
    
    public ARPDSession(T server_, DestIPInfo dst_ip_info_)
            throws InvalidParameterException
    {
        if(server_==null || dst_ip_info_ ==null)
            throw new InvalidParameterException("Cannot create session from null server_/DestIPInfo");
        server=server_;
        src_ip_info=server.getSrc_ip_info();
        dst_ip_info=dst_ip_info_;
        msg_to_send= new Message_InetSocketAddress_Pair(dst_ip_info_.getSocket_dst());
    }

    public Inet4Address getIp_dst_from_content() {
        return ip_dst_from_content;
    }

    public void setIp_dst_from_content(Inet4Address ip_dst_from_content_) {
        if(ip_dst_from_content_==null)
            return;
        session_reentrant_lock.lock();
        try{
            this.ip_dst_from_content = ip_dst_from_content_;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public MACAddress getMac_dst_from_content() {
        return mac_dst_from_content;
    }

    public void setMac_dst_from_content(MACAddress mac_dst_from_content_) {
        if(mac_dst_from_content_==null)
            return;
        session_reentrant_lock.lock();
        try{
            this.mac_dst_from_content = mac_dst_from_content_;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public SourceIPInfo getSrc_ip_info() {
        return src_ip_info;
    }

    public DestIPInfo getDst_ip_info() {
        return dst_ip_info;
    }

    
    public void sendLoopMessage(byte[] msg)
    {
        if(msg==null)
            return;
        session_reentrant_lock.lock();
        try {
            msg_to_send.setMess(msg);
            is_loop_send=true;
            counter_since_last_sent=0;
        } finally {
            session_reentrant_lock.unlock();
        }
    }
    
    public void stopLoopMessage()
    {
        session_reentrant_lock.lock();
        try {
            msg_to_send.resetMess();
            is_loop_send =false;
            counter_since_last_sent=0;
        } finally {
            session_reentrant_lock.unlock();
        }
    }
    
    public void sendSingleMessage(byte[] msg)
    {
        if(msg==null)
            return;
        session_reentrant_lock.lock();
        try {
            msg_to_send.setMess(msg); 
            is_loop_send=false;
            counter_since_last_sent=0;
        } finally {
            session_reentrant_lock.unlock();
        }      
    }
    
    public void resetSession()
    {
        session_reentrant_lock.lock();
        try {
            noLockResetSession();
        } finally {
            session_reentrant_lock.unlock();
        }
    }
    
    //meant to be used by the children only once they have acquired the lock
    //to avoid unnecessary copy-paste and potential errors
    protected void noLockResetSession()
    {
        //reset the msg related vars
        msg_to_send.resetMess();
        is_loop_send =false;
        counter_since_last_sent=0;
        //reset what we read inside their messages
        ip_dst_from_content=null;
        mac_dst_from_content=null;
    }
    
    //designed to be thread-safe !
    //sending directly instead of getting and sending somewhere else insures us against 
    //potentially modifying the content should an error occur between getting and sending
    public void SendMessageIfDirectlyPossible(UDPServer server)
    {
        if(server==null)
            return;
        boolean got_it=session_reentrant_lock.tryLock();
        boolean sent=false;
        try 
        {
            if(got_it)
            {
                //if there's no message check that is_loop_send is at it's default false value
                if(msg_to_send.getMess()==null || ! msg_to_send.hasMsg())
                {
                    is_loop_send=false;
                }
                //we don't want to send it if it's too early EG if (counter_since_last_sent < THRESHOLD_SEND_AGAIN)
                else if(counter_since_last_sent>THRESHOLD_SEND_AGAIN)
                {//we copy it and try to send it
                    Message_InetSocketAddress_Pair copy=new Message_InetSocketAddress_Pair(msg_to_send);
                    sent=server.sendIfPossible(copy);
                }
            }
        }finally 
        {
            
            if(sent)
            {
                if(!is_loop_send)//if this is not a loop message and it has been sent, we can remove it
                    msg_to_send.resetMess();
                counter_since_last_sent=0;//and put the counter back to 0
            }
            else
                counter_since_last_sent++;
            
            if(got_it)//always unlock, hence the finally !
                session_reentrant_lock.unlock();
        }    
    }

    public int getSession_container_id() {
        return session_container_id;
    }

    public void setSession_container_id(int session_container_id) {
        session_reentrant_lock.lock();
        try{
            this.session_container_id = session_container_id;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public ARPDSessionState getCurrentState() {
        return current_state;
    }

    public void setCurrentState(ARPDSessionState state_) {
        if(state_==null)
            return;
        else if(state_ == NOSTATE)
            return;
        session_reentrant_lock.lock();
        try{
            this.current_state = state_;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    
    protected int getCurrentOrderId()
    {
        return current_order_id;
    }
    
    protected void setCurrentOrderId(int new_nb)
    {
        session_reentrant_lock.lock();
        try{
            this.current_order_id=new_nb;
        }finally
        {
            session_reentrant_lock.unlock();
        }        
    }

    public boolean isValid()
    {
        return ! (current_order_id<0 || ! current_state.isValidState());
    }
    
}
