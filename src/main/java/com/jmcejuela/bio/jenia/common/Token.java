package com.jmcejuela.bio.jenia.common;

import com.jmcejuela.bio.jenia.MorphDic;

/**
 * From common.h
 */
public class Token {
  public final String text;
  public String pos;
  public String prd;
  public String cprd; // for chunking
  public String ne;

  public Token(String s, String p) {
    text = s;
    pos = p;
    // Must set to empty String to simulate c++ string default constructor behavior
    prd = "";
    cprd = "";
    ne = "";
  }

  public Token copy() {
    Token ret = new Token(this.text, this.pos);
    ret.prd = this.prd;
    ret.cprd = this.cprd;
    ret.ne = this.ne;
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    String token = text;
    String postag = prd;

    // s = ParenConverter.Pos2Ptb(s);
    // p = ParenConverter.Pos2Ptb(p);
    /*
     * if (i == 0) tmp += s + "/" + p; else tmp += " " + s + "/" + p;
     */

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
