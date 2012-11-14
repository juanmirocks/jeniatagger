package com.jmcejuela.bio.maxent;

import java.util.ArrayList;

import com.jmcejuela.util.Tuple2;

/**
 * From maxent.h
 */
public class ME_Sample {
  public final String label;
  public final ArrayList<String> features;
  public final ArrayList<Tuple2<String, Double>> rvfeatures; // jenia: real value features

  public ME_Sample() {
    label = "";
    features = new ArrayList<String>();
    rvfeatures = new ArrayList<Tuple2<String, Double>>();
  }

  public ME_Sample(String l) {
    label = l;
    features = new ArrayList<String>();
    rvfeatures = new ArrayList<Tuple2<String, Double>>();
  }

  // jenia: not deprecated by genia but never used and this makes the structure mutable
  // void set_label(const std::string & l) { label = l; }

  // to add a binary feature
  void add_feature(String f) {
    features.add(f);
  }

  // to add a real-valued feature
  void add_feature(String s, final Double d) {
    rvfeatures.add(Tuple2.$(s, d));
  }

  // // obsolete
  // void add_feature(const std::pair<std::string, double> & f) {
  // rvfeatures.push_back(f); // real-valued features
  // }
}

/**
 * Never used.
 */
// //
// //for those who want to use load_from_array()
// //
// typedef struct ME_Model_Data
// {
// char * label;
// char * feature;
// double weight;
// } ME_Model_Data;

