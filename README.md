# ARP_detox
Simple Java UDP Master/Slave apps used to counter  an ALREADY PRESENT ARP-poisonning (also works as a preventive measure, but its principal asset is being able to work enven if the MitM is already established, which usually doesn't work with other methods) on a local network and retrieve genuine IP and MAC addresses translation tables.

This projects uses Linux **Static ARP entries** to counter the ARP poisoning. Signed messages containing IP/MAC addresses are exchanged between the master and its slaves and are used to add (remove if it's over) static ARP entries that are not modified when receiving dynamic ARP messages.


## Instructions (for Netbeans)

#### Pre-configuration:
a. The java apps (whether Master or Slaves) **Must be able to modify the ARP table**, to do this the easiest solution on most Linux systems is to use a sudoers custom configuration:
   - launch *sudo visudo* from the commandline (for Ubuntu, Raspian...) (this will modify the sudoers file /etc/sudoers)
   - add a line similar to the following at the end:

     **_your-username_  ALL = (root:root) NOPASSWD: /usr/sbin/arp**

b. Open all the projects in Netbeans

c. Create a new remote platform for the RPi if you intend to use it as a slave:
   - Tools > Java Platform > Add Platform > Remote Java SE 
   - Choose a name (ex: "RPI_pumps")
   - The IP address goes in Host
   - Type the Username and Password needed to access the RPi (ex : "pi", "raspberry") 
   - Change the SSH port if needed
   - In "Remote JRE path" type "/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/" (no quotes)
   - Finish

d. Modify the ARPD_test_slave project to change the IP address *if the slave is potentially connected to severall different IPv4 networks* (if the slave is only connected to one IPv4 network, than the automatic function should work):
   - Open ARPD_test_slave > Source Package > arpd_test_slave > ARPD_test_slave.java
   - Potentially modify line 69
   - Save

e. Modify the ARPD_test_master project to change the IP addresses:
   - Open ARPD_test_master > Source Package > arpd_test_master > ARPD_test_master.java
   - Modify lines 45 and 50
   - Save

f. If you want to have access to logs (logged exchanged messages and actions taken), on each machine (use SSH on the slave RPi for example):
   - Connected as the user you selected in **c.**, create a new folder named "ARPD_logs" right under your home directory : "mkdir /home/_your-username_/ARPD_logs"

#### Remote slave startup:
1. Remotely launch the ARPD_test_slave project on the corresponding RPI:
   - Right click on the project's name > Properties > Run tab 
   - Select the corresponding Runtime Platform (it will probably say you have to create a new configuration, do so if needed)
   - OK
   - Run > Run project

#### Local master (client) startup:
1. Start the ARPD_test_master project on the current machine :
   - Run > Run project

#### Control:
You can easily start/stop the countermeasure and see the corredponding logs if you created the log directory (/home/_your-username_/ARPD_logs).
**Before closing, don't forget to stop the counter-measure if you don't want to see your ARP table polluted with unused static ARP entries !**

## Major limitations:
This project was **only a Proof-of-Concept**, as such:
 1. It only works with a hardcoded symetric key ("lala" right now) that is used to create an HMAC with SHA-256. It wouldn't be any problem to change the password source from hardcoded to a file for example. **However going from this HMAC to a proper asymetric cryptography signature would require significant code overhaul.**
 2. It only works with IPv4, making it work for IPv6 as well shouldn't be too much of an issue, the length of the exchanged messages as well as the serialization/deserialization of the messages must be changed.
 3. It only works on **one** of the master's possible connected networks (and only works for one symetric key instead of one for each connected subnets as such), it shouldn't be too hard to move on to several networks from there.

