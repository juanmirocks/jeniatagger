package com.jmcejuela.bio.jenia.common;

import java.util.ArrayList;

import com.jmcejuela.bio.jenia.Main;

/**
 * From common.h. In the original it's merely a wrapper (typedef std::vector<Token> Sentence)
 *
 * This class is not anymore generic, as it is known at compile & run time that is a sequence of
 * tokens. Consequently it can know how to clone/copy itself. This is necessary to simulate c++
 * behavior as the assignment of objects (in particular for vector<Token>) creates deep copies of
 * them. Use the method {@link #copy()} for this purpose.
 */
public class Sentence extends ArrayList<Token> {
  private static final long serialVersionUID = -8389935854206284421L;

  public Sentence() {
    super();
  }

  public Sentence(int size) {
    super(size);
  }

  public Sentence copy() {
    Sentence ret = new Sentence(this.size());
    for (Token t : this) {
      ret.add(t.copy());
    }
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for (Token t : this) {
      s.append(t);
      s.append(Main.ENDL);
    }
    return s.toString();
  }
}
