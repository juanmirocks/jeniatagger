package com.jmcejuela.bio.jenia;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class NamedEntityTest {

  // @BeforeClass
  // public void setUpClass() {
  // NamedEntity.load_ne_models();
  // }

  @Test
  public void testLoadWordInfo() {
    testMap(NamedEntity.load_word_info("/models_named_entity/word_info"), 22056);
  }

  public static <T> void testSet(Set<T> set, int numLinesFile) {
    assertEquals(numLinesFile, set.size());
    for (T elem : set) {
      assertTrue(elem != null);
    }
  }

  public static <K, V> void testMap(Map<K, V> map, int numLinesFile) {
    testSet(map.keySet(), numLinesFile);
    for (V value : map.values()) {
      assertTrue(value != null);
    }
  }

}
