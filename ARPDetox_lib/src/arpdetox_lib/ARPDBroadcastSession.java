/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import java.net.Inet4Address;

/**
 *
 * @author will
 */
public class ARPDBroadcastSession extends ARPDSession<ARPDServer.ARPDServerMaster>
{
    protected int number_slaves=1;
        
    protected Inet4Address current_primary_ip_dst=null;
    protected boolean currently_everyone_acts_or_only_dst_=false;
    protected int current_different_answers_received=0;

    public ARPDBroadcastSession(ARPDServer.ARPDServerMaster server_, final int port_dest_)
    {
        //TODO TEST it !
        super(server_,IPInfoContainers.DestIPInfo.getBroadcastDestIPInfoForPort(port_dest_));
        msg_to_send.setFullBroadcast();//the message will be in broadcast mode
    }
    protected int getNextOrderId()
    {
        session_reentrant_lock.lock();
        try{
            current_order_id++;
        }finally
        {
            session_reentrant_lock.unlock();
        }
        return current_order_id;
    }


    public Inet4Address getPrimary_ip_dst() {
        return current_primary_ip_dst;
    }

    protected void setPrimary_ip_dst(Inet4Address primary_ip_dst_) 
    {
        if(primary_ip_dst_==null)
            return;
        session_reentrant_lock.lock();
        try{
            this.current_primary_ip_dst = primary_ip_dst_;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public boolean isEveryone_acts_or_only_dst_() {
        return currently_everyone_acts_or_only_dst_;
    }

    protected void setEveryone_acts_or_only_dst_(boolean everyone_acts_or_only_dst_) 
    {

        session_reentrant_lock.lock();
        try{
            this.currently_everyone_acts_or_only_dst_ = everyone_acts_or_only_dst_;
        }finally
        {
            session_reentrant_lock.unlock();
        }

    }

    public int getNumber_different_answers_expected() {
        //if everyone acts then we expect an answer from eveyone, else only expect the primary_dst
        int nb= currently_everyone_acts_or_only_dst_ ? number_slaves : 1;
        return nb;
    }

    protected void setNumberSlaves(int number_different_answers_expected) {
        if(number_different_answers_expected <1)
            return;
        session_reentrant_lock.lock();
        try{
            this.number_slaves = number_different_answers_expected;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public int getNumberSlaves()
    {
        return number_slaves;
    }

    public void incrementAnswersReceived()
    {
        session_reentrant_lock.lock();
        try{
            this.current_different_answers_received++;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }
    public void resetAnswersReceived()
    {
        session_reentrant_lock.lock();
        try{
            this.current_different_answers_received=0;
        }finally
        {
            session_reentrant_lock.unlock();
        }
    }

    public int getAnswersReceived()
    {
        return current_different_answers_received;
    }

    public boolean getCanStopBroadcastBasedOnAnswersSoFar()
    {
        //TODO call the broadcast_session's method checking whether we should stop sending the order message
        //based on the number of answers and such ...
        ARPDSessionContainer.ARPDSessionTableContainer session_table= this.server.getSessionTable();
        boolean r=false;
        session_table.getLock().readLock().lock();
        try{
            int nb_anwers=0;
            //if multiple address than not one that corresponds therefore 
            //true if eveyone of this multiple network has answered 
            boolean primary_dst_has_answered=(!this.dst_ip_info.isMultipleAddress());
            for (ARPDSession s : session_table.getContent())
            {
                if(s.getCurrentOrderId()==this.current_order_id)
                {
                    nb_anwers++;
                    if(s.getDst_ip_info().getAddress_dst().equals(this.current_primary_ip_dst))
                        primary_dst_has_answered=true;
                }
            }
            //if we have the right nb of answer and the main dst has answered than we're good to stop!
            if(nb_anwers==this.getNumber_different_answers_expected() && primary_dst_has_answered)
                r=true;
        }finally
        {
            session_table.getLock().readLock().unlock();
        }
        return r;
    }


    @Override
    public void resetSession()
    {
        session_reentrant_lock.lock();
        try {
            this.noLockResetSession();                
        } finally {
            session_reentrant_lock.unlock();
        }
    }

    @Override
    protected void noLockResetSession()
    {
        super.noLockResetSession();
        //reset the "current" variables
        current_primary_ip_dst=null;
        currently_everyone_acts_or_only_dst_=false;
        current_different_answers_received=0;
    }
        
}
