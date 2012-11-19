package com.jmcejuela.bio.jenia.common;

import com.jmcejuela.bio.jenia.MorphDic;

/**
 * From common.h
 */
public class Token {
  public final String text;
  public String baseForm;
  public String pos;
  public String chunk;
  public String ne;

  public Token(String text) {
    this.text = text;
    // Must set to empty String to simulate c++ string default constructor behavior
    this.baseForm = "";
    this.pos = "";
    this.chunk = "";
    this.ne = "";
  }

  public Token copy() {
    Token ret = new Token(this.text);
    ret.baseForm = this.baseForm;
    ret.pos = this.pos;
    ret.chunk = this.chunk;
    ret.ne = this.ne;
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();

    // s = ParenConverter.Pos2Ptb(s);
    // p = ParenConverter.Pos2Ptb(p);
    /*
     * if (i == 0) tmp += s + "/" + p; else tmp += " " + s + "/" + p;
     */

    s.append(text);
    s.append("\t");

    s.append(baseForm);
    s.append("\t");

    s.append(pos);
    s.append("\t");

    s.append(chunk);
    s.append("\t");

    s.append(ne);

    return s.toString();
  }
}
