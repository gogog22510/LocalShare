package gogog22510.dht.core;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * KademliaId comparator with target key
 * @author charles
 *
 */
public class KademliaIdComparator implements Comparator<KademliaId> {
	private final BigInteger key;
	public KademliaIdComparator(KademliaId key) {
		this.key = key.getBigInt();
	}

	@Override
	public int compare(KademliaId o1, KademliaId o2) {
		BigInteger d1 = o1.getBigInt().xor(key);
		BigInteger d2 = o2.getBigInt().xor(key);
		return d1.abs().compareTo(d2.abs());
	}

}
