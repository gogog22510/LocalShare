package gogog22510.dht.util;

import java.util.HashMap;
import java.util.Set;

//one to one mapping

public class TwoWayMap<T1, T2> {
    private HashMap<T1, T2> firstToSecond = new HashMap<T1, T2>();
    private HashMap<T2, T1> secondToFirst = new HashMap<T2, T1>();

    public void put(T1 first, T2 second) {
    	if(firstToSecond.containsKey(first)){
    		secondToFirst.remove(firstToSecond.get(first));
    	} else if(secondToFirst.containsKey(second)){
    		firstToSecond.remove(secondToFirst.get(second));
    	}
        firstToSecond.put(first, second);
        secondToFirst.put(second, first);
    }

    public T2 getFirst(T1 first) {
        return firstToSecond.get(first);
    }

    public T1 getSecond(T2 second) {
        return secondToFirst.get(second);
    }
    
    public void removeFirst(T1 first){
    	T2 second = firstToSecond.get(first);
    	firstToSecond.remove(first);
    	secondToFirst.remove(second);
    }

    public void removeSecond(T2 second){
    	T1 first = secondToFirst.get(second);
    	secondToFirst.remove(second);
    	firstToSecond.remove(first);
    }
    
    public boolean containsKeyFirst(T1 first){
    	return firstToSecond.containsKey(first);
    }
    
    public boolean containsKeySecond(T2 second){
    	return secondToFirst.containsKey(second);
    }
    
    public Set<T1> keySetFirst(){
    	return firstToSecond.keySet();
    }
    
    public Set<T2> keySetSecond(){
    	return secondToFirst.keySet();
    }
    
    public void putAll(TwoWayMap<T1, T2> map){
    	if(map.isEmpty()){
    		return;
    	}
    	for(T1 first : map.keySetFirst()){
    		T2 second = map.getFirst(first);
    		put(first, second);
    	}
    }
    
    public int size(){
    	return firstToSecond.size();
    }
    
    public boolean isEmpty(){
    	return firstToSecond.size()==0;
    }
    
    public void teardown() {
    	firstToSecond.clear();
    	secondToFirst.clear();
    }
    
    public String toString() {
        return firstToSecond.toString() + "\n" + secondToFirst.toString();
    }
}
