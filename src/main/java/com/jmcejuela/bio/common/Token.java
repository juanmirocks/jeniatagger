package com.jmcejuela.bio.common;

/**
 * From common.h
 */
public class Token {
  public String str;
  public String pos;
  public String prd;
  public String cprd; // for chunking
  public String tag; // for chunking
  public String ne;

  public Token(String s, String p) {
    str = s;
    pos = p;
  }
}
