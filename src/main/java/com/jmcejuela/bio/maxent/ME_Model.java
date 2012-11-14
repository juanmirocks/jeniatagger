package com.jmcejuela.bio.maxent;

import java.util.ArrayList;
import java.util.Map;

import com.jmcejuela.util.Tuple2;

/**
 * From maxent.h
 */
public class ME_Model {
  ArrayList<Sample> _vs; // vector of training_samples
  StringBag _label_bag;
  MiniStringBag _featurename_bag;
  double _sigma; // Gaussian prior
  double _inequality_width;
  ArrayList<Double> _vl; // vector of lambda
  ArrayList<Double> _va; // vector of alpha (for inequality ME)
  ArrayList<Double> _vb; // vector of beta (for inequality ME)
  ME_FeatureBag _fb;
  int _num_classes;
  ArrayList<Double> _vee; // empirical expectation
  ArrayList<Double> _vme; // empirical expectation
  ArrayList<ArrayList<Integer>> _feature2mef;
  ArrayList<Sample> _heldout;
  double _train_error; // current error rate on the training data
  double _heldout_error; // current error rate on the heldout data
  int _nheldout;
  int _early_stopping_n;
  ArrayList<Double> _vhlogl;
  ME_Model _ref_modelp; // jenia: was a pointer

  private ME_Model() {
    _nheldout = 0;
    _early_stopping_n = 0;
    _ref_modelp = null;
  }

  // public:
  // public void add_training_sample(const ME_Sample & s);
  // public int train(const int cutoff = 0, const double sigma = 0, const double widthfactor = 0);
  // public ArrayList<Double> classify(ME_Sample & s) const;
  // public boolean load_from_file(const String & filename);
  // public boolean save_to_file(const String & filename) const;
  public final int num_classes() {
    return _num_classes;
  }

  public final String get_class_label(int i) {
    return _label_bag.Str(i);
  }

  public final int get_class_id(final String s) {
    return _label_bag.Id(s);
  }

  // public void get_features(list< pair< pair<String, String>, Double> > & fl);

  public void set_heldout(final int h, final int n) {
    _nheldout = h;
    _early_stopping_n = n;
  };

  // jenia: n = 0 default in set_heldout
  public void set_heldout(final int h) {
    set_heldout(h, 0);
  }

  // public boolean load_from_array(const ME_Model_Data data[]);

  public void set_reference_model(final ME_Model ref_model) {
    _ref_modelp = ref_model;
  };

  // public:
  // // obsolete. just for downward compatibility
  // int train(const ArrayList<ME_Sample> & train,
  // const int cutoff = 0, const double sigma = 0, const double widthfactor = 0);

  private static class Sample {
    int label;
    ArrayList<Integer> positive_features;
    ArrayList<Tuple2<Integer, Double>> rvfeatures;
    ArrayList<Double> ref_pd; // reference probability distribution

    final boolean operator_less(final Sample x) {
      for (int i = 0; i < positive_features.size(); i++) {
        if (i >= x.positive_features.size()) return false;
        int v0 = positive_features.get(i);
        int v1 = x.positive_features.get(i);
        if (v0 < v1) return true;
        if (v0 > v1) return false;
      }
      return false;
    }
  }

  private static class ME_Feature {
    private final int _body; // unsigned

    int MAX_LABEL_TYPES = 255;

    // ME_Feature(const int l, const int f) : _body((l << 24) + f) {
    // assert(l >= 0 && l < 256);
    // assert(f >= 0 && f <= 0xffffff);
    // };
    // int label() const { return _body >> 24; }
    // int feature() const { return _body & 0xffffff; }

    ME_Feature(int l, int f) {
      _body = ((f << 8) + l);
      assert (l >= 0 && l <= MAX_LABEL_TYPES);
      assert (f >= 0 && f <= 0xffffff);
    };

    final int label() {
      return _body & 0xff;
    }

    final int feature() {
      return _body >> 8;
    }

    final int body() {
      return _body;
    }
  }

  private class ME_FeatureBag {
    // unsigned int, int
    Map<Integer, Integer> mef2id; // TODO not init
    ArrayList<ME_Feature> id2mef; // TODO not init

    int Put(final ME_Feature i) {
      Integer j = mef2id.get(i.body());
      if (j == null) {
        int id = id2mef.size();
        id2mef.add(i);
        mef2id.put(i.body(), id);
        return id;
      }
      return j;
    }

    final int Id(final ME_Feature i) {
      Integer j = mef2id.get(i.body());
      if (j == null) {
        return -1;
      }
      return j;
    }

    final ME_Feature Feature(int id) {
      assert (id >= 0 && id < id2mef.size());
      return id2mef.get(id);
    }

    final int Size() {
      return id2mef.size();
    }

    void Clear() {
      mef2id.clear();
      id2mef.clear();
    }
  }

  /* jenia: never used and otherwise String's hash could be used */
  // private static class hashfun_str {
  // final int operator_parentheses(final String s) {
  // //assert(sizeof(int) == 4 && sizeof(char) == 1);
  // final int p = reinterpret_cast<const int*>(s.c_str());
  // size_t v = 0;
  // int n = s.size() / 4;
  // for (int i = 0; i < n; i++, p++) {
  // // v ^= *p;
  // v ^= p << (4 * (i % 2)); // note) 0 <= char < 128
  // }
  // int m = s.size() % 4;
  // for (int i = 0; i < m; i++) {
  // v ^= s[4 * n + i] << (i * 8);
  // }
  // return v;
  // }
  // };

  private static class MiniStringBag {
    int _size;
    Map<String, Integer> str2id; // TODO not init

    MiniStringBag() {
      _size = 0;
    }

    int Put(final String i) {
      Integer j = str2id.get(i);
      if (j == null) {
        int id = _size;
        _size++;
        str2id.put(i, id);
        return id;
      }
      return j;
    }

    final int Id(final String i) {
      Integer j = str2id.get(i);
      if (j == null) return -1;
      return j;
    }

    // const
    int Size() {
      return _size;
    }

    void Clear() {
      str2id.clear();
      _size = 0;
    }
    // map_type::const_iterator begin() const { return str2id.begin(); } //TODO investigate what to do with these
    // map_type::const_iterator end() const { return str2id.end(); } //TODO
  }

  private static class StringBag extends MiniStringBag {
    ArrayList<String> id2str;

    @Override
    int Put(final String i) {
      Integer j = str2id.get(i);
      if (j == null) {
        int id = id2str.size();
        id2str.add(i);
        str2id.put(i, id);
        return id;
      }
      return j;
    }

    final String Str(final int id) {
      assert (id >= 0 && id < id2str.size());
      return id2str.get(id);
    }

    @Override
    final int Size() {
      return id2str.size();
    }

    @Override
    void Clear() {
      str2id.clear();
      id2str.clear();
    }
  };

  // double heldout_likelihood();
  // int conditional_probability(const Sample & nbs, ArrayList<Double> & membp) const;
  // int make_feature_bag(const int cutoff);
  // int classify(const Sample & nbs, ArrayList<Double> & membp) const;
  // double update_model_expectation();
  // int perform_LMVM();
  // int perform_GIS(int C);
  // void set_ref_dist(Sample & s) const;
  // void init_feature2mef();

  // BLMVM
  /*
   * int BLMVMComputeFunctionGradient(BLMVM blmvm, BLMVMVec X,double *f,BLMVMVec G); int BLMVMComputeBounds(BLMVM blmvm,
   * BLMVMVec XL, BLMVMVec XU); int BLMVMSolve(double *x, int n); int BLMVMFunctionGradient(double *x, double *f, double
   * *g, int n); int BLMVMLowerAndUpperBounds(double *xl,double *xu,int n); int Solve_BLMVM(BLMVM blmvm, BLMVMVec X);
   */
}
