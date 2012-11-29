package com.jmcejuela.bio.jenia;

import static com.jmcejuela.bio.jenia.util.Util.minusEq;
import static com.jmcejuela.bio.jenia.util.Util.newArrayList;
import static com.jmcejuela.bio.jenia.util.Util.resourceStream;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.maxent.ME_Sample;
import com.jmcejuela.bio.jenia.util.Constructor;
import com.jmcejuela.bio.jenia.util.CppMap;

/**
 * From namedentity.cpp
 */
public class NamedEntity {
  private static ME_Model ne_model;
  private static Map<String, WordInfo> word_info;

  // private static Map<String, WordInfo> pos_info;

  // private final int max_term_length = 0;
  static final double BIAS_FOR_RECALL = 0.6;

  private NamedEntity() {};

  static {
    init();
  }

  static void init() {
    ne_model = new ME_Model();
    ne_model.load_from_file("/models_named_entity/model001");

    word_info = load_word_info("/models_named_entity/word_info");
  }

  static class WordInfo {
    String str;
    int inside_ne;
    int edge_ne;
    int total;

    WordInfo() {
      str = "";
      inside_ne = 0;
      total = 0;
      edge_ne = 0;
    }

    WordInfo(final String s, int i, int e, int t) {
      str = s;
      inside_ne = i;
      edge_ne = e;
      total = t;
    }

    static Constructor<WordInfo> CONSTRUCTOR = new Constructor<WordInfo>() {
      @Override
      public WordInfo neu() {
        return new WordInfo();
      }
    };

    final double out_prob() {
      return ((total - inside_ne) + 1.0) / (total + 2.0);
    }

    final double in_prob() {
      return (inside_ne + 1.0) / (total + 2.0);
    }

    final double edge_prob() {
      return (edge_ne + 1.0) / (total + 2.0);
    }

    boolean operator_less(final WordInfo x) {
      // return this.out_prob() > x.out_prob();
      return this.edge_prob() > x.edge_prob();
    }
  }

  static String normalize(final String s) {
    String tmp = "";
    for (int i = 0; i < s.length(); i++) {
      char c = toLowerCase(s.charAt(i));
      if (isDigit(c)) c = '#';
      if (c == '-' || c == ' ') continue;
      tmp += c;
    }
    // jenia. Note, the original did normalize '-' to the empty string but in c++ ""[-1] doesn't
    // throw an exception
    // TODO this also makes "s" the empty string. I guess this was not intended
    if (!tmp.isEmpty() && tmp.charAt(tmp.length() - 1) == 's') return tmp.substring(0, tmp.length() - 1);
    return tmp;
  }

  static String wordshape(final String s, boolean fine) {
    String tmp = "";
    char pre_c = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (isDigit(c))
        c = '#';
      else if (isUpperCase(c))
        c = 'A';
      else if (isLowerCase(c))
        c = 'a';
      else if (c == ' ' || c == '-')
        c = '-';
      else
        continue;
      if (fine || c != pre_c)
        tmp += c;
      pre_c = c;
    }
    return tmp;
  }

  static ME_Sample mesample(final String label, final Sentence sentence, int begin, int end) {
    ME_Sample mes = new ME_Sample(label);

    final int BUFLEN = 1000;

    String s = "";

    // contextual feature
    String s_1, s_2, s1, s2;
    // if (begin >= 1) s_1 = vt.get(begin-1).str;
    if (begin >= 1)
      s_1 = normalize(sentence.get(begin - 1).text);
    else
      s_1 = "BOS";
    mes.features.add(String.format("C-1_%s", s_1));

    // if (end < vt.length()) s1 = vt.get(end).str;
    if (end < sentence.size())
      s1 = normalize(sentence.get(end).text);
    else
      s1 = "EOS";
    mes.features.add(String.format("C+1_%s", s1));

    // if (begin >= 2) s_2 = vt.get(begin-2).str;
    if (begin >= 2)
      s_2 = normalize(sentence.get(begin - 2).text);
    else
      s_2 = "BOS";

    // if (end < vt.length()-1) s2 = vt.get(end+1).str;
    if (end < sentence.size() - 1)
      s2 = normalize(sentence.get(end + 1).text);
    else
      s2 = "EOS";

    mes.features.add("C-2-1_" + s_2 + "_" + s_1);
    mes.features.add("C-1+1_" + s_1 + "_" + s1);
    mes.features.add("C+1+2_" + s1 + "_" + s2);

    String tb = normalize(sentence.get(begin).text);
    mes.features.add(String.format("TB_%s", tb));

    for (int i = begin + 1; i < end - 1; i++) {
      // for (int i = begin; i < end; i++) {
      s = normalize(sentence.get(i).text);
      mes.features.add(String.format("TM_%s", s));
    }

    String te = normalize(sentence.get(end - 1).text);
    mes.features.add(String.format("TE_%s", te));

    // combination
    mes.features.add("C-1_TB_" + s_1 + "_" + tb);
    mes.features.add("C-1_TE_" + s_1 + "_" + te);
    mes.features.add("TB_TE_" + tb + "-" + te);
    mes.features.add("TB_C+1_" + tb + "_" + s1);
    mes.features.add("TE_C+1_" + te + "-" + s1);

    // mes.features.add("C-2-1_TE_" + s_2 + "_" + s_1 + "_" + te);
    // mes.features.add("TE_C+1+2_" + te + "_" + s1 + "_" + s2);

    s = "";
    String whole = "";
    // boolean contain_comma = false;
    for (int i = begin; i < end; i++) {
      if (s.length() + sentence.get(i).text.length() > BUFLEN - 100) break;
      s += normalize(sentence.get(i).text);
      whole += sentence.get(i).text;
    }

    // if (label > 0) mes.features.add(buf);
    mes.features.add(String.format("WHOLE_%s", s));
    mes.features.add("WS1_" + wordshape(whole, true));
    mes.features.add("WS2_" + wordshape(whole, false));

    // mes.features.add("WHOLE_C+1_" + whole + "-" + s1);

    // preffix and suffix
    int limit = Math.min(s.length(), 10);
    for (int j = 1; j <= limit; j++) {
      //mes.add_feature(String.format("SUF%d_%s", j, s.substring(s.length() - j)));
      mes.add_feature("SUF" + j + "_" + s.substring(s.length() - j));
      //mes.add_feature(String.format("PRE%d_%s", j, s.substring(0, j)));
      mes.add_feature("PRE" + j + "_" + s.substring(0, j));
    }

    // POS feature
    String p_2 = "BOS", p_1 = "BOS";
    String pb, pe;
    String p1 = "EOS", p2 = "EOS";
    if (begin >= 2) p_2 = sentence.get(begin - 2).pos;
    if (begin >= 1) p_1 = sentence.get(begin - 1).pos;
    pb = sentence.get(begin).pos;
    pe = sentence.get(end - 1).pos;
    if (end < sentence.size()) p1 = sentence.get(end).pos;
    if (end < sentence.size() - 1) p2 = sentence.get(end + 1).pos;

    mes.features.add("PoS-1_" + p_1);
    mes.features.add("PoS-B_" + pb);
    mes.features.add("PoS-E_" + pe);
    mes.features.add("PoS+1_" + p1);

    return mes;
  }

  static boolean is_candidate(final Sentence s, final int begin, final int end) {
    if (word_info.get(s.get(begin).text).edge_prob() < 0.01) return false;
    if (word_info.get(s.get(end - 1).text).edge_prob() < 0.01) return false;
    // if (end - begin > 10) return false;
    if (end - begin > 30) return false;

    int penalty = 0;
    int kakko = 0;
    for (int x = begin; x < end; x++) {
      if (s.get(x).text.equals("(")) kakko++;
      if (s.get(x).text.equals(")")) {
        if (kakko % 2 == 0) return false;
        kakko--;
      }
      double out_prob = word_info.get(s.get(x).text).out_prob();
      // if (out_prob >= 0.99) penalty++;
      // if (out_prob >= 0.90) penalty++;
      // if (out_prob >= 0.98) penalty++;
      // if (out_prob >= 0.94) penalty++;
      if (out_prob >= 0.99) penalty++;
      if (out_prob >= 0.98) penalty++;
      if (out_prob >= 0.97) penalty++;
      if (s.get(x).pos.equals("VBZ")) return false;
      if (s.get(x).pos.equals("VB")) return false;
      if (s.get(x).pos.equals("VBP")) return false;
      if (s.get(x).pos.equals("MD")) return false;
      if (s.get(x).pos.equals("RB")) penalty += 1;

      if (penalty >= 5) return false;
    }

    if (s.get(end - 1).pos.equals("JJ")) penalty += 2;
    if (s.get(end - 1).pos.equals("IN")) penalty += 3;

    if (penalty >= 5) return false;

    if (kakko % 2 != 0) return false;

    return true;
  }

  static Map<String, WordInfo> load_word_info(final String filename) {
    Map<String, WordInfo> ret = new CppMap<String, WordInfo>(WordInfo.CONSTRUCTOR);
    Scanner sc = new Scanner(resourceStream(filename));
    while (sc.hasNextLine()) {
      String s = sc.next();
      int i = sc.nextInt(), e = sc.nextInt(), t = sc.nextInt();

      ret.put(s, new WordInfo(s, i, e, t));

      sc.nextLine();
    }

    return ret;
  }

  static class Annotation {
    int label;
    int begin;
    int end;
    double prob;

    boolean operator_less(final Annotation x) {
      return prob > x.prob; //note, descending order
    }

    static final Comparator<Annotation> DescOrder = new Comparator<Annotation>() {
      @Override
      public int compare(Annotation o1, Annotation o2) {
        if (o1.prob > o2.prob)
          return -1;
        else if (o1.prob < o2.prob)
          return +1;
        else
          return 0;
      }
    };

    Annotation(final int l, final int b, final int e, final double p) {
      label = l;
      begin = b;
      end = e;
      prob = p;
    }

    @Override
    public String toString() {
      return "Annotation(" + label + ", " + begin + ", " + end + ", " + prob + ")";
    }
  }

  static void find_NEs(final ME_Model me, Sentence s) {
    final int other_class = me.get_class_id("O");

    ArrayList<Double> label_p = newArrayList(s.size(), 0.0);
    for (int j = 0; j < s.size(); j++) {
      s.get(j).ne = "O";
      // jenia, done in init already; label_p.set(j, 0);
    }

    List<Annotation> annotations = newArrayList();
    for (int j = 0; j < s.size(); j++) {
      for (int k = j + 1; k <= s.size(); k++) {
        if (!is_candidate(s, j, k)) {
          continue;
        }
        ME_Sample nbs = mesample("?", s, j, k);
        ArrayList<Double> membp = me.classify(nbs);
        int label = 0;
        minusEq(membp, other_class, BIAS_FOR_RECALL);
        for (int l = 0; l < me.num_classes(); l++) {
          if (membp.get(l) > membp.get(label)) label = l;
        }
        double prob = membp.get(label);
        if (label != other_class) {
          annotations.add(new Annotation(label, j, k, prob));
        }
      }
    }
    Collections.sort(annotations, Annotation.DescOrder);

    for (Annotation annotation : annotations) {
      boolean override = true;
      for (int l = annotation.begin; l < annotation.end; l++) {
        if (label_p.get(l) >= annotation.prob) {
          override = false;
          break;
        }
        if (!s.get(l).ne.equals("O")) {
          // erase the old label
          int lbegin = l;
          while (s.get(lbegin).ne.charAt(0) != 'B')
            lbegin--;
          int lend = l;
          while (lend < s.size() && s.get(lend).ne.charAt(0) != 'O')
            lend++;
          for (int t = lbegin; t < lend; t++) {
            s.get(t).ne = "O";
            label_p.set(t, 0.0);
          }
        }
      }
      if (!override) continue;
      for (int l = annotation.begin; l < annotation.end; l++) {
        label_p.set(l, annotation.prob);
        if (l == annotation.begin)
          s.get(l).ne = "B-" + me.get_class_label(annotation.label);
        else
          s.get(l).ne = "I-" + me.get_class_label(annotation.label);
      }
    }
  }

  static void netagging(Sentence vt) {
    find_NEs(ne_model, vt);
  }

}
