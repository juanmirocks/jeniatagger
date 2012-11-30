package com.jmcejuela.bio.jenia.maxent;

import static com.jmcejuela.bio.jenia.util.Util.divEq;
import static com.jmcejuela.bio.jenia.util.Util.increase;
import static com.jmcejuela.bio.jenia.util.Util.max;
import static com.jmcejuela.bio.jenia.util.Util.newArrayList;
import static com.jmcejuela.bio.jenia.util.Util.plusEq;
import static com.jmcejuela.bio.jenia.util.Util.pop;
import static com.jmcejuela.bio.jenia.util.Util.resourceStream;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jmcejuela.bio.jenia.util.Constructor;
import com.jmcejuela.bio.jenia.util.Tuple2;
import com.jmcejuela.bio.jenia.util.Util;

/**
 * From maxent.h
 */
public class ME_Model {
  //TODO ideally all fields are final
  private ArrayList<Sample> _vs; // vector of training_samples
  final StringBag _label_bag = new StringBag();
  private final MiniStringBag _featurename_bag = new MiniStringBag();
  private double _sigma; // Gaussian prior
  private double _inequality_width;
  double[] _vl; // vector of lambda
  private ArrayList<Double> _va; // vector of alpha (for inequality ME)
  private ArrayList<Double> _vb; // vector of beta (for inequality ME)
  final ME_FeatureBag _fb = new ME_FeatureBag();
  private final ArrayList<ArrayList<Integer>> _feature2mef = newArrayList();
  private int _num_classes;
  private ArrayList<Double> _vee; // empirical expectation
  private ArrayList<Double> _vme; // empirical expectation
  private ArrayList<Sample> _heldout;
  private double _train_error; // current error rate on the training data
  private double _heldout_error; // current error rate on the heldout data
  private int _nheldout;
  private int _early_stopping_n;
  private ArrayList<Double> _vhlogl;
  private ME_Model _ref_modelp;

  public ME_Model() {
    _nheldout = 0;
    _early_stopping_n = 0;
    _ref_modelp = null;
  }

  public static Constructor<ME_Model> CONSTRUCTOR = new Constructor<ME_Model>() {
    @Override
    public ME_Model neu() {
      return new ME_Model();
    }
  };

  public final int num_classes() {
    return _num_classes;
  }

  public final String get_class_label(int i) {
    return _label_bag.Str(i);
  }

  public final int get_class_id(final String s) {
    return _label_bag.Id(s);
  }

  public void set_heldout(final int h, final int n) {
    _nheldout = h;
    _early_stopping_n = n;
  };

  // jenia: n = 0 default in set_heldout
  public void set_heldout(final int h) {
    set_heldout(h, 0);
  }

  public void set_reference_model(final ME_Model ref_model) {
    _ref_modelp = ref_model;
  };

  private static class Sample {
    int label;
    ArrayList<Integer> positive_features = newArrayList();
    ArrayList<Double> ref_pd = newArrayList(); // reference probability distribution

    // jenia, converted to Comparator
    private final boolean operator_less(final Sample x) {
      for (int i = 0; i < positive_features.size(); i++) {
        if (i >= x.positive_features.size()) return false;
        int v0 = positive_features.get(i);
        int v1 = x.positive_features.get(i);
        if (v0 < v1) return true;
        if (v0 > v1) return false;
      }
      return false;
    }

    /**
     * jenia: This is my interpretation of Sample comparator according to
     * operator_less.
     */
    static final Comparator<Sample> Order = new Comparator<Sample>() {
      @Override
      public int compare(Sample s1, Sample s2) {
        //this clause doesn't appear in the original operator_less
        if (s1.positive_features.size() < s2.positive_features.size())
          return -1;
        else if (s1.positive_features.size() > s2.positive_features.size())
          return +1;
        else {
          for (int i = 0; i < s1.positive_features.size(); i++) {
            int v1 = s1.positive_features.get(i);
            int v2 = s2.positive_features.get(i);
            if (v1 < v2) return -1;
            if (v1 > v2) return +1;
          }
          return 0;
        }
      }
    };
  }

  private static class ME_Feature {
    public final int body; // unsigned
    public final int label;
    //public final int feature;

    static final int MAX_LABEL_TYPES = 255;

    ME_Feature(int l, int f) {
      assert (l >= 0 && l <= MAX_LABEL_TYPES);
      assert (f >= 0 && f <= 0xffffff);
      body = ME_Feature.body(l, f);
      label = l;
      //feature = f;
    };

    /**
     * New in jenia. Used to calculate the body value of a {@link ME_Feature}
     * without having to create a new object.
     */
    final static int body(int l, int f) {
      return ((f << 8) + l);
    }
  }

  class ME_FeatureBag {
    // unsigned int, int
    final Map<Integer, Integer> mef2id;
    final ArrayList<ME_Feature> id2mef;

    ME_FeatureBag() {
      mef2id = new HashMap<Integer, Integer>();
      id2mef = newArrayList();
    }

    int Put(final ME_Feature i) {
      Integer j = mef2id.get(i.body);
      if (j == null) {
        int id = id2mef.size();
        id2mef.add(i);
        mef2id.put(i.body, id);
        return id;
      }
      return j;
    }

    final int Id(final ME_Feature i) {
      Integer j = mef2id.get(i.body);
      if (j == null) {
        return -1;
      }
      return j;
    }

    final ME_Feature Feature(Integer id) {
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

  static class MiniStringBag implements Iterable<Entry<String, Integer>> {
    int _size;
    Map<String, Integer> str2id;

    MiniStringBag() {
      _size = 0;
      str2id = new HashMap<String, Integer>();
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

    private static final int MINUS_ONE = new Integer(-1);

    final Integer Id(final String i) {
      Integer j = str2id.get(i);
      if (j == null)
        return MINUS_ONE;
      else
        return j;
    }

    // final
    int Size() {
      return _size;
    }

    void Clear() {
      str2id.clear();
      _size = 0;
    }

    /**
     * jenia: equivalent in original to:
     *
     * map_type::final_iterator begin() final { return str2id.begin(); }
     *
     * map_type::final_iterator end() final { return str2id.end(); }
     */
    @Override
    public Iterator<Entry<String, Integer>> iterator() {
      return str2id.entrySet().iterator();
    }
  }

  static class StringBag extends MiniStringBag {
    ArrayList<String> id2str;

    StringBag() {
      id2str = newArrayList();
    }

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
  }

  /**
   * the parameter is discarded and set to 1 at the beginning
   *
   * @param C
   *          discarded
   * @return
   */
  public int perform_GIS(int C) {
    // cerr << "C = " << C << endl;
    C = 1;
    // cerr << "performing AGIS" << endl;
    double[] pre_v = new double[_vl.length]; // jenia: same size as _vl's because later it can set _vl
    double pre_logl = -999999;
    for (int iter = 0; iter < 200; iter++) {
      double logl = update_model_expectation();
      // fprintf(stderr, "iter = %2d  C = %d  f = %10.7f  train_err = %7.5f", iter, C, logl, _train_error);
      if (_heldout.size() > 0) {
        // double hlogl = heldout_likelihood();
        // fprintf(stderr, "  heldout_logl(err) = %f (%6.4f)", hlogl, _heldout_error);
      }
      // cerr << endl;

      if (logl < pre_logl) {
        C += 1;
        _vl = pre_v;
        iter--;
        continue;
      }

      if (C > 1 && iter % 10 == 0) C--;

      pre_logl = logl;
      pre_v = _vl; // TODO jenia this doesn't have any effect
      assert (_vl.length >= _fb.Size());
      for (int i = 0; i < _fb.Size(); i++) {
        double coef = _vee.get(i) / _vme.get(i);
        _vl[i] = log(coef) / C;
      }
    }
    // cerr << endl;
    return 0; // jenia: the original didn't return anything explicitly
  }

  public int perform_LMVM() {
    // cerr << "performing LMVM" << endl;
    if (_inequality_width > 0) {
      int nvars = _fb.Size() * 2;
      double[] x = new double[nvars];

      // INITIAL POINT
      for (int i = 0; i < nvars / 2; i++) {
        x[i] = _va.get(i);
        x[i + _fb.Size()] = _vb.get(i);
      }

      // int info = BLMVMSolve(x, nvars);

      for (int i = 0; i < nvars / 2; i++) {
        _va.set(i, x[i]);
        _vb.set(i, x[i + _fb.Size()]);
        _vl[i] = _va.get(i) - _vb.get(i);
      }

      return 0;
    } else {

      int nvars = _fb.Size();
      double[] x = new double[nvars];

      // INITIAL POINT
      for (int i = 0; i < nvars; i++) {
        x[i] = _vl[i];
      }

      // int info = BLMVMSolve(x, nvars);

      for (int i = 0; i < nvars; i++) {
        _vl[i] = x[i];
      }

      return 0;
    }
  }

  public final int conditional_probability(final Sample s, double[] membp) {
    double[] powv = new double[_num_classes];

    for (Integer j : s.positive_features) {
      for (Integer k : _feature2mef.get(j)) {
        powv[_fb.Feature(k).label] += _vl[k];
      }
    }

    double pmax = max(powv);
    double offset = max(0.0, pmax - 700); // to avoid overflow

    double sum = 0;
    int max_label = 0;

    for (int label = 0; label < _num_classes; label++) {
      double pow = powv[label] - offset;
      double prod = exp(pow);
      //if (_ref_modelp != null) prod *= s.ref_pd.get(label);
      assert (prod != 0);
      membp[label] = prod;
      sum += prod;
    }
    for (int label = 0; label < _num_classes; label++) {
      membp[label] /= sum;
      if (membp[label] > membp[max_label]) max_label = label;
    }
    assert (max_label >= 0);
    return max_label;
  }

  int make_feature_bag(final int cutoff) {
    int max_num_features = 0;

    // count the occurrences of features
    // jenia: unsigned int, int
    Map<Integer, Integer> count = new HashMap<Integer, Integer>();
    if (cutoff > 0) {
      for (Sample i : _vs) {
        for (Integer j : i.positive_features) {
          increase(count, ME_Feature.body(i.label, j));
        }
      }
    }

    for (Sample i : _vs) {
      max_num_features = max(max_num_features, (i.positive_features.size()));
      for (Integer j : i.positive_features) {
        final ME_Feature feature = new ME_Feature(i.label, j);
        // if (cutoff > 0 && count[feature.body()] < cutoff) continue;
        if (cutoff > 0 && count.get(feature.body) <= cutoff) continue;
        // int id = _fb.Put(feature);, jenia
        // cout << i->label << "\t" << *j << "\t" << id << endl;
        // feature2sample[id].push_back(n);
      }
    }
    count.clear();

    // cerr << "num_classes = " << _num_classes << endl;
    // cerr << "max_num_features = " << max_num_features << endl;

    // int c = 0;, jenia

    init_feature2mef();

    return max_num_features;
  }

  public double heldout_likelihood() {
    double logl = 0;
    int ncorrect = 0;

    for (Sample i : _heldout) {
      double[] membp = new double[_num_classes];
      int l = classify(i, membp);
      logl += log(membp[i.label]);
      if (l == i.label) ncorrect++;
    }
    _heldout_error = 1 - (double) ncorrect / _heldout.size();

    return logl /= _heldout.size();
  }

  public double update_model_expectation() {
    double logl = 0;
    int ncorrect = 0;

    _vme = newArrayList(_fb.Size(), 0.0);

    for (Sample i : _vs) {
      double[] membp = new double[_num_classes];
      int max_label = conditional_probability(i, membp);

      logl += log(membp[i.label]);
      // cout << membp[*i] << " " << logl << " ";
      if (max_label == i.label) ncorrect++;

      // model_expectation
      for (Integer j : i.positive_features) {
        for (Integer k : _feature2mef.get(j)) {
          plusEq(_vme, k, membp[_fb.Feature(k).label]);
        }
      }
    }

    for (int i = 0; i < _fb.Size(); i++) {
      divEq(_vme, i, _vs.size());
    }

    _train_error = 1 - (double) ncorrect / _vs.size();

    logl /= _vs.size();

    if (_inequality_width > 0) {
      for (int i = 0; i < _fb.Size(); i++) {
        logl -= (_va.get(i) + _vb.get(i)) * _inequality_width;
      }
    } else {
      if (_sigma > 0) {
        final double c = 1 / (2 * _sigma * _sigma);
        for (int i = 0; i < _fb.Size(); i++) {
          logl -= _vl[i] * _vl[i] * c;
        }
      }
    }

    // logl /= _vs.size();

    // fprintf(stderr, "iter =%3d  logl = %10.7f  train_acc = %7.5f\n", iter, logl, (double)ncorrect/train.size());
    // fprintf(stderr, "logl = %10.7f  train_acc = %7.5f\n", logl, (double)ncorrect/_train.size());

    return logl;
  }

  // obsolete. just for downward compatibility
  public int train(final ArrayList<ME_Sample> vms, final int cutoff, final double sigma, final double widthfactor) {
    // convert ME_Sample to Sample
    // ArrayList<Sample> vs;
    _vs.clear();
    for (ME_Sample i : vms) {
      add_training_sample(i);
    }

    return train(cutoff, sigma, widthfactor);
  }

  void add_training_sample(final ME_Sample mes) {
    Sample s = new Sample();
    s.label = _label_bag.Put(mes.label);
    if (s.label > ME_Feature.MAX_LABEL_TYPES) {
      // cerr << "error: too many types of labels." << endl;
      System.exit(1);
    }
    for (String j : mes.features) {
      s.positive_features.add(_featurename_bag.Put(j));
    }

    if (_ref_modelp != null) {
      ME_Sample tmp = mes;
      s.ref_pd = _ref_modelp.classify(tmp);
    }

    _vs.add(s);
  }

  int train(final int cutoff, final double sigma, final double widthfactor) {
    if (sigma > 0 && widthfactor > 0) {
      // cerr << "error: Gausian prior and inequality modeling cannot be used together." << endl;
      return 0;
    }
    if (_vs.size() == 0) {
      // cerr << "error: no training data." << endl;
      return 0;
    }
    if (_nheldout >= _vs.size()) {
      // cerr << "error: too much heldout data. no training data is available." << endl;
      return 0;
    }
    // if (_nheldout > 0) random_shuffle(_vs.begin(), _vs.end());

    int max_label = 0;
    for (Sample i : _vs) {
      max_label = max(max_label, i.label);
    }
    _num_classes = max_label + 1;
    if (_num_classes != _label_bag.Size()) {
      // cerr << "warning: _num_class != _label_bag.Size()" << endl;
    }

    if (_ref_modelp != null) {
      // cerr << "setting reference distribution...";
      for (int i = 0; i < _ref_modelp.num_classes(); i++) {
        _label_bag.Put(_ref_modelp.get_class_label(i));
      }
      _num_classes = _label_bag.Size();
      for (Sample i : _vs) {
        set_ref_dist(i);
      }
      // cerr << "done" << endl;
    }

    for (int i = 0; i < _nheldout; i++) {
      _heldout.add(pop(_vs));
    }

    // for (std::ArrayList<Sample>::iterator i = _vs.begin(); i != _vs.end(); i++) {
    // sort(i->positive_features.begin(), i->positive_features.end());
    // }
    Collections.sort(_vs, Sample.Order);
    // for (std::ArrayList<Sample>::final_iterator i = _vs.begin(); i != _vs.end(); i++) {
    // for (ArrayList<Integer>::final_iterator j = i->positive_features.begin(); j != i->positive_features.end(); j++){
    // cout << *j << " ";
    // }
    // cout << endl;
    // }

    // _sigma = sqrt(Nsigma2 / (double)_train.size());
    _sigma = sigma;
    _inequality_width = widthfactor / _vs.size();

    if (cutoff > 0)
      // cerr << "cutoff threshold = " << cutoff << endl;
      if (_sigma > 0)
        // cerr << "Gaussian prior sigma = " << _sigma << endl;
        // cerr << "N*sigma^2 = " << Nsigma2 << " sigma = " << _sigma << endl;
        if (widthfactor > 0) {
          // cerr << "widthfactor = " << widthfactor << endl;
        }
    // cerr << "preparing for estimation...";
    // int C = make_feature_bag(cutoff);, jenia
    // _vs.clear();
    // cerr << "done" << endl;
    // cerr << "number of samples = " << _vs.size() << endl;
    // cerr << "number of features = " << _fb.Size() << endl;

    // cerr << "calculating empirical expectation...";

    // resize and set to 0
    _vee = newArrayList(_fb.Size(), 0.0);

    for (Sample i : _vs) {
      for (Integer j : i.positive_features) {
        for (Integer k : _feature2mef.get(j)) {
          if (_fb.Feature(k).label == i.label) plusEq(_vee, k, 1.0);
        }
      }
    }

    for (int i = 0; i < _fb.Size(); i++) {
      divEq(_vee, i, _vs.size());
    }
    // cerr << "done" << endl;

    // resizes

    _vl = new double[_fb.Size()];

    if (_inequality_width > 0) {
      _va = newArrayList(_fb.Size(), 0.0);
      _vb = newArrayList(_fb.Size(), 0.0);
    }

    // perform_GIS(C);
    perform_LMVM();

    if (_inequality_width > 0) {
      int sum = 0;
      for (int i = 0; i < _fb.Size(); i++) {
        if (_vl[i] != 0) sum++;
      }
      // cerr << "number of active features = " << sum << endl;
    }

    return 0; // jenia: no explicit value returned in original
  }

  void get_features(List<Tuple2<Tuple2<String, String>, Double>> fl) {
    fl.clear();
    // for (int i = 0; i < _fb.Size(); i++) {
    // ME_Feature f = _fb.Feature(i);
    // fl.push_back( make_pair(make_pair(_label_bag.Str(f.label()), _featurename_bag.Str(f.feature())), _vl[i]));
    // }

    // for (MiniStringBag::map_type::final_iterator i = _featurename_bag.begin(); i != _featurename_bag.end(); i++) {
    for (Entry<String, Integer> i : _featurename_bag) {
      for (int j = 0; j < _label_bag.Size(); j++) {
        String label = _label_bag.Str(j);
        String history = i.getKey();
        int id = _fb.Id(new ME_Feature(j, i.getValue()));
        if (id < 0) continue;
        fl.add(Tuple2.$(Tuple2.$(label, history), _vl[id]));
      }
    }
  }

  public boolean load_from_file(final String filename) {
    try {
      //_vl.clear();
      ArrayList<Double> _auxvl = newArrayList();
      _label_bag.Clear();
      _featurename_bag.Clear();
      _fb.Clear();

      // char[] buf = new char[1024];
      /*
       * jenia: the original algorithm read the file with a char buffer of size
       * 1024. In main this is fixed as the maximum line size. Therefore the
       * original algorithm was equivalent to read the file line by line.
       */

      BufferedReader br = new BufferedReader(new InputStreamReader(resourceStream(filename)));
      String line;
      while ((line = br.readLine()) != null) {
        String[] tokens = line.trim().split("\t");
        if (tokens.length < 3) {
          /*
           * TODO
           *
           * All lines should contain exactly 3 tokens but this one is a bug:
           *
           * "CONTAIN_UPPER 0.606178" (model.bidir.0)
           *
           * The original doesn't treat this explicitly but when this happens
           * classname is set to the empty String featurename to first token
           * (typically second) and w to the second token (typically third)
           */
          tokens = new String[] { "", tokens[0], tokens[1] };
        }
        String classname = tokens[0];
        String featurename = tokens[1];
        String w = tokens[2];
        double lambda = Double.parseDouble(w);// jenia: was float originally, but _vl is vector of doubles

        int label = _label_bag.Put(classname);
        int feature = _featurename_bag.Put(featurename);
        _fb.Put(new ME_Feature(label, feature));
        _auxvl.add(lambda);
      }

      _vl = Util.listDouble2arraydouble(_auxvl);
      _num_classes = _label_bag.Size();

      init_feature2mef();

      br.close();

      return true;
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  void init_feature2mef() {
    _feature2mef.clear();
    for (int i = 0; i < _featurename_bag.Size(); i++) {
      ArrayList<Integer> vi = newArrayList();
      for (int k = 0; k < _num_classes; k++) {
        int id = _fb.Id(new ME_Feature(k, i));
        if (id >= 0) vi.add(id);
      }
      _feature2mef.add(vi);
    }
  }

  boolean load_from_array(final ME_Model_Data[] data) {
    //_vl.clear();
    ArrayList<Double> _auxvl = newArrayList();
    for (int i = 0;; i++) {
      if (data[i].label.equals("///")) break;
      int label = _label_bag.Put(data[i].label);
      int feature = _featurename_bag.Put(data[i].feature);
      _fb.Put(new ME_Feature(label, feature));
      _auxvl.add(data[i].weight);
    }
    _vl = Util.listDouble2arraydouble(_auxvl);
    _num_classes = _label_bag.Size();

    init_feature2mef();

    return true;
  }

  public final void save_to_file(final String filename) {
    try {
      File fp = new File(filename);

      PrintWriter out = new PrintWriter(fp);
      for (Entry<String, Integer> i : _featurename_bag) {
        for (int j = 0; j < _label_bag.Size(); j++) {
          String label = _label_bag.Str(j);
          String history = i.getKey();
          int id = _fb.Id(new ME_Feature(j, i.getValue()));
          if (id < 0) continue;
          if (_vl[id] == 0) continue; // ignore zero-weight features
          out.format("%s\t%s\t%f\n", label, history, _vl[id]);
        }
      }

      out.close();
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  final void set_ref_dist(Sample s) {
    ArrayList<Double> v0 = s.ref_pd;
    ArrayList<Double> v = newArrayList(_num_classes);
    for (int i = 0; i < _num_classes; i++) {
      v.add(0.0);
      String label = get_class_label(i);
      int id_ref = _ref_modelp.get_class_id(label);
      if (id_ref != -1) {
        v.set(i, v0.get(id_ref));
      }
      if (v.get(i) == 0) v.set(i, 0.001); // to avoid -inf logl
    }
    s.ref_pd = v;
  }

  final int classify(final Sample nbs, double[] membp) {
    // ArrayList<Double> membp(_num_classes);
    assert (_num_classes == membp.length);
    conditional_probability(nbs, membp);
    int max_label = 0;
    double max = 0.0;
    for (int i = 0; i < membp.length; i++) {
      if (membp[i] > max) {
        max_label = i;
        max = membp[i];
      }
    }
    return max_label;
  }

  public final ArrayList<Double> classify(ME_Sample mes) {
    Sample s = new Sample();
    for (String j : mes.features) {
      Integer id = _featurename_bag.Id(j);
      if (id >= 0)
        s.positive_features.add(id);
    }
    if (_ref_modelp != null) {
      s.ref_pd = _ref_modelp.classify(mes);
      set_ref_dist(s);
    }

    double[] vp = new double[_num_classes];
    int label = classify(s, vp);
    mes.label = get_class_label(label);

    ArrayList<Double> ret = newArrayList();
    for (double d : vp) {
      ret.add(d);
    }
    return ret;
  }
}
