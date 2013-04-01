package com.jmcejuela.bio.jenia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class NamedEntityTest {

  @Test
  public void testLoadNeModelsNothingCrashes() {
    JeniaTagger.setModelsPath("src/test/resources/models");
    NamedEntity.init();
  }

  @Test
  public void testLoadWordInfo() {
    testMap(NamedEntity.load_word_info("models_named_entity/word_info"), 22056);
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
