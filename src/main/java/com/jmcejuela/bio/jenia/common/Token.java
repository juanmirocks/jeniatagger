package com.jmcejuela.bio.jenia.common;

import com.jmcejuela.bio.jenia.MorphDic;

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

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    String token = str;
    String postag = prd;

    s.append(token);
    s.append("\t");

    s.append(MorphDic.base_form(token, postag));
    s.append("\t");

    s.append(postag);
    s.append("\t");

    s.append(cprd);
    s.append("\t");

    s.append(ne);

    return s.toString();
  }
}
