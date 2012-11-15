package com.jmcejuela.bio.jenia.util;

/**
 * C++ std::pair equivalent as a Scala-like tuple.
 * 
 * @param <T1>
 * @param <T2>
 */
public class Tuple2<T1, T2> {
  public final T1 _1;
  public final T2 _2;

  private Tuple2(T1 _1, T2 _2) {
    this._1 = _1;
    this._2 = _2;
  }

  public static <T1, T2> Tuple2<T1, T2> $(T1 _1, T2 _2) {
    return new Tuple2<T1, T2>(_1, _2);
  }

}