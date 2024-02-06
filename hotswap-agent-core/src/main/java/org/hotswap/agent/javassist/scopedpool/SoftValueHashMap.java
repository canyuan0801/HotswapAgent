

package org.hotswap.agent.javassist.scopedpool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class SoftValueHashMap<K,V> implements Map<K,V> {
    private static class SoftValueRef<K,V> extends SoftReference<V> {
        public K key;

        private SoftValueRef(K key, V val, ReferenceQueue<V> q) {
            super(val, q);
            this.key = key;
        }

        private static <K,V> SoftValueRef<K,V> create(
                            K key, V val, ReferenceQueue<V> q) {
            if (val == null)
                return null;
            else
                return new SoftValueRef<K,V>(key, val, q);
        }

    }


    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        processQueue();
        Set<Entry<K,V>> ret = new HashSet<Entry<K,V>>();
        for (Entry<K,SoftValueRef<K,V>> e:hash.entrySet()) 
                ret.add(new SimpleImmutableEntry<K,V> (
                        e.getKey(), e.getValue().get()));
        return ret;        
    }


    private Map<K,SoftValueRef<K,V>> hash;


    private ReferenceQueue<V> queue = new ReferenceQueue<V>();


    private void processQueue() {
        Object ref;
        if (!hash.isEmpty())
        while ((ref = queue.poll()) != null)
            if (ref instanceof SoftValueRef)  {
                @SuppressWarnings("rawtypes")
                SoftValueRef que =(SoftValueRef) ref;
                if (ref == hash.get(que.key))

                    hash.remove(que.key);
            }
    }




    public SoftValueHashMap(int initialCapacity, float loadFactor) {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>(initialCapacity, loadFactor);
    }


    public SoftValueHashMap(int initialCapacity) {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>(initialCapacity);
    }


    public SoftValueHashMap() {
        hash = new ConcurrentHashMap<K,SoftValueRef<K,V>>();
    }


    public SoftValueHashMap(Map<K,V> t) {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }




    @Override
    public int size() {
        processQueue();
        return hash.size();
    }


    @Override
    public boolean isEmpty() {
        processQueue();
        return hash.isEmpty();
    }


    @Override
    public boolean containsKey(Object key) {
        processQueue();
        return hash.containsKey(key);
    }




    @Override
    public V get(Object key) {
        processQueue();
        return valueOrNull(hash.get(key));
    }


    @Override
    public V put(K key, V value) {
        processQueue();
        return valueOrNull(hash.put(key, SoftValueRef.create(key, value, queue)));
    }


    @Override
    public V remove(Object key) {
        processQueue();
        return valueOrNull(hash.remove(key));
    }


    @Override
    public void clear() {
        processQueue();
        hash.clear();
    }


    @Override
    public boolean containsValue(Object arg0) {
        processQueue();
        if (null == arg0)
            return false;
        
        for (SoftValueRef<K,V> e:hash.values())
            if (null != e && arg0.equals(e.get()))
                return true;
        return false;
    }


    @Override
    public Set<K> keySet() {
        processQueue();
        return hash.keySet();
    }
    

    @Override
    public void putAll(Map<? extends K,? extends V> arg0) {
        processQueue();
        for (K key:arg0.keySet())
            put(key, arg0.get(key));
    }


    @Override
    public Collection<V> values() {
        processQueue();
        List<V> ret = new ArrayList<V>();
        for (SoftValueRef<K,V> e:hash.values())
            ret.add(e.get());
        return ret;
    }
    
    private V valueOrNull(SoftValueRef<K,V> rtn) { 
        if (null == rtn)
            return null;
        return rtn.get();
    }
}
