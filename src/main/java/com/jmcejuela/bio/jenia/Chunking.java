package com.jmcejuela.bio.jenia;

import static com.jmcejuela.bio.jenia.util.Util.last;
import static com.jmcejuela.bio.jenia.util.Util.newArrayList;
import static java.lang.Math.log;
import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.jmcejuela.bio.jenia.common.Token;
import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.maxent.ME_Sample;
import com.jmcejuela.bio.jenia.util.Constructor;
import com.jmcejuela.bio.jenia.util.Tuple2;

/**
 * From chunking.cpp
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

  static ME_Sample mesample(final ArrayList<Token> vt, int pos,
      final String tag_left2, final String tag_left1,
      final String tag_right1, final String tag_right2)
  {
    ME_Sample sample = new ME_Sample();

    sample.label = vt.get(pos).tag;

    String[] w = new String[5];
    String[] p = new String[5];
    String[] t = new String[5];

    w[0] = "BOS2";
    if (pos > 1) w[0] = vt.get(pos - 2).str;
    w[1] = "BOS";
    if (pos > 0) w[1] = vt.get(pos - 1).str;
    w[2] = vt.get(pos).str;
    w[3] = "EOS";
    if (pos < vt.size() - 1) w[3] = vt.get(pos + 1).str;
    w[4] = "EOS2";
    if (pos < vt.size() - 2) w[4] = vt.get(pos + 2).str;

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
      sample.features.add(String.format("W%d_%s", i - 2, w[i]));
      sample.features.add(String.format("P%d_%s", i - 2, p[i]));
    }
    // bigram
    for (int i = 0; i < 4; i++) {
      int j = i + 1;
      sample.features.add(String.format("P%dP%d_%s_%s", i - 2, j - 2, p[i], p[j]));
      sample.features.add(String.format("W%dW%d_%s_%s", i - 2, j - 2, w[i], w[j]));
    }
    // pos trigram
    for (int i = 0; i < 3; i++) {
      int j = i + 1;
      int k = i + 2;
      sample.features.add(String.format("P%dP%dP%d_%s_%s_%s", i - 2, j - 2, k - 2, p[i], p[j], p[k]));
    }

    t[0] = tag_left2;
    t[1] = tag_left1;
    t[2] = "";
    t[3] = tag_right1;
    t[4] = tag_right2;
    // first-order
    for (int i = 0; i < 5; i++) {
      // for (int i = 1; i < 4; i++) {
      if (t[i].isEmpty()) continue;
      sample.features.add(String.format("T%d_%s", i - 2, t[i]));
    }

    // second-order
    for (int i = 0; i < 4; i++) {
      int j = i + 1;
      if (t[i].isEmpty()) continue;
      if (t[j].isEmpty()) continue;
      sample.features.add(String.format("T%dT%d_%s_%s", i - 2, j - 2, t[i], t[j]));
    }

    if (!t[1].isEmpty() && !t[3].isEmpty()) {
      sample.features.add(String.format("T%dT%d_%s_%s", 1 - 2, 3 - 2, t[1], t[3]));
    }
    if (!t[0].isEmpty() && !t[1].isEmpty() && !t[3].isEmpty()) {
      sample.features.add(String.format("T%dT%dT%d_%s_%s_%s", 0 - 2, 1 - 2, 3 - 2, t[0], t[1], t[3]));
    }
    if (!t[1].isEmpty() && !t[3].isEmpty() && !t[4].isEmpty()) {
      sample.features.add(String.format("T%dT%dT%d_%s_%s_%s", 1 - 2, 3 - 2, 4 - 2, t[1], t[3], t[4]));
    }

    /*
     * for (int j = 0; j < vt.size(); j++) cout << vt.get(j].str << "/" << vt[j).pos << " "; cout << endl; cout << pos
     * << endl; for (List<String>::final_iterator j = sample.features.begin(); j != sample.features.end(); j++) { cout
     * << *j << " "; } cout << endl << endl;
     */

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
    ArrayList<Token> vt;
    ArrayList<Double> vent;
    ArrayList<Integer> order;
    // ArrayList<Integer> model;
    ArrayList<ArrayList<Tuple2<String, Double>>> vvp;
    double prob;

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

    Hypothesis(final ArrayList<Token> vt_, final ArrayList<ME_Model> vme) {
      prob = 1.0;
      vt = vt_;
      int n = vt.size();

      vent = newArrayList(n, 0.0);
      vvp = newArrayList(n, new Constructor<ArrayList<Tuple2<String, Double>>>() {
            @Override
            public ArrayList<Tuple2<String, Double>> neu() {
              return new ArrayList<Tuple2<String, Double>>();
            }
          });
      order = newArrayList(n, 0);
      // model.resize(n);

      for (int i = 0; i < n; i++) {
        vt.get(i).cprd = "";
        Update(i, vme);
      }
    }

    void Print() {
      for (int k = 0; k < vt.size(); k++) {
        // cout << vt.get(k).str << "/";
        // if (vt.get(k).cprd.equals("")) cout << "?";
        // cout << vt.get(k).cprd;
        // cout << " ";
      }
      // cout << endl;
    }

    void Update(final int j, final ArrayList<ME_Model> vme) {
      String tag_left1 = "BOS", tag_left2 = "BOS2";
      if (j >= 1) tag_left1 = vt.get(j - 1).cprd; // maybe bug??
      // if (j >= 1 && !vt.get(j-1].isEmpty()) pos_left1 = vt[j-1).cprd; // this should be correct
      if (j >= 2) tag_left2 = vt.get(j - 2).cprd;
      String tag_right1 = "EOS", tag_right2 = "EOS2";
      if (j <= vt.size() - 2) tag_right1 = vt.get(j + 1).cprd;
      if (j <= vt.size() - 3) tag_right2 = vt.get(j + 2).cprd;
      ME_Sample mes = mesample(vt, j, tag_left2, tag_left1, tag_right1, tag_right2);

      ArrayList<Double> membp;
      ME_Model mep = null;
      int bits = 0;
      if (TAG_WINDOW_SIZE >= 2 && !tag_left2.equals("")) bits += 8;
      if (!tag_left1.isEmpty()) bits += 4;
      if (!tag_right1.isEmpty()) bits += 2;
      if (TAG_WINDOW_SIZE >= 2 && !tag_right2.equals("")) bits += 1;
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
        vent.set(j, -maxp);
        // vent[j] = maxp; // easiest last
        // vent[j] = second / maxp;
        // vent[j] = entropy(membp);
        break;
      case LEFT_TO_RIGHT:
        vent.set(j, (double) j);
        break;
      case RIGHT_TO_LEFT:
        vent.set(j, (double) -j);
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
      for (int i = 0; i < vt.size() - 1; i++) {
        final String a = vt.get(i).cprd;
        final String b = vt.get(i + 1).cprd;
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
    int n = h.vt.size();
    int pred_position = -1;
    double min_ent = 999999;
    String pred = "";
    double pred_prob = 0;
    for (int j = 0; j < n; j++) {
      if (!h.vt.get(j).cprd.equals("")) continue;
      double ent = h.vent.get(j);
      if (ent < min_ent) {
        // pred = h.vvp[j].begin()->first;
        // pred_prob = h.vvp[j].begin()->second;
        min_ent = ent;
        pred_position = j;
      }
    }
    assert (pred_position >= 0 && pred_position < n);

    for (Tuple2<String, Double> k : h.vvp.get(pred_position)) {
      Hypothesis newh = h;

      newh.vt.get(pred_position).cprd = k._1;
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
        if (newh.vt.get(j).cprd.equals("")) newh.Update(j, vme);
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

  static void bidir_chuning_decode_beam(ArrayList<Token> vt, final ArrayList<ME_Model> vme) {
    int n = vt.size();
    if (n == 0) return;

    ArrayList<Hypothesis> vh = newArrayList(); // TODO check size
    Hypothesis h = new Hypothesis(vt, vme);
    vh.add(h);

    for (int i = 0; i < n; i++) {
      ArrayList<Hypothesis> newvh = newArrayList(); // TODO check size
      for (Hypothesis j : vh) {
        generate_hypotheses(i, j, vme, newvh);
      }
      Collections.sort(newvh, Hypothesis.Order);
      while (newvh.size() > BEAM_NUM) {
        newvh.remove(0);
      }
      vh = newvh;
    }

    if (!vh.isEmpty()) {
      h = last(vh);
    } else {
      // cerr << "warning: no hypothesis found" << endl;
      h = new Hypothesis(vt, vme);
    }

    ArrayList<String> tags = newArrayList(); // TODO check size
    for (int k = 0; k < n; k++) {
      // cout << h.vt.get(k].str << "/" << h.vt[k).cprd << "/" << h.order[k] << " ";
      tags.add(h.vt.get(k).cprd);
    }

    convert_startend_to_iob2_sub(tags);
    for (int k = 0; k < n; k++) {
      vt.get(k).cprd = tags.get(k);
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
