/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import static arpdetox_lib.ARPDetoxCountermeasure.executeCommand;
import quick_logger.LockedLogger;

/**
 *
 * @author will
 */
public class ARPDLoggers {
    
    public static final String loggers_dir="ARPD_logs";
    
    public static final LockedLogger message_logger;  
    
    public static final LockedLogger action_logger;
    
    static{
        
        String home_dir=System.getProperty("user.home");
        
        String actual_dir=home_dir+"/"+loggers_dir;
        //MESSAG log:
        message_logger = new LockedLogger(actual_dir,"Messages_log");
        
        //Action log : 
        action_logger = new LockedLogger(actual_dir,"Actions_log");
        
        
    }
    
    
    
    
}
