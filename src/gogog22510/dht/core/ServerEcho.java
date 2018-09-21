package gogog22510.dht.core;

import java.net.*;
import java.util.*;

public class ServerEcho 
{
	public static void main( String args[]) throws Exception 
	{
		DatagramSocket dsock = new DatagramSocket(7);
		byte arr1[] = new byte[150];                                
		DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

		while(true) 
		{
			dsock.receive(dpack);

			byte arr2[] = dpack.getData();
			int packSize = dpack.getLength();
			String s2 = new String(arr2, 0, packSize);

			System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ s2);
			dsock.send(dpack);
		}
	}           
}
