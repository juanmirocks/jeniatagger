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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.maxent.ME_Sample;

/**
 * From namedentity.cpp
 */
public class NamedEntity {
  private static ME_Model ne_model;
  static Map<String, WordInfo> word_info;
  private static Map<String, WordInfo> pos_info;

  static final double BIAS_FOR_RECALL = 0.6;

  private final int max_term_length = 0;

  static void load_ne_models() {
    String model_file = "./models_named_entity/model001";
    String wordinfo_file = "./models_named_entity/word_info";

    // cerr << "loading named_entity_models.";
    ne_model.load_from_file(model_file);
    // cerr << ".";
    word_info = load_word_info(wordinfo_file);
    // cerr << "done." << endl;
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
      total = t;
      edge_ne = e; // TODO this is the order written in the original
    }

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
    /*
     * if (tmp == "is") tmp = "be"; if (tmp == "was") tmp = "be"; if (tmp == "are") tmp = "be"; if (tmp == "were") tmp =
     * "be"; if (tmp == "an") tmp = "a"; if (tmp == "the") tmp = "a";
     */
    if (tmp.charAt(tmp.length() - 1) == 's') return tmp.substring(0, tmp.length() - 1);
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

  static ME_Sample mesample(final String label, final ArrayList<Token> vt, int begin, int end) {
    ME_Sample mes = new ME_Sample();

    mes.label = label;

    final int BUFLEN = 1000;

    String s;

    // contextual feature
    String s_1, s_2, s1, s2;
    // if (begin >= 1) s_1 = vt.get(begin-1).str;
    if (begin >= 1)
      s_1 = normalize(vt.get(begin - 1).str);
    else
      s_1 = "BOS";
    mes.features.add(String.format("C-1_%s", s_1));

    // if (end < vt.length()) s1 = vt.get(end).str;
    if (end < vt.size())
      s1 = normalize(vt.get(end).str);
    else
      s1 = "EOS";
    mes.features.add(String.format("C+1_%s", s1));

    // if (begin >= 2) s_2 = vt.get(begin-2).str;
    if (begin >= 2)
      s_2 = normalize(vt.get(begin - 2).str);
    else
      s_2 = "BOS";

    // if (end < vt.length()-1) s2 = vt.get(end+1).str;
    if (end < vt.size() - 1)
      s2 = normalize(vt.get(end + 1).str);
    else
      s2 = "EOS";

    mes.features.add("C-2-1_" + s_2 + "_" + s_1);
    mes.features.add("C-1+1_" + s_1 + "_" + s1);
    mes.features.add("C+1+2_" + s1 + "_" + s2);

    // term feature
    char firstletter = vt.get(begin).str.charAt(0);
    char lastletter = vt.get(end - 1).str.charAt(vt.get(end - 1).str.length() - 1);
    // if (begin != 0 && isupper(firstletter))
    // if (isupper(firstletter) && isupper(lastletter))
    // mes.features.add("IS_UPPER");

    // if (end - begin == 1) {
    // mes.features.add("EXACT_" + vt.get(begin).str);
    // }

    String tb = normalize(vt.get(begin).str);
    mes.features.add(String.format("TB_%s", tb));

    for (int i = begin + 1; i < end - 1; i++) {
      // for (int i = begin; i < end; i++) {
      s = normalize(vt.get(i).str);
      mes.features.add(String.format("TM_%s", s));
    }

    String te = normalize(vt.get(end - 1).str);
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
    boolean contain_comma = false;
    for (int i = begin; i < end; i++) {
      if (s.length() + vt.get(i).str.length() > BUFLEN - 100) break;
      s += normalize(vt.get(i).str);
      whole += vt.get(i).str;
    }

    // if (label > 0) mes.features.add(buf);
    mes.features.add(String.format("WHOLE_%s", s));
    mes.features.add("WS1_" + wordshape(whole, true));
    mes.features.add("WS2_" + wordshape(whole, false));

    // mes.features.add("WHOLE_C+1_" + whole + "-" + s1);

    // preffix and suffix
    for (int j = 1; j <= 10; j++) {
      if (s.length() >= j) {
        mes.add_feature(String.format("SUF%d_%s", j, s.substring(s.length() - j)));
      }
      if (s.length() >= j) {
        mes.add_feature(String.format("PRE%d_%s", j, s.substring(0, j)));
      }
    }

    // if (contain_comma)
    // mes.features.add("CONTAIN_COMMA");

    // cout << fb.Id(buf) << " " << buf << endl;

    // POS feature
    String p_2 = "BOS", p_1 = "BOS";
    String pb, pe;
    String p1 = "EOS", p2 = "EOS";
    if (begin >= 2) p_2 = vt.get(begin - 2).pos;
    if (begin >= 1) p_1 = vt.get(begin - 1).pos;
    pb = vt.get(begin).pos;
    pe = vt.get(end - 1).pos;
    if (end < vt.size()) p1 = vt.get(end).pos;
    if (end < vt.size() - 1) p2 = vt.get(end + 1).pos;

    mes.features.add("PoS-1_" + p_1);
    mes.features.add("PoS-B_" + pb);
    mes.features.add("PoS-E_" + pe);
    mes.features.add("PoS+1_" + p1);
    // String posseq;
    // for (int i = begin; i < end; i++) {
    // posseq += vt.get(i).pos + "_";
    // }
    // mes.features.add("PosSeq_" + posseq);

    return mes;
  }

  static boolean is_candidate(final Sentence s, final int begin, final int end) {
    if (word_info.get(s.get(begin).str).edge_prob() < 0.01) return false;
    if (word_info.get(s.get(end - 1).str).edge_prob() < 0.01) return false;
    // if (end - begin > 10) return false;
    if (end - begin > 30) return false;

    int penalty = 0;
    int kakko = 0;
    for (int x = begin; x < end; x++) {
      if (s.get(x).str == "(") kakko++;
      if (s.get(x).str == ")") {
        if (kakko % 2 == 0) return false;
        kakko--;
      }
      double out_prob = word_info.get(s.get(x).str).out_prob();
      // if (out_prob >= 0.99) penalty++;
      // if (out_prob >= 0.90) penalty++;
      // if (out_prob >= 0.98) penalty++;
      // if (out_prob >= 0.94) penalty++;
      if (out_prob >= 0.99) penalty++;
      if (out_prob >= 0.98) penalty++;
      if (out_prob >= 0.97) penalty++;
      if (s.get(x).pos == "VBZ") return false;
      if (s.get(x).pos == "VB") return false;
      if (s.get(x).pos == "VBP") return false;
      if (s.get(x).pos == "MD") return false;
      if (s.get(x).pos == "RB") penalty += 1;

      if (penalty >= 5) return false;
    }

    if (s.get(end - 1).pos == "JJ") penalty += 2;
    if (s.get(end - 1).pos == "IN") penalty += 3;

    if (penalty >= 5) return false;

    if (kakko % 2 != 0) return false;

    // for (int x = begin; x < end; x++) {
    // cout << s.get(x].str << "/" << s[x).pos << " ";
    // }
    // cout << endl;

    return true;
  }

  private static Map<String, WordInfo> load_word_info(final String filename) {
    Map<String, WordInfo> ret = new HashMap<String, NamedEntity.WordInfo>();
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
      return prob > x.prob;
    }

    static final Comparator<Annotation> Order = new Comparator<Annotation>() {
      @Override
      public int compare(Annotation o1, Annotation o2) {
        if (o1.prob < o2.prob)
          return -1;
        else if (o1.prob > o2.prob)
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
  }

  static void find_NEs(final ME_Model me, Sentence s) {
    final int other_class = me.get_class_id("O");

    ArrayList<Double> label_p = newArrayList(s.size(), 0.0);
    for (int j = 0; j < s.size(); j++) {
      s.get(j).ne = "O";
      // label_p.set(j, 0);
    }

    List<Annotation> la = newArrayList(); // TODO check size
    for (int j = 0; j < s.size(); j++) {
      // for (int k = s.length(); k > j; k--) {
      for (int k = j + 1; k <= s.size(); k++) {
        if (!is_candidate(s, j, k)) {
          // if (isterm(s_org, j, k)) num_candidate_false_negatives++;
          continue;
        }
        ME_Sample nbs = mesample("?", s, j, k);
        ArrayList<Double> membp = newArrayList(me.num_classes());
        // int label = nb.classify(nbs, NULL, &membp);
        // me.classify(nbs, &membp);
        membp = me.classify(nbs);
        int label = 0;
        minusEq(membp, other_class, BIAS_FOR_RECALL);
        for (int l = 0; l < me.num_classes(); l++) {
          if (membp.get(l) > membp.get(label)) label = l;
        }
        double prob = membp.get(label);

        // print_features(fb, nbs);
        // cout << endl << "------- ";
        // for (int l = 0; l < me.num_classes(); l++) cout << membp[l] << " ";
        // cout << endl;

        if (label != other_class) {
          la.add(new Annotation(label, j, k, prob));
        }
      }
    }
    Collections.sort(la, Annotation.Order);
    // for (int j = 0; j < s.length(); j++) cout << j << ":" << s.get(j).str << " ";
    // cout << endl;
    for (Annotation j : la) {
      // cout << j.label << " begin = " << j.begin << " end = " << j.end << " prob = " << j.prob << endl;
      boolean override = true;
      for (int l = j.begin; l < j.end; l++) {
        if (label_p.get(l) >= j.prob) {
          override = false;
          break;
        }
        if (s.get(l).ne != "O") {
          // erase the old label
          int lbegin = l;
          while (s.get(lbegin).ne.charAt(0) != 'B')
            lbegin--;
          int lend = l;
          while (s.get(lend).ne.charAt(0) != 'O' && lend < s.size())
            lend++;
          for (int t = lbegin; t < lend; t++) {
            s.get(t).ne = "O";
            label_p.set(t, 0.0);
          }
        }
      }
      if (!override) continue;
      for (int l = j.begin; l < j.end; l++) {
        label_p.set(l, j.prob);
        if (l == j.begin)
          s.get(l).ne = "B-" + me.get_class_label(j.label);
        else
          s.get(l).ne = "I-" + me.get_class_label(j.label);
      }
    }
  }

  static int netagging(Sentence vt) {
    find_NEs(ne_model, vt);
    return 0; // TODO no explicit return in original
  }

}
