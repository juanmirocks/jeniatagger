package com.jmcejuela.bio.jenia.common;

import com.jmcejuela.bio.jenia.MorphDic;

/**
 * From common.h
 */
public class Token {
  public final String text;
  public String pos;
  public String chunk;
  public String ne;

  public Token(String text) {
    this.text = text;
    this.pos = "";
    // Must set to empty String to simulate c++ string default constructor behavior
    this.chunk = "";
    this.ne = "";
  }

  public Token copy() {
    Token ret = new Token(this.text);
    ret.pos = this.pos;
    ret.chunk = this.chunk;
    ret.ne = this.ne;
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    // String postag = prd;

    // s = ParenConverter.Pos2Ptb(s);
    // p = ParenConverter.Pos2Ptb(p);
    /*
     * if (i == 0) tmp += s + "/" + p; else tmp += " " + s + "/" + p;
     */

    s.append(text);
    s.append("\t");

    s.append(MorphDic.base_form(text, pos));
    s.append("\t");

    s.append(pos);
    s.append("\t");

    s.append(chunk);
    s.append("\t");

    s.append(ne);

    return s.toString();
  }
}
