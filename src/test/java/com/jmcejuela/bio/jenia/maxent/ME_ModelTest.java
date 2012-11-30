package com.jmcejuela.bio.jenia.maxent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ME_ModelTest {

  @Test
  public void test() {
    testModel("/models_medline/model.bidir.0", 105262);
    testModel("/models_medline/model.bidir.1", 98818);
    testModel("/models_medline/model.bidir.2", 103152);
    testModel("/models_medline/model.bidir.3", 102679);
    testModel("/models_medline/model.bidir.4", 101320);
    testModel("/models_medline/model.bidir.5", 100651);
    testModel("/models_medline/model.bidir.6", 107283);
    testModel("/models_medline/model.bidir.7", 111653);
    testModel("/models_medline/model.bidir.8", 101580);
    testModel("/models_medline/model.bidir.9", 98305);
    testModel("/models_medline/model.bidir.10", 98513);
    testModel("/models_medline/model.bidir.11", 102116);
    testModel("/models_medline/model.bidir.12", 102449);
    testModel("/models_medline/model.bidir.13", 99862);
    testModel("/models_medline/model.bidir.14", 108613);
    testModel("/models_medline/model.bidir.15", 114054);

    testModel("/models_chunking/model.bidir.0", 159110);
    testModel("/models_chunking/model.bidir.2", 96736);
    testModel("/models_chunking/model.bidir.4", 95383);
    testModel("/models_chunking/model.bidir.6", 36225);
  }

  public static <T> void testModel(String meResource, int numLinesFile) {
    System.out.println(meResource);
    ME_Model me = new ME_Model();
    me.load_from_file(meResource);
    testModel(me, numLinesFile);
  }

  public static <T> void testModel(ME_Model me, int numLinesFile) {
    assertEquals(numLinesFile, me._fb.Size());
    assertEquals(numLinesFile, me._vl.length);

    System.out.println("  # classes: " + me.num_classes());
    System.out.println("    classes: " + me._label_bag.id2str);
  }

}
