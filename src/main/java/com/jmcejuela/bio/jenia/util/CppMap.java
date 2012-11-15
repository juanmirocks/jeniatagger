package com.jmcejuela.bio.jenia.util;

import java.util.HashMap;

/**
 * std::map::operator[] (http://www.cplusplus.com/reference/stl/map/operator[]/)
 *
 * if key is not in the map, the key is inserted with a new value initialized with the default constructor.
 *
 * Due to type-erasure in Java a object generator must be given explicitly to simulate the same behavior.
 */
public class CppMap<K, V> extends HashMap<K, V> {
  Constructor<V> c;

  private static final long serialVersionUID = -197420711650949268L;

  public CppMap(Constructor<V> c) {
    this.c = c;
  }

  // can't verify the type due to type erasure -> (K) cast
  @SuppressWarnings("unchecked")
  @Override
  public V get(Object key) {
    V ret = super.get(key);
    if (null == ret) {
      ret = c.neu();
      super.put((K) key, ret);
    }
    return ret;
  }
}
