package com.jmcejuela.bio.jenia.maxent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ME_ModelTest {

  @Test
  public void test() {
    // 16 files, numerated from 0 to 15
    for (int i = 0; i < 16; i++) {
      ME_Model me = new ME_Model();
      me.load_from_file("/models_medline/model.bidir." + i);

      testModel(me, 105262);
    }
  }

  public static <T> void testModel(ME_Model me, int numLinesFile) {
    // assertEquals(numLinesFile, me._label_bag._size);
    // assertEquals(numLinesFile, me._label_bag.Size());

    // assertEquals(numLinesFile, me._featurename_bag._size);
    // assertEquals(numLinesFile, me._featurename_bag.Size());

    // assertEquals(numLinesFile, me._fb.Size());

    // assertEquals(numLinesFile, me._vl.size());

    // assertEquals(numLinesFile, me._num_classes);
  }

}
