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
import org.junit.Ignore;
import org.junit.Test;

public class MainTest {

  static final String COMMON_ARGUMENTS = "--models src/test/resources/models --nt";

  @BeforeClass
  public static void setUpClass() throws Exception {
    //Force the dictionaries to be loaded only once at the beginning
    JeniaTagger.setModelsPath("src/test/resources/models");
    JeniaTagger.analyzeAll("load", true);
  }

  @Test
  public void testOutputNT() throws IOException {
    testSameAsOriginalOutput("genia-nt.in", "genia-nt.out", COMMON_ARGUMENTS);
  }

  /**
   * The test file is very big (the output is 8.8MB)
   * 
   * It should throughly test whether jenia's output is the same as genia's. The Bc2Gm training data
   * contained various entities for all named-classes recognized by the genia tagger.
   * 
   * @throws IOException
   */
  @Ignore
  @Test
  public void testOutputNTBc2GmTrainingData() throws IOException {
    testSameAsOriginalOutput("genia-nt.bc2gm-training.in", "genia-nt.bc2gm-training.out", COMMON_ARGUMENTS);
  }

  /**
   * Test for a subset of the Bc2GmTrainingData for rapid testing
   * 
   * @throws IOException
   */
  @Test
  public void testOutputNTBc2GmTrainingDataSmall() throws IOException {
    long before = System.currentTimeMillis();
    testSameAsOriginalOutput("genia-nt.bc2gm-training-3000.in", "genia-nt.bc2gm-training-3000.out", COMMON_ARGUMENTS);
    long after = System.currentTimeMillis();
    System.err.println("Time testOutputNTBc2GmTrainingDataSmall in s: " + ((double) (after - before)) / 1000);
  }

  public void testSameAsOriginalOutput(String in, String out, String arguments) throws IOException {
    System.setIn(ClassLoader.class.getResourceAsStream("/" + in));

    File tmpOut = File.createTempFile("test-" + in, ".out");
    System.out.println(in + " test, tmp out file:" + tmpOut.getAbsolutePath());
    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

    long before = System.currentTimeMillis();
    Main.main(arguments.trim().split("\\s+"));
    long time = System.currentTimeMillis() - before;
    System.err.println("Done (" + (time / 1000) + " s). Now comparing the output files, line by line");

    BufferedReader expectOutput = new BufferedReader(new InputStreamReader(ClassLoader.class.getResourceAsStream("/" + out)));
    BufferedReader actualOutput = new BufferedReader(new FileReader(tmpOut));

    String expectLine = null;
    String actualLine = null;
    int n = 1;
    while (((expectLine = expectOutput.readLine()) != null) && ((actualLine = actualOutput.readLine()) != null)) {
      assertEquals("The lines are not the same at line " + n, expectLine, actualLine);
      n++;
    }

    // "Both file must have the exact number of lines",
    assertEquals(null, expectLine);
    assertEquals(null, actualOutput.readLine()); // note, the last line of actual was not read yet

    expectOutput.close();
    actualOutput.close();
  }

  /**
   * TODO test known to fail.
   * 
   * In some particular cases jenia's and genia's output are different with unicode characters.
   * 
   * In fact, the original c++ code did not handle unicode well. It used std::string which makes a
   * unicode character of >= 2 bytes appear in a string as multiple characters and thus wrongnly
   * increment the size of the containing string. This leads to unexpected behavior in the same
   * original code.
   * 
   * @throws IOException
   */
  @Ignore
  @Test
  public void testUnicode() throws IOException {
    testSameAsOriginalOutput("genia-unicode.in", "genia-unicode.out", COMMON_ARGUMENTS);
  }

}
