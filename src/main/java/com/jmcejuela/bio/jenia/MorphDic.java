package com.jmcejuela.bio.jenia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * From morphdic.cpp
 */
public class MorphDic {
  static Map<String, String> verbex;
  static Map<String, String> nounex;
  static Map<String, String> advex;
  static Map<String, String> adjex;
  static Set<String> noundic;
  static Set<String> verbdic;
  static Set<String> adjdic;

  private MorphDic() {}

  static void Init() {
    // cerr << "loading MorphDic...";
    LoadEx("./morphdic/noun.exc", nounex);
    LoadEx("./morphdic/verb.exc", verbex);
    LoadEx("./morphdic/adj.exc", adjex);
    LoadEx("./morphdic/adv.exc", advex);
    LoadIdx("./morphdic/noun.dic", noundic);
    LoadIdx("./morphdic/verb.dic", verbdic);
    LoadIdx("./morphdic/adj.dic", adjdic);
    // cerr << "done." << endl;
  }

  static void LoadEx(final String filename, Map<String, String> exmap) {
    try {
      File ifile = new File(filename);
      Scanner sc = new Scanner(ifile);
      while (sc.hasNextLine()) {
        String org = sc.next();
        String base = sc.next();
        exmap.put(org, base);

        exmap.put(
            Character.toUpperCase(org.charAt(0)) + org.substring(1),
            Character.toUpperCase(base.charAt(0)) + base.substring(1));

        sc.nextLine();
      }
      sc.close();
    } catch (Exception e) {
      throw new IOError(e);
    }
  }

  static void LoadIdx(final String filename, Set<String> dic) {
    try {
      File ifile = new File(filename);

      BufferedReader br = new BufferedReader(new FileReader(ifile));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.charAt(0) == ' ') continue;

        String base = line.split(" ")[0];
        dic.add(base);

        dic.add(Character.toUpperCase(base.charAt(0)) + base.substring(1));
      }
      br.close();
    } catch (Exception e) {
      throw new IOError(e);
    }
  }

  static boolean LookUpDicNoun(final String s) {
    return noundic.contains(s);
  }

  static boolean LookUpDicVerb(final String s) {
    return verbdic.contains(s);
  }

  static boolean LookUpDicAdj(final String s) {
    return adjdic.contains(s);
  }

  static String BaseFormNoun(final String s) {
    String ret = nounex.get(s);
    if (ret == null)
      return "";
    else
      return ret;
  }

  static String BaseFormVerb(final String s) {
    String ret = verbex.get(s);
    if (ret == null)
      return "";
    else
      return ret;
  }

  static String BaseFormAdj(final String s) {
    String ret = adjex.get(s);
    if (ret == null)
      return "";
    else
      return ret;
  }

  static String BaseFormAdv(final String s) {
    String ret = advex.get(s);
    if (ret == null)
      return "";
    else
      return ret;
  }

  static String base_form_noun(final String s)
  {
    String ex = MorphDic.BaseFormNoun(s);
    if (ex != "") return ex;

    int len = s.length();
    if (len > 1) {
      String suf1 = s.substring(len - 1);
      if (suf1 == "s") {
        if (MorphDic.LookUpDicNoun(s.substring(0, len - 1))) return s.substring(0, len - 1);
        // if (MorphDic.LookUpDicVerb(s.substring(0, len - 1))) return s.substring(0, len - 1);
      }
    }
    if (len > 4) {
      String suf4 = s.substring(len - 4);
      if (suf4 == "ches") return s.substring(0, len - 4) + "ch";
      if (suf4 == "shes") return s.substring(0, len - 4) + "sh";
    }
    if (len > 3) {
      String suf3 = s.substring(len - 3);
      if (suf3 == "ses") return s.substring(0, len - 3) + "s";
      if (suf3 == "xes") return s.substring(0, len - 3) + "x";
      if (suf3 == "zes") return s.substring(0, len - 3) + "z";
      if (suf3 == "men") return s.substring(0, len - 3) + "man";
      if (suf3 == "ies") return s.substring(0, len - 3) + "y";
    }
    if (len > 1) {
      String suf1 = s.substring(len - 1);
      if (suf1 == "s") return s.substring(0, len - 1);
    }
    return s;
  }

  static String base_form_verb(final String s) {
    String ex = MorphDic.BaseFormVerb(s);
    if (ex != "") return ex;
    if (MorphDic.LookUpDicVerb(s)) return s;

    int len = s.length();
    if (len > 3) {
      String suf3 = s.substring(len - 3);
      if (suf3 == "ies") return s.substring(0, len - 3) + "y";
      if (suf3 == "ing") {
        if (MorphDic.LookUpDicVerb(s.substring(0, len - 3)))
          return s.substring(0, len - 3);
        else
          return s.substring(0, len - 3) + "e";
      }
    }
    if (len > 2) {
      String suf2 = s.substring(len - 2);
      if (suf2 == "es" || suf2 == "ed") {
        if (MorphDic.LookUpDicVerb(s.substring(0, len - 2)))
          return s.substring(0, len - 2);
        else
          return s.substring(0, len - 2) + "e";
      }
    }
    if (len > 1) {
      String suf1 = s.substring(len - 1);
      if (suf1 == "s") return s.substring(0, len - 1);
    }
    return s;
  }

  static String base_form_adjective(final String s) {
    String ex = MorphDic.BaseFormAdj(s);
    if (ex != "") return ex;

    int len = s.length();
    if (len > 3) {
      String suf3 = s.substring(len - 3);
      if (suf3 == "est") {
        if (MorphDic.LookUpDicAdj(s.substring(0, len - 3) + "e"))
          return s.substring(0, len - 3) + "e";
        else
          return s.substring(0, len - 3);
      }
    }
    if (len > 2) {
      String suf2 = s.substring(len - 2);
      if (suf2 == "er") {
        if (MorphDic.LookUpDicAdj(s.substring(0, len - 2) + "e"))
          return s.substring(0, len - 2) + "e";
        else
          return s.substring(0, len - 2);
      }
    }
    return s;
  }

  static String base_form_adverb(final String s) {
    String ex = MorphDic.BaseFormAdv(s);
    if (ex != "") return ex;

    return s;
  }

  static void init_morphdic() {
    MorphDic.Init();
  }

  static String base_form(final String s, final String pos) {
    if (pos == "NNS") return base_form_noun(s);
    if (pos == "NNPS") return base_form_noun(s);

    if (pos == "JJR") return base_form_adjective(s);
    if (pos == "JJS") return base_form_adjective(s);

    if (pos == "RBR") return base_form_adverb(s);
    if (pos == "RBS") return base_form_adverb(s);

    if (pos == "VBD") return base_form_verb(s);
    if (pos == "VBG") return base_form_verb(s);
    if (pos == "VBN") return base_form_verb(s);
    if (pos == "VBP") return base_form_verb(s);
    if (pos == "VBZ") return base_form_verb(s);

    return s;
  }

}
