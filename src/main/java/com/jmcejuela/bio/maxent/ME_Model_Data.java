package com.jmcejuela.bio.maxent;

//
//for those who want to use load_from_array()
//
public class ME_Model_Data {
  final String label;
  final String feature;
  final double weight;

  public ME_Model_Data(String label, String feature, double weight) {
    this.label = label;
    this.feature = feature;
    this.weight = weight;
  }
}
