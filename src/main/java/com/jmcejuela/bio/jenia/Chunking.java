package com.jmcejuela.bio.jenia;

import static com.jmcejuela.bio.jenia.util.Util.last;
import static com.jmcejuela.bio.jenia.util.Util.newArrayList;
import static java.lang.Math.log;
import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.jmcejuela.bio.jenia.common.Sentence;
import com.jmcejuela.bio.jenia.common.Token;
import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.maxent.ME_Sample;
import com.jmcejuela.bio.jenia.util.Constructor;
import com.jmcejuela.bio.jenia.util.Tuple2;

/**
 * From chunking.cpp
 *
 * TODO This is essentially all a copy of {@link Bidir} except for the implicitly declared chunking models used.
 *
 * Keep it DRY.
 *
 */
public class Chunking {

  static final int TAG_WINDOW_SIZE = 1;
  static final int BEAM_NUM = 1;
  // static final int BEAM_NUM = 10;
  static final double BEAM_WINDOW = 0.01;
  static final boolean ONLY_VERTICAL_FEATURES = false;

  // static final boolean DISTANT_FEATURES = false;

  enum DecodingStrategy {
    LEFT_TO_RIGHT, RIGHT_TO_LEFT, EASIEST_FIRST
  };

  static final DecodingStrategy decoding_strategy = DecodingStrategy.EASIEST_FIRST;

  // final DecodingStrategy decoding_strategy = LEFT_TO_RIGHT;
  // final DecodingStrategy decoding_strategy = RIGHT_TO_LEFT;

  static ME_Sample mesample(
      final ArrayList<Token> vt, int pos,
      final String tag_left2, final String tag_left1,
      final String tag_right1, final String tag_right2)
  {
    ME_Sample sample = new ME_Sample();

    String[] w = new String[5];
    String[] p = new String[5];

    w[0] = "BOS2";
    if (pos > 1) w[0] = vt.get(pos - 2).text;
    w[1] = "BOS";
    if (pos > 0) w[1] = vt.get(pos - 1).text;
    w[2] = vt.get(pos).text;
    w[3] = "EOS";
    if (pos < vt.size() - 1) w[3] = vt.get(pos + 1).text;
    w[4] = "EOS2";
    if (pos < vt.size() - 2) w[4] = vt.get(pos + 2).text;

    p[0] = "BOS2";
    if (pos > 1) p[0] = vt.get(pos - 2).pos;
    p[1] = "BOS";
    if (pos > 0) p[1] = vt.get(pos - 1).pos;
    p[2] = vt.get(pos).pos;
    p[3] = "EOS";
    if (pos < vt.size() - 1) p[3] = vt.get(pos + 1).pos;
    p[4] = "EOS2";
    if (pos < vt.size() - 2) p[4] = vt.get(pos + 2).pos;

    // first-order
    for (int i = 0; i < 5; i++) {
      int iminus2 = i - 2;
      //sample.features.add(String.format("W%d_%s", i - 2, w[i]));
      sample.features.add("W" + iminus2 + "_" + w[i]);
      //sample.features.add(String.format("P%d_%s", i - 2, p[i]));
      sample.features.add("P" + iminus2 + "_" + p[i]);
    }
    // bigram
    for (int i = 0; i < 4; i++) {
      int iminus2 = i - 2;
      int j = i + 1;
      int jminus2 = j - 2;
      //sample.features.add(String.format("P%dP%d_%s_%s", i - 2, j - 2, p[i], p[j]));
      sample.features.add("P" + iminus2 + "P" + jminus2 + "_" + p[i] + "_" + p[j]);
      //sample.features.add(String.format("W%dW%d_%s_%s", i - 2, j - 2, w[i], w[j]));
      sample.features.add("W" + iminus2 + "W" + jminus2 + "_" + w[i] + "_" + w[j]);
    }
    // pos trigram
    for (int i = 0; i < 3; i++) {
      int j = i + 1;
      int k = i + 2;
      //sample.features.add(String.format("P%dP%dP%d_%s_%s_%s", i - 2, j - 2, k - 2, p[i], p[j], p[k]));
      sample.features.add("P" + (i - 2) + "P" + (j - 2) + "P" + (k - 2) + "_" + p[i] + "_" + p[j] + "_" + p[k]);
    }

    String[] t = new String[] {
        tag_left2,
        tag_left1,
        "",
        tag_right1,
        tag_right2
    };

    // first-order
    for (int i = 0; i < 5; i++) {
      if (t[i].isEmpty()) continue;
      //sample.features.add(String.format("T%d_%s", i - 2, t[i]));
      sample.features.add("T" + (i - 2) + "_" + t[i]);
    }

    // second-order
    for (int i = 0; i < 4; i++) {
      int j = i + 1;
      if (t[i].isEmpty()) continue;
      if (t[j].isEmpty()) continue;
      //sample.features.add(String.format("T%dT%d_%s_%s", i - 2, j - 2, t[i], t[j]));
      sample.features.add("T" + (i - 2) + "T" + (j - 2) + "_" + t[i] + "_" + t[j]);
    }

    if (!t[1].isEmpty() && !t[3].isEmpty()) {
      //sample.features.add(String.format("T%dT%d_%s_%s", 1 - 2, 3 - 2, t[1], t[3]));
      sample.features.add("T" + (1 - 2) + "T" + (3 - 2) + "_" + t[1] + "_" + t[3]);
    }
    if (!t[0].isEmpty() && !t[1].isEmpty() && !t[3].isEmpty()) {
      //sample.features.add(String.format("T%dT%dT%d_%s_%s_%s", 0 - 2, 1 - 2, 3 - 2, t[0], t[1], t[3]));
      sample.features.add("T" + (0 - 2) + "T" + (1 - 2) + "T" + (3 - 2) + "_" + t[0] + "_" + t[1] + "_" + t[3]);
    }
    if (!t[1].isEmpty() && !t[3].isEmpty() && !t[4].isEmpty()) {
      //sample.features.add(String.format("T%dT%dT%d_%s_%s_%s", 1 - 2, 3 - 2, 4 - 2, t[1], t[3], t[4]));
      sample.features.add("T" + (1 - 2) + "T" + (3 - 2) + "T" + (4 - 2) + "_" + t[1] + "_" + t[3] + "_" + t[4]);
    }

    return sample;
  }

  static double entropy(final ArrayList<Double> v) {
    double sum = 0, maxp = 0;
    for (int i = 0; i < v.size(); i++) {
      if (v.get(i) == 0) continue;
      sum += v.get(i) * log(v.get(i));
      maxp = max(maxp, v.get(i));
    }
    return -sum;
  }

  static class Hypothesis {
    Sentence sentence;
    ArrayList<Double> entropies;
    ArrayList<Integer> order;
    // ArrayList<Integer> model;
    ArrayList<ArrayList<Tuple2<String, Double>>> vvp;
    double prob;

    Hypothesis(final Sentence sentence, final ArrayList<ME_Model> vme) {
      prob = 1.0;
      this.sentence = sentence.copy();
      int n = this.sentence.size();
      entropies = newArrayList(n, 0.0);
      vvp = newArrayList(n, new Constructor<ArrayList<Tuple2<String, Double>>>() {
            @Override
            public ArrayList<Tuple2<String, Double>> neu() {
              return new ArrayList<Tuple2<String, Double>>();
            }
          });
      order = newArrayList(n, 0);
      // model.resize(n);
      for (int i = 0; i < n; i++) {
        this.sentence.get(i).chunk = "";
        Update(i, vme);
      }
    }

    private Hypothesis() {};

    Hypothesis copy() {
      Hypothesis ret = new Hypothesis();
      ret.sentence = this.sentence.copy();
      /* The following can be done because Double, Integer, and Tuple2<String, Double> are immutable objects */
      ret.entropies = new ArrayList<Double>(this.entropies);
      ret.order = new ArrayList<Integer>(this.order);
      ret.vvp = newArrayList(this.vvp.size());
      for (ArrayList<Tuple2<String, Double>> a : this.vvp) {
        ArrayList<Tuple2<String, Double>> reta = new ArrayList<Tuple2<String, Double>>(a);
        ret.vvp.add(reta);
      }
      ret.prob = this.prob;
      return ret;
    }

    final boolean operator_less(final Hypothesis h) {
      return prob < h.prob;
    }

    static final Comparator<Hypothesis> Order = new Comparator<Hypothesis>() {
      @Override
      public int compare(Hypothesis o1, Hypothesis o2) {
        if (o1.prob < o2.prob)
          return -1;
        else if (o1.prob > o2.prob)
          return +1;
        else
          return 0;
      }
    };

    void Print() {
      for (int k = 0; k < this.sentence.size(); k++) {
        // cout << vt.get(k).str << "/";
        // if (vt.get(k).cprd.equals("")) cout << "?";
        // cout << vt.get(k).cprd;
        // cout << " ";
      }
      // cout << endl;
    }

    void Update(final int j, final ArrayList<ME_Model> vme) {
      String tag_left1 = "BOS", tag_left2 = "BOS2";
      if (j >= 1) tag_left1 = sentence.get(j - 1).chunk; // maybe bug??
      // if (j >= 1 && !vt.get(j-1].isEmpty()) pos_left1 = vt[j-1).cprd; // this should be correct
      if (j >= 2) tag_left2 = sentence.get(j - 2).chunk;
      String tag_right1 = "EOS", tag_right2 = "EOS2";
      if (j <= sentence.size() - 2) tag_right1 = sentence.get(j + 1).chunk;
      if (j <= sentence.size() - 3) tag_right2 = sentence.get(j + 2).chunk;
      ME_Sample mes = mesample(sentence, j, tag_left2, tag_left1, tag_right1, tag_right2);

      ArrayList<Double> membp;
      ME_Model mep = null;
      int bits = 0;
      // if (TAG_WINDOW_SIZE >= 2 && !tag_left2.equals("")) bits += 8;, jenia TAG_WINDOW_SIZE is 1
      if (!tag_left1.isEmpty()) bits += 4;
      if (!tag_right1.isEmpty()) bits += 2;
      // if (TAG_WINDOW_SIZE >= 2 && !tag_right2.equals("")) bits += 1;, jenia TAG_WINDOW_SIZE is 1
      assert (bits >= 0 && bits < 16);
      mep = vme.get(bits);
      membp = mep.classify(mes);
      assert (!mes.label.equals(""));
      // cout << "(" << j << ", " << bits << ") ";

      double maxp = membp.get(mep.get_class_id(mes.label));
      // ArrayList<Double> tmpv(membp);
      // sort(tmpv.begin(), tmpv.end());
      // double second = tmpv[1];

      switch (decoding_strategy) {
      case EASIEST_FIRST:
        entropies.set(j, -maxp);
        // vent[j] = maxp; // easiest last
        // vent[j] = second / maxp;
        // vent[j] = entropy(membp);
        break;
      case LEFT_TO_RIGHT:
        entropies.set(j, (double) j);
        break;
      case RIGHT_TO_LEFT:
        entropies.set(j, (double) -j);
        break;
      }

      vvp.get(j).clear();
      // vp[j] = mes.label;
      for (int i = 0; i < mep.num_classes(); i++) {
        double p = membp.get(i);
        if (p > maxp * BEAM_WINDOW)
          vvp.get(j).add(Tuple2.$(mep.get_class_label(i), p));
      }
    }

    final boolean IsErroneous() {
      for (int i = 0; i < sentence.size() - 1; i++) {
        final String a = sentence.get(i).chunk;
        final String b = sentence.get(i + 1).chunk;
        if (a.equals("") || b.equals("")) continue;
        // if (a[0] == 'B' && b[0] == 'B') {
        // if (a.substring(2) == b.substring(2)) return true;
        // }
        if (b.charAt(0) == 'I') {
          if (a.charAt(0) == 'O') return true;
          if (!a.substring(2).equals(b.substring(2))) return true;
        }
      }
      return false;
    }
  };

  static void generate_hypotheses(final int order, final Hypothesis h,
      final ArrayList<ME_Model> vme,
      List<Hypothesis> vh)
  {
    int n = h.sentence.size();
    int pred_position = -1;
    double min_ent = 999999;
    String pred = "";
    double pred_prob = 0;
    for (int j = 0; j < n; j++) {
      if (!h.sentence.get(j).chunk.equals("")) continue;
      double ent = h.entropies.get(j);
      if (ent < min_ent) {
        // pred = h.vvp[j].begin()->first;
        // pred_prob = h.vvp[j].begin()->second;
        min_ent = ent;
        pred_position = j;
      }
    }
    assert (pred_position >= 0 && pred_position < n);

    for (Tuple2<String, Double> k : h.vvp.get(pred_position)) {
      Hypothesis newh = h.copy();

      newh.sentence.get(pred_position).chunk = k._1;
      newh.order.set(pred_position, order + 1);
      newh.prob = h.prob * k._2;

      // if (newh.IsErroneous()) {
      // cout << "*errorneous" << endl;
      // newh.Print();
      // continue;
      // }

      // update the neighboring predictions
      for (int j = pred_position - TAG_WINDOW_SIZE; j <= pred_position + TAG_WINDOW_SIZE; j++) {
        if (j < 0 || j > n - 1) continue;
        if (newh.sentence.get(j).chunk.equals("")) newh.Update(j, vme);
      }
      vh.add(newh);
    }

  }

  static void convert_startend_to_iob2_sub(ArrayList<String> s) {
    for (int i = 0; i < s.size(); i++) {
      String tag = s.get(i);
      String newtag = tag;
      if (tag.charAt(0) == 'S') {
        newtag = "B" + tag.substring(1);
      }
      if (tag.charAt(0) == 'E') {
        newtag = "I" + tag.substring(1);
      }
      s.set(i, newtag);
    }
  }

  static void bidir_chunking_decode_beam(Sentence sentence, final ArrayList<ME_Model> chunkingModels) {
    int n = sentence.size();
    if (n == 0) return;

    ArrayList<Hypothesis> hypotheses = newArrayList();
    Hypothesis hyp = new Hypothesis(sentence, chunkingModels);
    hypotheses.add(hyp);

    for (int i = 0; i < n; i++) {
      ArrayList<Hypothesis> newHypotheses = newArrayList();
      for (Hypothesis j : hypotheses) {
        generate_hypotheses(i, j, chunkingModels, newHypotheses);
      }
      Collections.sort(newHypotheses, Hypothesis.Order);
      while (newHypotheses.size() > BEAM_NUM) {
        newHypotheses.remove(0);
      }
      hypotheses = newHypotheses;
    }

    if (!hypotheses.isEmpty()) {
      hyp = last(hypotheses);
    } else {
      // cerr << "warning: no hypothesis found" << endl;
      hyp = new Hypothesis(sentence, chunkingModels);
    }

    ArrayList<String> tags = newArrayList();
    for (int k = 0; k < n; k++) {
      // cout << h.vt.get(k].str << "/" << h.vt[k).cprd << "/" << h.order[k] << " ";
      tags.add(hyp.sentence.get(k).chunk);
    }

    convert_startend_to_iob2_sub(tags);
    for (int k = 0; k < n; k++) {
      sentence.get(k).chunk = tags.get(k);
    }

    // cout << endl;
  }

  /*
   * void bidir_chunking(ArrayList<Sentence> vs, final ArrayList<ME_Model> vme) { cerr << "now tagging";
   *
   * int n = 0; int ntokens = 0; for (ArrayList<Sentence>::iterator i = vs.begin(); i != vs.end(); i++) { Sentence s =
   * *i; ntokens += s.size(); bidir_decode_beam(s, vme); // bidir_decode_search(s, vme[0], vme[4], vme[2], vme[6]); //
   * decode_no_context(s, vme[0]); // decode_l1(s, vme[4]);
   *
   * cout << n << endl; if (n++ % 10 == 0) cerr << "."; } cerr << endl;
   *
   * // cerr << ntokens / (msec/1000.0) << " tokens / sec" << endl; }
   */

}
