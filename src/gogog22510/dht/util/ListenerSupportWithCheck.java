package gogog22510.dht.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * A modified version of quickfix.ListenerSupport class that will do the existence checking
 * before actually adding and removing listener from listener list (support thread safe)
 * 
 * @author charles
 *
 */
public class ListenerSupportWithCheck {
    private final List<Object> listeners = new CopyOnWriteArrayList<Object>();
    private final Object multicaster;

    public ListenerSupportWithCheck(Class<?> listenerClass) {
        multicaster = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class<?>[] { listenerClass }, new ListenerInvocationHandler());
    }

    public Object getMulticaster() {
        return multicaster;
    }

    private class ListenerInvocationHandler implements InvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString") && args.length == 0) {
                return getClass().getSimpleName() + "@" + System.identityHashCode(proxy);
            } else if (method.getDeclaringClass() == Object.class) {
                return method.invoke(proxy, args);
            }
            for (Object listener : listeners) {
                method.invoke(listener, args);
            }
            return null;
        }
    }

    public void addListener(Object listener) {
    	if(!listeners.contains(listener))
    		listeners.add(listener);
    }

    public void removeListener(Object listener) {
    	if(listeners.contains(listener))
    		listeners.remove(listener);
    }
    
    public void clear() {
    	listeners.clear();
    }
}
