package gogog22510.dht.core;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class ClientEcho
{
	public static void main( String args[] ) throws Exception 
	{
		InetAddress add = InetAddress.getByName(args[0]);   

		DatagramSocket dsock = new DatagramSocket( );
		String message1 = "This is client calling";    
		byte arr[] = message1.getBytes( );  
		DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, 7);
		dsock.send(dpack);                                   // send the packet
		Date sendTime = new Date();                          // note the time of sending the message

		dsock.receive(dpack);                                // receive the packet
		String message2 = new String(dpack.getData( ));
		Date receiveTime = new Date( );   // note the time of receiving the message
		System.out.println((receiveTime.getTime( ) - sendTime.getTime( )) + " milliseconds echo time for " + message2);
	}
}