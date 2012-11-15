package com.jmcejuela.bio.jenia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.jmcejuela.bio.jenia.maxent.ME_Model;
import com.jmcejuela.bio.jenia.util.Util;

/**
 * From main.cpp
 */
public class Main {

  public static final String ENDL = System.getProperty("line.separator");

  public static void line(StringBuilder s, String msg) {
    s.append(msg);
    s.append(ENDL);
  }

  public static String help() {
    StringBuilder s = new StringBuilder();

    line(s, "Usage: geniatagger [OPTION]... [FILE]...");
    line(s, "Analyze English sentences and print the base forms, part-of-speech tags, ");
    line(s, "chunk tags, and named entity tags.");
    line(s, "");
    line(s, "Options:");
    line(s, "  -nt          don't perform tokenization.");
    line(s, "  --help       display this help and exit.");
    line(s, "");
    line(s, "Report bugs to: github.com/jmcejuela/jeniatagger/issues");

    return s.toString();
  }

  public static String version() {
    return "0.0.1";
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    boolean dont_tokenize = false;
    String ifilename = null, ofilename;
    for (String arg : args) {
      if (arg == "-nt") {
        dont_tokenize = true;
      }
      if (arg == "--help") {
        help();
        System.exit(0);
      }
      else
        ifilename = arg;
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // default standard input
    if (ifilename != "" && ifilename != "-") {
      in = new BufferedReader(new FileReader(new File(ifilename)));
    }

    // ----------------------------------------------------------------------------

    MorphDic.init_morphdic();

    ArrayList<ME_Model> vme = Util.newArrayList(16, new ME_Model()); // TODO check init value

    // cerr << "loading pos_models";
    for (int i = 0; i < 16; i++) {
      vme.get(i).load_from_file(String.format("./models_medline/model.bidir.%d", i));
      // cerr << ".";
    }
    // cerr << "done." << endl;

    // cerr << "loading chunk_models";
    ArrayList<ME_Model> vme_chunking = Util.newArrayList(16, new ME_Model()); // TODO check init value
    for (int i = 0; i < 8; i += 2) {
      vme_chunking.get(i).load_from_file(String.format("./models_chunking/model.bidir.%d", i));
      // cerr << ".";
    }
    // cerr << "done." << endl;

    NamedEntity.load_ne_models();

    String line;
    int n = 1;
    while ((line = in.readLine()) != null) {
      if (line.length() > 1024) {
        throw new IllegalArgumentException("warning: the sentence seems to be too long at line " + n +
            " (please note that the input should be one-sentence-per-line).");
      }
      String postagged = Bidir.bidir_postag(line, vme, vme_chunking, dont_tokenize);
      // cout << postagged << endl; //TODO
      n++;
    }

    in.close();
  }
}
