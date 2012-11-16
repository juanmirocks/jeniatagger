package com.jmcejuela.bio.jenia.common;

/**
 * From common.h
 */
public class Token {
  public final String str;
  public String pos;
  public String prd;
  public String cprd; // for chunking
  public final String tag; // for chunking
  public String ne;

  public Token(String s, String p) {
    str = s;
    pos = p;
    // Must set to empty String to simulate c++ string default constructor behavior
    prd = "";
    cprd = "";
    tag = "";
    ne = "";
  }
}
