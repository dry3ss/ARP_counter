/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arpdetox_lib;

import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author will
 */
public abstract class ARPDMessage {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(ByteBuffer bytes) {
        return bytesToHex(bytes.array());//usually bad, here ok
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        int v = b & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }
    protected final static Logger logger=Logger.getLogger(ARPDetox_lib.class.getName());
    
    public static String nTabs(int n)
    {
        if(n<=0)
            return "\n";
        String a=new String("\t");
        for(int i=1;i<n;i++)
            a+=a;
        return "\n"+a;
    }
    
    
    public static byte[] getAllRemainingBytesFromByteBuffer(final ByteBuffer in, final boolean byte_buffer_to_be_reused)
    {
        boolean  byte_buffer_completely_filled= ( in.position()==0 &&  in.limit()==in.capacity() );
        
        if(!byte_buffer_to_be_reused && byte_buffer_completely_filled && in.hasArray())
            return in.array();
        else
        {
            byte[] out= new byte[in.remaining()];
            in.get(out);
            return out;
        }        
    }
    
    
    
    public enum ARPD_MESSAGE_TYPE
    {
        ORDER_START_ARPD(0,ARPDOrder.class),ANSWER_ACK_START(1,ARPDAnswer.class),
        ORDER_STOP_ARPD(2,ARPDOrder.class), ANSWER_ACK_STOP(3,ARPDAnswer.class);
        
        private short numval;
        private Class associated_class;
        
        public static ARPD_MESSAGE_TYPE getFromValue(int a) throws InvalidParameterException
        {
            switch(a)
            {
                case 0:
                    return ORDER_START_ARPD;
                case 1:
                    return ANSWER_ACK_START;
                case 2:
                    return ORDER_STOP_ARPD;
                case 3:
                    return ANSWER_ACK_STOP;
                default:
                    throw new InvalidParameterException("Value: "+a+" cannot be converted to ARPD_MESSAGE_TYPE");
            }
        }
        
        private ARPD_MESSAGE_TYPE(final int b,final Class associated_class_)
        {
            numval=(short)b;
            associated_class=associated_class_;
        }
        
        public short getNumVal()
        {
            return numval;
        }
        
        public Class getAssociatedClass()
        {
            return associated_class;
        }
        
    };
    public static class ActionDelayBits
    {
        
        private short action_delay;
        
        // binary : 00000000 00000000     0001 1111 1111 1111
        //                00       00        1    F    F    F
        final public static int mask=0x1FFF;
        public static short sanitizeInt(int a)
        {
            return (short)(a & mask);
        }
        
                                                            
        public ActionDelayBits(int a)
        {//we only want the lower 8+5=13 bits
            action_delay= sanitizeInt(a);
        }
        
        public ActionDelayBits()
        {//we only want the lower 8+5=13 bits
            action_delay=0;
        }
        
        public void setActionDelay(int a)
        {
            action_delay= sanitizeInt(a);
        }
        
        public short getActionDelay()
        {
            return action_delay;
        }

        @Override
        public String toString() {
            return "ActionDelayBits{" + "action_delay=" + action_delay + '}';
        }
        
        
        public String toString(int i) {
            i++;
            String spaces=nTabs(i);
            return "ActionDelayBits {" +spaces+ "action_delay=" + action_delay + '}';
        }
        
        
        
    }
    public static final class MACAddress
    {
        protected final byte[] bytes;
        public static final int BYTES=6;

        public MACAddress()
        {
            bytes= new byte[MACAddress.BYTES];
            resetBytes();
        }
        public MACAddress(String s)
        {
            bytes= new byte[6];
            
            //expect xx:xx:xx:xx:xx:xx = 6*2+5=17 chars
            if(s.length()!=17)
                throw new InvalidParameterException("The string : "+s+" is not a valid MAC address in the format xx:xx:xx:xx:xx:xx");
            String r=s.replaceAll(":", "").replaceAll("-", "").toUpperCase();//strip the ":" and "-" if they were used
            long l=Long.parseLong(r,16);//parse the number we obtained in a long
            l=(l & 0x0000FFFFFFFFFFFFL);//we mask the 2 highest bytes just in case
            for (int i = 0; i < 6; i++) 
            {
                bytes[5-i] = (byte)(l & 0xFF);
                l >>= 8;//shift right by 1 byte
            }
        }
        public MACAddress(byte[] i)
        {
            bytes= new byte[MACAddress.BYTES];
            setBytes(i);
        }
        
        
        public void resetBytes()
        {
            for(int i=0;i<bytes.length;i++)
            {
                bytes[i]=0;
            }
        }

        public void setBytes(byte[] b)
        {
            if(b.length>bytes.length)
                throw new java.lang.ArrayIndexOutOfBoundsException();
            for(int i=0;i<b.length;i++)
                bytes[i]=b[i];
        }

        public byte[] getBytes()
        {
            return bytes;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(18);
            for (byte b : bytes) {
                if (sb.length() > 0)
                    sb.append(':');
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Arrays.hashCode(this.bytes);
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
            final MACAddress other = (MACAddress) obj;
            if (!Arrays.equals(this.bytes, other.bytes)) {
                return false;
            }
            return true;
        }
 
        
    }
    public static class Suffix
    {

        public static class Signature 
        {
            protected final byte[] bytes;
            public static final int BYTES=32;
            
            //protected constructor, use getSignatureFrom to create an instance
            protected Signature(byte[] hash) throws java.lang.ArrayIndexOutOfBoundsException
            {
                bytes= new byte[Signature.BYTES];                
                if(hash.length>bytes.length)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                for(int i=0;i<hash.length;i++)
                    bytes[i]=hash[i];
            }

            //public interfaces 
            
            //this one is for when you have a payload, and you want to sign it
            public static Signature createSignatureFromPayload(byte[] s,byte[] password_) 
            {
                try {
                    
                    int s_length=(s==null) ? 0:s.length;
                    int passwd_length=(password_==null) ? 0:password_.length;
                    MessageDigest digester = MessageDigest.getInstance("SHA-256");
                    ByteBuffer bb= ByteBuffer.allocate(s_length+passwd_length);
                    if(s!=null)
                        bb.put(s);
                    if(password_!=null)
                        bb.put(password_);
                    bb.flip();
                    byte[] r=new byte[bb.limit()];
                    bb.get(r);
                    byte[] hash = digester.digest(r);
                    return new Signature(hash);
                } 
                catch (NoSuchAlgorithmException | java.lang.ArrayIndexOutOfBoundsException ex) {
                    Logger.getLogger(ARPDMessage.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
                
            }
            
            //this one is when you have received a message and you want to
            //store the message's signature somewhere
            public static Signature fillSignatureWithHash(byte[] s)
            {
                try {
                    return new Signature(s);    
                } 
                catch (java.lang.ArrayIndexOutOfBoundsException ex) {
                    Logger.getLogger(ARPDMessage.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }               
            }

            public boolean isValidForPayload(byte[] data, byte[] password)
            {
                Signature s=createSignatureFromPayload(data,password);
                if(this.equals(s))
                    return true;
                else
                    return false;
            }
            
            public byte[] toBytes()
            {
                return bytes.clone();
            }
            
            public Signature duplicate()
            {
                return fillSignatureWithHash(this.toBytes());
            }
            
            
            @Override
            public int hashCode() {
                int hash = 7;
                hash = 89 * hash + Arrays.hashCode(this.bytes);
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
                final Signature other = (Signature) obj;
                if (!Arrays.equals(this.bytes, other.bytes)) {
                    return false;
                }
                return true;
            }
            
            //protected functions, right now unnecessary, but who knows in the future
            protected void resetBytes()
            {
                for(int i=0;i<bytes.length;i++)
                {
                    bytes[i]=0;
                }
            }


            protected void setBytes(byte[] b) throws java.lang.ArrayIndexOutOfBoundsException
            {
                if(b.length>bytes.length)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                for(int i=0;i<b.length;i++)
                    bytes[i]=b[i];
            }

            @Override
            public String toString() {
                return "Signature{" + "bytes=" + bytesToHex(bytes) + '}';
            }

            public String toString(int i) 
            {
                String spaces=nTabs(i);
                i++;
                return "Signature "
                    +spaces+'{'
                    +spaces+"\t"+ "bytes=" + bytesToHex(bytes)
                    +spaces+" }";
            }
            
        }
        
        
        protected final long timestamp;
        protected final int noonce;
        protected final Signature signature;
        protected final byte[] timestamp_and_noonce_bytes;
        
        public static final int LENGTH_TIMESTAMP_NOONCE=Integer.BYTES+Long.BYTES;
        public static final int BYTES=LENGTH_TIMESTAMP_NOONCE+Signature.BYTES;
        public static final long TIMESTAMP_HALF_WINDOW_MS=500;
        final byte[] payload;
        
        //CONSTRUCTOR 1 : when you have a payload, a noonce and want to sign&send it
        protected Suffix(final byte[] payload_,final int noonce_,final byte[] password)
        {
            
            timestamp= System.currentTimeMillis();
            noonce=noonce_;
            payload=payload_;
            timestamp_and_noonce_bytes=timestampAndNoonceToBytes();
            
            int payload_length=payload.length;
            ByteBuffer buff=ByteBuffer.allocate(payload_length+LENGTH_TIMESTAMP_NOONCE);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(payload);
            buff.put(timestamp_and_noonce_bytes);
            buff.flip();            
            byte[] r= new byte[buff.limit()];
            buff.get(r);
            signature=Signature.createSignatureFromPayload(r,password);
        }
         //CONSTRUCTOR 2 : when you received a message and want to interpret it
        protected Suffix(final byte[] complete_message) throws InvalidParameterException
        {
            if(complete_message.length<Suffix.BYTES)
                throw new InvalidParameterException("The message to be interpreted is too short");
            ByteBuffer buff=ByteBuffer.allocate(complete_message.length);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(complete_message);
            buff.flip();//sets limit()
            
            //so let's start by copying the payload (eveything up to size - size_of_suffix)
            int length_payload=complete_message.length-Suffix.BYTES;
            payload= new byte[length_payload];
            buff.get(payload);
            //here we want to take the raw bytes and interpret them at same time
            //so we slice the bytebuffer to get a "copy" from current position onwards
            ByteBuffer sliced_for_double_take=buff.slice();
            //then we grab the raw bytes
            timestamp_and_noonce_bytes= new byte[LENGTH_TIMESTAMP_NOONCE];
            sliced_for_double_take.get(timestamp_and_noonce_bytes);
            //then the sliced object is discarded and we can just grab directly 
            //the values this time from the position before the slice so:
            //next is  noonce
            noonce=buff.getInt();
            //then timestamp;
            timestamp=buff.getLong();
            //then the rest is the signature
            byte[] copy_of_signature =new byte[buff.remaining()];
            buff.get(copy_of_signature);
            signature=Signature.fillSignatureWithHash(copy_of_signature);            
        }
        
        
        public boolean isValid(byte [] password,long timestamp_for_comparison)
        {
            //TODO ajouter toutes les autres verifs
            boolean r= this.hasValidSignatureForPayload(password);
            r= r && hasValidTimestamp(timestamp_for_comparison);
            return r;
        }
        public boolean hasValidTimestamp(long compared_to)
        {
            long low =this.timestamp-TIMESTAMP_HALF_WINDOW_MS;
            long high =this.timestamp+TIMESTAMP_HALF_WINDOW_MS;
            boolean r=
                    (compared_to <= high )
                    &&
                    (compared_to >= low );
            logger.log(Level.INFO, ""+compared_to+"-> ["+low+" ; "+high+"]");
            return r;
        }
        
        public byte[] timestampAndNoonceToBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(LENGTH_TIMESTAMP_NOONCE);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.putInt(noonce);
            buff.putLong(timestamp);
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);
        }
        
        public byte[] toBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(Suffix.BYTES);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(timestamp_and_noonce_bytes);
            buff.put(signature.toBytes());
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);
        }
        
        public boolean hasValidSignatureForPayload(byte [] password)
        {
            ByteBuffer buff=ByteBuffer.allocate(payload.length+LENGTH_TIMESTAMP_NOONCE);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(payload);
            buff.put(timestamp_and_noonce_bytes);
            buff.flip();            
            byte[] r= new byte[buff.limit()];
            buff.get(r);
            return signature.isValidForPayload(r,password);
        }
        
        public long getTimestamp() {
            return timestamp;
        }

        public int getNoonce() {
            return noonce;
        }

        public byte[] getPayload() {
            return payload;
        }

        @Override
        public String toString() {
            return "Suffix{" + "timestamp_and_noonce_bytes=" + bytesToHex(timestamp_and_noonce_bytes)+ "\ntimestamp=" + timestamp + ", noonce=" + noonce + "\nsignature=" + signature  + "\npayload=" + bytesToHex(payload) +'}';
        }

        public String toString(int i) 
        {
            return toString(i,null,System.currentTimeMillis());
        
        }
        public String toString(int i,byte[] passwd, long timestamp_for_comparison) 
        {
            String spaces=nTabs(i);
            i++;
            return "Suffix : "+ "IS_VALID ?:"+isValid(passwd,timestamp_for_comparison)+"(Signature valid:"+hasValidSignatureForPayload(passwd)+" ; Timestamp valid :"+hasValidTimestamp(timestamp_for_comparison)
                    +spaces+'{'
                    +spaces+ "\t"+ "timestamp_and_noonce_bytes=" + bytesToHex(timestamp_and_noonce_bytes)
                    +spaces+ "\t"+"timestamp=" + timestamp + ", noonce=" + noonce 
                    +spaces+ "\t"+"signature=" + signature.toString(i)
                    +spaces+ "\t"+"payload=" + bytesToHex(payload)
                    +spaces+'}';
        
        }
        
    }
    
    
    
    
    
    
    public static final int Inet4Address_BYTES=4;
    public static Inet4Address getIpv4AddressFromString(String ip) throws UnknownHostException,InvalidParameterException
    {
        InetAddress a=InetAddress.getByName(ip);
        if(a.getClass() != Inet4Address.class)
            throw new InvalidParameterException("This IP is not v4, this demo is only valid for IPv4 no IPv6");
        return (Inet4Address) a;
    }
    public static Inet4Address getIpv4AddressFromBytes(byte[] ip) throws UnknownHostException,InvalidParameterException
    {
        if(ip.length!=Inet4Address_BYTES)
            throw new InvalidParameterException("The length of this byte doesn't correspond to an ipv4 address");
        InetAddress a=InetAddress.getByAddress(ip);
        if(a.getClass() != Inet4Address.class)
            throw new InvalidParameterException("This IP is not v4, this demo is only valid for IPv4 no IPv6");
        return (Inet4Address) a;
    }
    
    public static ARPD_MESSAGE_TYPE getMsgTypeFromBytes(byte[] msg) throws InvalidParameterException
    {
        if(msg.length<1)
            throw new InvalidParameterException("The msg whose type is to be determined is empty");
        int first=msg[0] & 0xFF;//ANDing to 1byte needed to remove problems with upcasting signed "negative" values
        first=first  >> 6;//shift right 6 times to get only the first 2 bits
        return ARPD_MESSAGE_TYPE.getFromValue(first);
    }
    
    
    public static <T extends ARPDMessage> T fromBytes(ARPD_MESSAGE_TYPE type,byte[] msg) throws InvalidParameterException, UnknownHostException
    {
        try{
            Class<T> return_class=type.getAssociatedClass();
            Class[] parameters_class=new Class[] {ARPD_MESSAGE_TYPE.class,byte[].class};
            Object[] parameters=new Object[]{ARPD_MESSAGE_TYPE.ORDER_START_ARPD,msg};
            return return_class.getDeclaredConstructor(parameters_class).newInstance(parameters);
        }
        catch (InstantiationException | IllegalAccessException|  IllegalArgumentException|  InvocationTargetException|  NoSuchMethodException ex)
        {
            throw new InvalidParameterException("Could not translate the message into an instance of ARPDMessage's children");
        }
    }
    
    public abstract int BYTES();//returns the size of one message of this type
    public abstract byte[] payloadToBytes();
    public abstract byte[] toBytes();
    
    public String toString(int i)
    {
        return toString(i,null,System.currentTimeMillis()); 
    }
    public abstract String toString(int i, byte[] passwd,long timestamp_for_comparison);
    public abstract ARPD_MESSAGE_TYPE getMsg_type();
        
    public abstract Suffix getSuffix();
    public abstract Inet4Address getIP_src();
    public abstract MACAddress getMAC_src();

    
    
    
    
    public static class ARPDOrder extends  ARPDMessage
    {
        
        @Override
        public int BYTES()
        {
            return PAYLOAD_LENGTH+Suffix.BYTES;
        }
        
        
        final ARPD_MESSAGE_TYPE msg_type;
        final boolean everyone_acts_or_only_dst;
        final Inet4Address IP_src;
        final MACAddress MAC_src;
        final ActionDelayBits  AD;
        final Inet4Address IP_dst;
        final Suffix suffix;
        
        public static final int PAYLOAD_LENGTH=
                2+//the flag and action delay
                2*Inet4Address_BYTES+//the 2 IP addresses
                MACAddress.BYTES;//the source MAC address
        
        
        //CONSTRUCTOR 1 : when you create an order to send it
        //this one is public because you know which kind of message you want to send
        public ARPDOrder(final ARPD_MESSAGE_TYPE msg_type_,final boolean everyone_acts_or_only_dst_,final  short action_delay ,final Inet4Address ip_src,final MACAddress mac_src,final Inet4Address ip_dst,final int order_nb, final byte[] passwd )
        {
            msg_type=msg_type_;
            IP_src= ip_src;
            MAC_src=mac_src;  
            everyone_acts_or_only_dst=everyone_acts_or_only_dst_;
            AD= new ActionDelayBits(action_delay);
            IP_dst= ip_dst;

            byte[] payload=payloadToBytes();           
            suffix=new Suffix(payload,order_nb,passwd); 
        }
        
        //CONSTRUCTOR 2 : when you receive an order and want to read it
        //this one is not public because you don't know which kind of message you received
        //so you use ARPDMessage.fromBytes() to get it
        protected ARPDOrder(final ARPD_MESSAGE_TYPE type,final byte[] msg) 
                throws InvalidParameterException, UnknownHostException
        {
            if(type != ARPD_MESSAGE_TYPE.ORDER_START_ARPD && type != ARPD_MESSAGE_TYPE.ORDER_STOP_ARPD)
                throw new InvalidParameterException("This msg is not of the right type to be an order");
            if(msg.length!=this.BYTES())
                throw new InvalidParameterException("This msg doesn't have the right size for an order");
            
            suffix=new Suffix(msg);
            
            ByteBuffer buff=ByteBuffer.allocate(ARPDOrder.PAYLOAD_LENGTH);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(suffix.getPayload());//Suffix has already separated the payload from the suffix for us
            buff.flip();
        // THE HEADER (FIRST TWO BYTES here since its easier not to separate them)
            //let's setup the first 2 bytes : Flag and Action_DELAY inside the same short
            //FLAG contains: Msg_type(1b) | Everyone_acts_or_only_dst(2b) | the highest 5 bits of Action_DELAY
                //store everything in integers that we will bit-shift
            int header=buff.getShort() & 0xFFFF;//get the short at the start and upcast to int for bitwise ops without adding ones on the left (hence ANDing)
            msg_type=type;
            int storage_boolean=(header >>> 13) & 1;////unsigned right shift and bitwise AND
            everyone_acts_or_only_dst= (storage_boolean==1);
            AD=new ActionDelayBits(header);//everything's automatic for this one
                    
        // SOURCE IP
            byte [] src_ip_buff= new byte [Inet4Address_BYTES];
            buff.get(src_ip_buff);
            IP_src= getIpv4AddressFromBytes(src_ip_buff);         
        // SOURCE MAC
            byte [] src_mac_buff= new byte [MACAddress.BYTES];
            buff.get(src_mac_buff);
            MAC_src=new MACAddress(src_mac_buff);
        // DESTINATION IP
            byte [] address_buff= new byte [Inet4Address_BYTES];
            buff.get(address_buff);
            IP_dst= getIpv4AddressFromBytes(address_buff);
        }
        
        public boolean isValid(byte[] passwd,long timestamp_for_comparison)
        {
            //TODO add other ocnditions
            return suffix.isValid(passwd, timestamp_for_comparison);
        }
        
        
        @Override
        public byte[] payloadToBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(this.PAYLOAD_LENGTH);          
            buff.order( ByteOrder.BIG_ENDIAN);
        // THE HEADER (FIRST TWO BYTES here since its easier not to separate them)
            //let's setup the first 2 bytes : Flag and Action_DELAY inside the same short
            //FLAG contains: Msg_type(1b) | Everyone_acts_or_only_dst(2b) | the highest 5 bits of Action_DELAY
                //store everything in integers that we will bit-shift
            int storage_msg_type=msg_type.getNumVal();
            int storage_boolean=everyone_acts_or_only_dst ? 1:0;
            int storage_ad_1=AD.getActionDelay();
                //shift everything left to the appropriate spot
            storage_msg_type=storage_msg_type << 14; //bitshift so that the msg_type is first once we convert eveyhing back to short
            storage_boolean=storage_boolean<<13;//this boolean is next
                    //and the AD should already be properly set up
                //let's concatenate everything
            storage_ad_1=storage_ad_1 | storage_boolean | storage_msg_type; //the order doesn't matter
            //now let's downcast it to short
            short actual_first_two_bytes=(short)storage_ad_1;
            
            buff.putShort(actual_first_two_bytes);          
        // SOURCE IP
            buff.put(IP_src.getAddress());
        // SOURCE MAC
            buff.put(MAC_src.getBytes());
        // DESTINATION IP
            buff.put(IP_dst.getAddress());
        //FINISHED !
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);          
        }
        
        @Override
        public byte[] toBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(this.BYTES());          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(suffix.getPayload());
            buff.put(suffix.toBytes());      
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);  
        }

        @Override
        public String toString() {
            return "ARPDOrder{ msg_type=" + msg_type + ", everyone_acts_or_only_dst=" + everyone_acts_or_only_dst + ", IP_src=" + IP_src + ", MAC_src=" + MAC_src + ", AD=" + AD + ", IP_dst=" + IP_dst + ", suffix=" + suffix + '}';
        }

        
        @Override
        public String toString(int i, byte[] passwd,long timestamp_for_comparison) 
        {
            String spaces=nTabs(i);
            i++;
            return "ARPDOrder"+ " IS_VALID ? "+ isValid(passwd, timestamp_for_comparison)
                    +spaces+'{'
                    +spaces+"\t"+"msg_type=" + msg_type + ", everyone_acts_or_only_dst=" + everyone_acts_or_only_dst +", AD=" + AD 
                    +spaces+"\t"+ ", IP_src=" + IP_src + ", MAC_src=" + MAC_src 
                    +spaces+"\t"+ ", IP_dst=" + IP_dst 
                    +spaces+"\t"+ ", suffix=" + suffix.toString(i,passwd, timestamp_for_comparison)
                    +spaces+"}";        
        }


        public boolean isEveryone_acts_or_only_dst() {
            return everyone_acts_or_only_dst;
        }

        @Override
        public ARPD_MESSAGE_TYPE getMsg_type() {
            return msg_type;
        }
        
        @Override
        public Suffix getSuffix() {
            return suffix;
        }
        
        @Override
        public Inet4Address getIP_src() {
            return IP_src;
        }

        @Override
        public MACAddress getMAC_src() {
            return MAC_src;
        }

        public ActionDelayBits getAD() {
            return AD;
        }

        public Inet4Address getIP_dst() {
            return IP_dst;
        }

        
    }
     public static class ARPDAnswer extends  ARPDMessage
    {
        
        @Override
        public int BYTES()
        {
            return PAYLOAD_LENGTH+Suffix.BYTES;
        }
        
        
        final ARPD_MESSAGE_TYPE msg_type;
        final boolean answer_is_1_confirmation_is_0;
        final Inet4Address IP_src;
        final MACAddress MAC_src;
        final Suffix suffix;
        
        public static final int PAYLOAD_LENGTH=
                1+//the flag and action delay
                Inet4Address_BYTES+//the src IP addresses
                MACAddress.BYTES;//the source MAC address
        
        
        //CONSTRUCTOR 1 : when you create a response to send it
        //this one is public because you know which kind of message you want to send
        public ARPDAnswer(final ARPD_MESSAGE_TYPE msg_type_,final boolean answer_is_1_confirmation_is_0_,final Inet4Address ip_src,final MACAddress mac_src,final int order_nb,final byte[] passwd )
        {
            msg_type=msg_type_;
            IP_src= ip_src;
            MAC_src=mac_src;            
            answer_is_1_confirmation_is_0=answer_is_1_confirmation_is_0_;

            byte[] payload=payloadToBytes();           
            suffix=new Suffix(payload,order_nb,passwd); 
        }
        
        //CONSTRUCTOR 2 : when you receive an order and want to read it
        //this one is not public because you don't know which kind of message you received
        //so you use ARPDMessage.fromBytes() to get it
        protected ARPDAnswer(final ARPD_MESSAGE_TYPE type,final byte[] msg) 
                throws InvalidParameterException, UnknownHostException
        {
            if(type != ARPD_MESSAGE_TYPE.ORDER_START_ARPD || type != ARPD_MESSAGE_TYPE.ORDER_STOP_ARPD)
                throw new InvalidParameterException("This msg is not of the right type to be an order");
            if(msg.length!=this.BYTES())
                throw new InvalidParameterException("This msg doesn't have the right size for an order");
            
            suffix=new Suffix(msg);
            
            ByteBuffer buff=ByteBuffer.allocate(ARPDOrder.PAYLOAD_LENGTH);          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(suffix.getPayload());//Suffix has already separated the payload from the suffix for us
            buff.flip();
        // THE HEADER (FIRST TWO BYTES here since its easier not to separate them)
            //let's setup the first 2 bytes : Flag and Action_DELAY inside the same short
            //FLAG contains: Msg_type(1b) | Everyone_acts_or_only_dst(2b) | the highest 5 bits of Action_DELAY
                //store everything in integers that we will bit-shift
            int header=buff.get() & 0xFF;//get the byte at the start and upcast to int for bitwise ops without adding ones on the left (hence ANDing)
            msg_type=type;
            int storage_boolean=(header >>> 5) & 1;////unsigned right shift and bitwise AND
            answer_is_1_confirmation_is_0= (storage_boolean==1);
                    
        // SOURCE IP
            byte [] src_ip_buff= new byte [Inet4Address_BYTES];
            buff.get(src_ip_buff);
            IP_src= getIpv4AddressFromBytes(src_ip_buff);         
        // SOURCE MAC
            byte [] src_mac_buff= new byte [MACAddress.BYTES];
            buff.get(src_mac_buff);
            MAC_src=new MACAddress(src_mac_buff);
        }
        
        @Override
        public byte[] payloadToBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(this.PAYLOAD_LENGTH);          
            buff.order( ByteOrder.BIG_ENDIAN);
        // THE HEADER (FIRST TWO BYTES here since its easier not to separate them)
            //let's setup the first 2 bytes : Flag and Action_DELAY inside the same short
            //FLAG contains: Msg_type(1b) | Everyone_acts_or_only_dst(2b) | the highest 5 bits of Action_DELAY
                //store everything in integers that we will bit-shift
            int storage_msg_type=msg_type.getNumVal();
            int storage_boolean=answer_is_1_confirmation_is_0 ? 1:0;
                //shift everything left to the appropriate spot
            storage_msg_type=storage_msg_type << 6; //bitshift so that the msg_type is first once we convert eveyhing back to short
            storage_boolean=storage_boolean<<5;//this boolean is next
                //let's concatenate everything
            storage_msg_type= storage_boolean | storage_msg_type; //the order doesn't matter
            //now let's downcast it to byte
            byte actual_first_bytes=(byte)storage_msg_type;
            
            buff.put(actual_first_bytes);
        // SOURCE IP
            buff.put(IP_src.getAddress());
        // SOURCE MAC
            buff.put(MAC_src.getBytes());
        //FINISHED !
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);        
        }
        
        @Override
        public byte[] toBytes()
        {
            ByteBuffer buff=ByteBuffer.allocate(this.BYTES());          
            buff.order( ByteOrder.BIG_ENDIAN);
            buff.put(suffix.getPayload());
            buff.put(suffix.toBytes());      
            buff.flip();
            return getAllRemainingBytesFromByteBuffer(buff,false);  
        }
        
        
        public boolean isValid(byte[] passwd,long timestamp_for_comparison)
        {
            //TODO add other ocnditions
            return suffix.isValid(passwd,timestamp_for_comparison);
        }
        
        @Override
        public String toString(int i, byte[] passwd,long timestamp_for_comparison) 
        {
            String spaces=nTabs(i);
            i++;
            return "ARPDAnswer"+" IS_VALID : "+isValid(passwd, timestamp_for_comparison)
                    +spaces+'{'
                    +spaces+"\t"+"msg_type=" + msg_type + ", answer_is_1_confirmation_is_0=" + answer_is_1_confirmation_is_0
                    +spaces+"\t"+ ", IP_src=" + IP_src + ", MAC_src=" + MAC_src 
                    +spaces+"\t"+ ", suffix=" + suffix.toString(i,passwd,timestamp_for_comparison)
                    +spaces+"}";        
        }

        @Override
        public ARPD_MESSAGE_TYPE getMsg_type() {
            return msg_type;
        }

        public boolean isAnswer_is_1_confirmation_is_0() {
            return answer_is_1_confirmation_is_0;
        }

        @Override
        public Inet4Address getIP_src() {
            return IP_src;
        }

        @Override
        public MACAddress getMAC_src() {
            return MAC_src;
        }

        @Override
        public Suffix getSuffix() {
            return suffix;
        }
        
        
    }
}
