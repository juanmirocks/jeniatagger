package com.jmcejuela.bio.common;

/**
 * From common.h
 */
public class Token {
  String str;
  String pos;
  String prd;
  String cprd; // for chunking
  String tag; // for chunking
  String ne;

  Token(String s, String p) {
    str = s;
    pos = p;
  }
}
