/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import arpdetox_lib.IPInfoContainers.DestIPInfo;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author will
 */
public class ARPDSessionContainer {
    
    
    public static class ARPDSessionTableContainer
    {
        protected final ArrayList<ARPDSession> content= new ArrayList<ARPDSession>();
        protected final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        
        protected ReadWriteLock getLock()
        {
            return lock;
        }
        //WRITE LOCK
        public void addARPDSession(ARPDSession e)
        {
            lock.writeLock().lock();
            boolean added=false;
            try{
                added=content.add(e);            
            }finally
            {
                if(added)
                    e.setSession_container_id(content.size()-1);
                lock.writeLock().unlock();
            }
        }
        public void removeARPDSession(int index_of_session_to_remove)
        {
            ARPDSession removed=null;
            lock.writeLock().lock();
            try{
                removed=content.remove(index_of_session_to_remove);
            }finally
            {
                if(removed !=null)
                {//if something was removed we need to update the subsequent indexes !
                    removed.setSession_container_id(-1);
                    for(int i =index_of_session_to_remove; i<content.size();i++)
                    {
                        content.get(i).setSession_container_id(i);
                    }
                }
                lock.writeLock().unlock();
            }
        }
        
        //READ LOCK
        public void sendLoopMessageToSessionAtIndex(int index,byte[] msg)
        {
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                ARPDSession s=content.get(index);
                s.sendLoopMessage(msg);
            }finally
            {
                lock.readLock().unlock();
            }
        }
        
        public void stopLoopMessageOfSessionAtIndex(int index)
        {
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                ARPDSession s=content.get(index);
                s.stopLoopMessage();
            }finally
            {
                lock.readLock().unlock();
            }
        }
        
        public void sendSingleMessageToSessionAtIndex(int index,byte[] msg)
        {
           //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                ARPDSession s=content.get(index);
                s.sendSingleMessage(msg);
            }finally
            {
                lock.readLock().unlock();
            }
        }
       
        public void resetSessionsOverlappingWith(DestIPInfo dest)
        {
            if(dest==null)
                return;
            
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                for(ARPDSession s: content)
                {
                    //if we are overlapping
                    if(s.getDst_ip_info().hasIPInRange(dest.getAddress_dst()))
                        s.resetSession();
                }
            }finally
            {
                lock.readLock().unlock();
            }
        }
        public void resetAllSessions()
        {            
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                for(ARPDSession s: content)
                {
                    s.resetSession();
                }
            }finally
            {
                lock.readLock().unlock();
            }
        }
       
        public ARPDSession getSessionWith(DestIPInfo dest)
        {
            if(dest==null || dest.getAddress_dst()==null)
            {
                Logger.getLogger(ARPDetox_lib.class.getName()).log(Level.WARNING, "Could not getSessionWith null dest ");
                return null;
            }
            ARPDSession ret=null;
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                for(ARPDSession s: content)
                {
                    if(s.getDst_ip_info().equals(dest))
                    {
                        ret=s;
                        break;
                    }
                }
            }finally
            {
                lock.readLock().unlock();
            }
            return ret;
        }
        
        
        
        
        
        //carefull with this one ! TAKE The readlock at least before using ANY method!
        //and don't forget the TRY ... FINALLY
        protected ArrayList<ARPDSession> getContent()
        {
            return content;
        }
    }
    
    public static class ARPDSingleSessionContainer
    {
        protected ARPDSession content= null;
        protected final ReadWriteLock lock = new ReentrantReadWriteLock(true);
        
        protected ReadWriteLock getLock()
        {
            return lock;
        }
        //WRITE LOCK
        public void setARPDSession(ARPDSession e)
        {
            if(e==null)
                return;
            lock.writeLock().lock();
            try{
                content=e;           
            }finally
            {
                lock.writeLock().unlock();
            }
        }
        
        //READ LOCK
        public void sendLoopMessage(byte[] msg)
        {
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                content.sendLoopMessage(msg);
            }finally
            {
                lock.readLock().unlock();
            }
        }
        
        public void stopLoopMessage()
        {
            //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                content.stopLoopMessage();
            }finally
            {
                lock.readLock().unlock();
            }
        }
        
        public void sendSingleMessage(byte[] msg)
        {
           //read lock on the array containing all the sessions
            lock.readLock().lock();
            try{
                content.sendSingleMessage(msg);
            }finally
            {
                lock.readLock().unlock();
            }
        }
       
       
        //carefull with this one ! TAKE The readlock at least before using ANY method!
        //and don't forget the TRY ... FINALLY
        protected ARPDSession getContent()
        {
            return content;
        }
    }
}
