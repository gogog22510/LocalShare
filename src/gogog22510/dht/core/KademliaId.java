package gogog22510.dht.core;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/**
 * KadeliaId
 * @author charles
 *
 */
public class KademliaId {
	public final static int ID_LENGTH = 160; // sha1 160 bits

	public static KademliaId randomId() {
		byte[] b = new byte[ID_LENGTH/8];
		new Random().nextBytes(b);
		return new KademliaId(b);
	}

	private byte[] keyBytes;

	public KademliaId(byte[] data) {
		if(data.length != (ID_LENGTH / 8)) throw new RuntimeException("not a valid id length");
		keyBytes = data;
	}

	public KademliaId(String data) throws NoSuchAlgorithmException {
		keyBytes = KademliaUtil.sha1Hash(data);
	}

	public byte[] getBytes() {
		return this.keyBytes;
	}

	public BigInteger getBigInt() {
		return new BigInteger(1, keyBytes);
	}

	/**
	 * Checks the distance between this and another Node
	 */
	public KademliaId xor(KademliaId nid)
	{
		int len = ID_LENGTH / 8;
		byte[] result = new byte[len];
		byte[] nidBytes = nid.getBytes();

		for (int i = 0; i < len; i++)
		{
			result[i] = (byte) (this.keyBytes[i] ^ nidBytes[i]);
		}

		KademliaId resNid = new KademliaId(result);

		return resNid;
	}

	/**
	 * Counts the number of leading 0's in this NodeId
	 */
	public int getFirstSetBitIndex()
	{
		int prefixLength = 0;

		for (byte b : this.keyBytes)
		{
			if (b == 0)
			{
				prefixLength += 8;
			}
			else
			{
				/* If the byte is not 0, we need to count how many MSBs are 0 */
				int count = 0;
				for (int i = 7; i >= 0; i--)
				{
					boolean a = (b & (1 << i)) == 0;
					if (a)
					{
						count++;
					}
					else
					{
						break;   // Reset the count if we encounter a non-zero number
					}
				}

				/* Add the count of MSB 0s to the prefix length */
				prefixLength += count;

				/* Break here since we've now covered the MSB 0s */
				break;
			}
		}
		return prefixLength;
	}

	/**
	 * Gets the distance from this NodeId to another NodeId
	 */
	public int getDistance(KademliaId to) {
		/**
		 * Compute the xor of this and to
		 * Get the index i of the first set bit of the xor returned NodeId
		 * The distance between them is ID_LENGTH - i
		 * The value range can be 0 ~ 160 (0 is itself)
		 */
		return ID_LENGTH - this.xor(to).getFirstSetBitIndex();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + Arrays.hashCode(this.keyBytes);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;
		if(obj instanceof KademliaId) {
			return this.hashCode() == obj.hashCode();
		}
		else {
			return false;
		}
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException {
		KademliaId kid = new KademliaId("192.168.0.103");
		System.out.println(kid);
		System.out.println(kid.hashCode());
	}
}
