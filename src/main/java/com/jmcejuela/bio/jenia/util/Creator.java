package com.jmcejuela.bio.jenia.util;


/**
 * Object T-type creator.
 *
 * Simulate c++ classes' implicit default constructor initialization for example in vectors or maps.
 */
public interface Creator<T> {

  public T neu();

}
