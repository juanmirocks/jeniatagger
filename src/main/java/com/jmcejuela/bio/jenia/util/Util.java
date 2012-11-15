package com.jmcejuela.bio.jenia.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class Util {

  public static InputStream resourceStream(String resource) {
    return Util.class.getResourceAsStream(
        (resource.startsWith("/") ? resource : "/" + resource));
  }

  public static <T> ArrayList<T> newArrayList(int size) {
    return new ArrayList<T>(size);
  }

  public static <T> ArrayList<T> newArrayList() {
    return new ArrayList<T>();
  }

  public static <T> ArrayList<T> newArrayList(int size, Creator<T> initValueCreator) {
    ArrayList<T> ret = new ArrayList<T>(size);
    for (int i = 0; i < size; i++) {
      ret.add(initValueCreator.neu());
    }
    return ret;
  }

  public static ArrayList<Double> newArrayList(int size, double initValue) {
    ArrayList<Double> ret = new ArrayList<Double>(size);
    for (int i = 0; i < size; i++) {
      ret.add(initValue);
    }
    return ret;
  }

  public static ArrayList<Integer> newArrayList(int size, int initValue) {
    ArrayList<Integer> ret = new ArrayList<Integer>(size);
    for (int i = 0; i < size; i++) {
      ret.add(initValue);
    }
    return ret;
  }

  public static double max(double[] array) {
    if (array.length == 0)
      throw new NoSuchElementException("max of nothing");
    double ret = array[0];
    for (int i = 1; i < array.length; i++) {
      if (array[i] > ret)
        ret = array[i];
    }
    return ret;
  }

  public static <T> void increase(Map<T, Integer> map, T key) {
    Integer val;
    if ((val = map.get(key)) == null) {
      map.put(key, 1);
    } else {
      map.put(key, val + 1);
    }
  }

  public static <T> T removeFirst(ArrayList<T> vector) {
    return vector.remove(0);
  }

  public static <T> T pop(ArrayList<T> vector) {
    return vector.remove(vector.size() - 1);
  }

  public static <T> T last(List<T> vector) {
    return vector.get(vector.size() - 1);
  }

  public static void plusEq(ArrayList<Double> vector, int index, double plus) {
    vector.set(index, vector.get(index) + plus);
  }

  public static void minusEq(ArrayList<Double> vector, int index, double minus) {
    vector.set(index, vector.get(index) - minus);
  }

  public static void timesEq(ArrayList<Double> vector, int index, double times) {
    vector.set(index, vector.get(index) * times);
  }

  public static void divEq(ArrayList<Double> vector, int index, double div) {
    vector.set(index, vector.get(index) / div);
  }

  public static <T> void resize(ArrayList<T> vector, int size, T val) {
    int diffsize = vector.size() - size;

    if (diffsize > 0) {
      for (int i = 0; i < diffsize; i++) {
        vector.add(val);
      }
    } else if (diffsize < 0) {
      for (int i = vector.size() - 1; diffsize < 0; i--) {
        vector.remove(i);
        diffsize++;
      }
    }
  }

  /**
   * From tokenize.cpp
   *
   * Original c++ signature: tokenize(const String & s1, list<String> & lt)
   *
   * Implement when tests are ready.
   *
   * @param s1
   * @return
   */
  public static final List<String> tokenize(final String s1) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * From tokenize.cpp
   *
   * Original c++ signature: replace(String & s, const String & s1, const String & s2, const char skip)
   *
   * Replace by {@link String#replace(char, char)} I believe the skip flag is not necessary
   *
   * @param s
   * @param s1
   * @param s2
   * @param skip
   */
  // public static final void replace(String s, final String s1, final String s2, final char skip)

}
