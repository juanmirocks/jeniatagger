package com.jmcejuela.bio.jenia;

import static com.jmcejuela.bio.jenia.MorphDic.adjdic;
import static com.jmcejuela.bio.jenia.MorphDic.adjex;
import static com.jmcejuela.bio.jenia.MorphDic.advex;
import static com.jmcejuela.bio.jenia.MorphDic.noundic;
import static com.jmcejuela.bio.jenia.MorphDic.nounex;
import static com.jmcejuela.bio.jenia.MorphDic.verbdic;
import static com.jmcejuela.bio.jenia.MorphDic.verbex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class MorphDicTest {

  @BeforeClass
  public static void setUpClass() {
    MorphDic.init();
  }

  @Test
  public void testMorphDicLoadEx() {
    testMap(verbex, 5254);
    testMap(adjex, 1325);
    testMap(advex, 6);
    testMap(nounex, 5974);
  }

  @Test
  public void testMorphDicLoadIdx() {
    testSet(adjdic, 19101);
    testSet(noundic, 87642);
    testSet(verbdic, 14727);
  }

  /**
   * The size of each dic must be the file's number of lines times 2 - number of elems that do not start with a letter
   * (see alg.)
   */
  public static void testSet(Set<String> set, int numLinesFile) {
    assertEquals(numLinesFile * 2 - nonLetterStart(set), set.size());
    for (String elem : set) {
      assertTrue(elem != null);
    }
  }

  public static void testMap(Map<String, String> map, int numLinesFile) {
    testSet(map.keySet(), numLinesFile);
    for (String value : map.values()) {
      assertTrue(value != null);
    }
  }

  public static int nonLetterStart(Set<String> set) {
    int ret = 0;
    for (String elem : set) {
      if (!Character.isLetter(elem.charAt(0)))
        ret++;
    }
    return ret;
  }

}
