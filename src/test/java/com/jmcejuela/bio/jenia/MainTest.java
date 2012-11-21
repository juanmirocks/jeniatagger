package com.jmcejuela.bio.jenia;

import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jmcejuela.bio.jenia.util.Util;

public class MainTest {

  @BeforeClass
  public static void setUpClass() throws Exception {
    //Force the dictionaries to be loaded only once at the beginning
    JeniaTagger.analyzeAll("load", true);
  }

  @Test
  public void testOutputNT() throws IOException {
    testSameAsOriginalOutput("/genia-nt.in", "/genia-nt.out", "-nt");
  }

  /**
   * TODO known to fail for named-entity recognition in some instances
   *
   * The test file is very big (the output is 8.8MB)
   *
   * It should throughly test whether jenia's output is the same as genia's. The Bc2Gm training data
   * contained various entities for all named-classes recognized by the genia tagger.
   *
   * @throws IOException
   */
  @Test
  public void testOutputNTBc2GmTrainingData() throws IOException {
    testSameAsOriginalOutput("genia-nt.bc2gm-training.in", "genia-nt.bc2gm-training.out", "-nt");
  }

  public void testSameAsOriginalOutput(String in, String out, String arguments) throws IOException {
    System.setIn(Util.resourceStream(in));
    File tmpOut = File.createTempFile("test-" + in, ".out");
    System.out.println(in + " test, tmp out file:" + tmpOut.getAbsolutePath());
    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

    long before = System.currentTimeMillis();
    Main.main(arguments.trim().split("\\s+"));
    long time = System.currentTimeMillis() - before;
    System.err.println("Done (" + (time / 1000) + " s). Now comparing the output files, line by line");

    BufferedReader expectOutput = new BufferedReader(new InputStreamReader(Util.resourceStream(out)));
    BufferedReader actualOutput = new BufferedReader(new FileReader(tmpOut));

    String expectLine = null;
    String actualLine = null;
    int n = 1;
    while (((expectLine = expectOutput.readLine()) != null) && ((actualLine = actualOutput.readLine()) != null)) {
      assertEquals("The lines are the same at line " + n, expectLine, actualLine);
      n++;
    }

    // "Both file must have the exact number of lines",
    assertEquals(null, expectLine);
    assertEquals(null, actualOutput.readLine()); // note, the last line of actual was not read yet

    expectOutput.close();
    actualOutput.close();
  }

  /**
   * TODO known to have different output for named-entity recognition with unicode characters.
   *
   * The original geniatagger actually does not handle unicode well.
   *
   * @throws IOException
   */
  @Test
  public void testUnicodeDoesNotCrash() throws IOException {
    System.setIn(Util.resourceStream("/genia-unicode.in"));
    File tmpOut = File.createTempFile("test-genia-unicode", ".out");
    System.out.println("genia unicode test: tmp out file: " + tmpOut.getAbsolutePath());
    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));
    Main.main(new String[] { "-nt" });
  }

}
