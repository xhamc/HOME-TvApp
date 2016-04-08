package com.sony.sel.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A Set of observers of any interface type that can all be notified
 * by making a single method call on a {@link #proxy() proxy} object provided by the ObserverSet.
 * The proxy implements the same interface as the observers and serves
 * as a multiplexer.
 * 
 * @author glewis
 *
 * @param <T> The type of observer
 */
public class ObserverSet<T> extends CopyOnWriteArraySet<T> {
    private static final long serialVersionUID = 1L;
    
    /**
     * Determines if and how invocation of listeners should be performed.
     * The when and where is determined by the {@link DispatchMethod}.
     * 
     * @author glewis
     *
     */
    public static class Dispatcher {
        
        /** returns a singleton dispatcher that always notifies each listener and does nothing else. */
        public static final Dispatcher SIMPLE_DISPATCHER = new Dispatcher();
        
        /**
         * called when the proxy method is invoked and determines whether or not invocation should proceed
         * at a time determined by the {@link DispatchMethod}.
         * 
         * @return true to perform invocation or false to cancel notification for this call.
         */
        public boolean prepare(Set<?> observers, Method method, Object[] args) {
            return true;
        }
        
        /**
         * called when the {@link DispatchMethod} determines it is time to notify the listeners.
         */
        public void invoke(Set<?> observers, Method method, Object[] args) {
        	if (!observers.isEmpty()) {
	            for (Object observer : observers) {
	                try {
	                    method.invoke(observer, args);
	                }
	                catch (Exception e) {
	                    // ignore
	                }
	            }
        	}
        }
    }

    /**
     * Indicates when listeners should be notified and on what thread they should be notified.
     * If and how they should be notified is determined by the {@link Dispatcher}.
     */
    public enum DispatchMethod {
        
        /** notifies all listeners on the current thread. */
        SYNCHRONOUS {
            void invoke(Set<?> observers, Method method, Object[] args, final Dispatcher dispatcher) {
                dispatcher.invoke(observers, method, args);
            }
        },
        
        /**
         * notifies all listeners from the UI thread.
         * This will be synchronous if currently on the UI thread.
         */
        ON_UI_THREAD {
            void invoke(final Set<?> observers, final Method method, final Object[] args, final Dispatcher dispatcher) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    public void run() {
                        dispatcher.invoke(observers, method, args);
                    }
                });
            }
        },
        
        /**
         * notifies all listeners from the UI thread.
         * This will always be asynchronous, even if currently on the UI thread.
         */
        POST_ON_UI_THREAD {
            void invoke(final Set<?> observers, final Method method, final Object[] args, final Dispatcher dispatcher) {
                ThreadUtils.postOnUiThread(new Runnable() {
                    public void run() {
                        dispatcher.invoke(observers, method, args);
                    }
                });
            }
        },
        
        /**
         * notifies all listeners from a non-UI thread.
         * This will be synchronous if currently not on the UI thread.
         * This can result in calls being made from multiple threads simultaneously.
         */
        OFF_UI_THREAD {
            void invoke(final Set<?> observers, final Method method, final Object[] args, final Dispatcher dispatcher) {
                ThreadUtils.runOffUiThread(new Runnable() {
                    public void run() {
                        dispatcher.invoke(observers, method, args);
                    }
                });
            }
        },
        
        /**
         * notifies all listeners from a non-UI thread.
         * This will always be asynchronous, even if currently not on the UI thread.
         * This can result in calls being made from multiple threads simultaneously.
         */
        ASYNCHRONOUS {
            void invoke(final Set<?> observers, final Method method, final Object[] args, final Dispatcher dispatcher) {
                ThreadUtils.runElsewhere(new Runnable() {
                    public void run() {
                        dispatcher.invoke(observers, method, args);
                    }
                });
            }
        };
        
        abstract void invoke(final Set<?> observers, final Method method, final Object[] args, final Dispatcher dispatcher);
    }
    
    /**
     * This Dispatcher is used to prevent multiple callbacks of the same method from being queued up to be called.
     * For each invocation, this will effectively remove any previous calls to the same method from the queue.
     * <p>
     * This is primarily used when sending status updates and removes any intermediate updates when a future update
     * is ready.  Be careful when using this.
     * 
     * @author glewis
     *
     */
    public static class NonRepeatingDispatcher extends Dispatcher {
        private Map<Method, Object[]> mMethodMap = new HashMap<Method, Object[]>();
        
        @Override
        public boolean prepare(Set<?> observers, Method method, Object[] args) {
            synchronized (mMethodMap) {
            	if (observers.isEmpty()) {
            		mMethodMap.put(method, null); // probably more efficient than adding and removing every time.
            		return false;
            	}
            	else {
            		mMethodMap.put(method, args);
            		return true;
            	}
            }
        }
    
        @Override
        public void invoke(Set<?> observers, Method method, Object[] args) {
            synchronized (mMethodMap) {
                if (mMethodMap.get(method) != args) {
                    return;
                }
                mMethodMap.put(method, null); // probably more efficient than adding and removing every time.
            }
            super.invoke(observers, method, args);
        }
    }
    
    private T mProxy;
    
    /**
     * creates a new ObserverSet that will notify listeners synchronously.
     * 
     * @param clazz the type of observer, which must be an interface.
     * 
     * @throws IllegalArgumentException if clazz is not an interface.
     * @throws NullPointerException if clazz is null.
     */
    public ObserverSet(Class<T> clazz) {
        this(clazz, DispatchMethod.SYNCHRONOUS, Dispatcher.SIMPLE_DISPATCHER);
    }
    
    /**
     * creates a new ObserverSet that will notify listeners according to the specified DispatchMethod.
     * 
     * @param clazz the type of observer, which must be an interface.
     * @param dispatchMethod the DispatchMethod to use for notifying observers.
     * 
     * @throws IllegalArgumentException if clazz is not an interface.
     * @throws NullPointerException if either clazz or dispatchMethod is null.
     */
    public ObserverSet(Class<T> clazz, final DispatchMethod dispatchMethod) {
        this(clazz, dispatchMethod, Dispatcher.SIMPLE_DISPATCHER);
    }

    /**
     * creates a new ObserverSet that will notify listeners according to the specified DispatchMethod
     * and using the specified Dispatcher.
     * 
     * @param clazz the type of observer, which must be an interface.
     * @param dispatchMethod the DispatchMethod to use for notifying observers.
     * @param dispatcher the Dispatcher to use for notifying observers.
     * 
     * @throws IllegalArgumentException if clazz is not an interface.
     * @throws NullPointerException if clazz, dispatchMethod, or dispatcher is null.
     */
    @SuppressWarnings("unchecked")
    public ObserverSet(Class<T> clazz, final DispatchMethod dispatchMethod, final Dispatcher dispatcher) {
        if ((dispatchMethod == null) || (dispatcher == null)) {
            throw new NullPointerException();
        }
        mProxy = (T)Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (dispatcher.prepare(ObserverSet.this, method, args)) {
                    dispatchMethod.invoke(ObserverSet.this, method, args, dispatcher);
                }
                return ObjectUtils.getDefaultValue(method.getReturnType());
            }
        });
    }
    
    /**
     * returns the proxy.  Any methods invoked on the proxy will be invoked
     * with the same arguments on all of the observers in this ObserverSet.
     */
    public T proxy() {
        return mProxy;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
