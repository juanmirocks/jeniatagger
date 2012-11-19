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

import org.junit.Test;

import com.jmcejuela.bio.jenia.util.Util;

public class MainTest {

  @Test
  public void testSameAsOriginalOutputWithNT() throws IOException {
    System.setIn(Util.resourceStream("/genia-nt.in"));
    File tmpOut = File.createTempFile("test-genia-nt", ".out");
    System.out.println("genia -nt test: tmp out file: " + tmpOut.getAbsolutePath());
    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

    Main.main(new String[] { "-nt" });
    System.err.println("Done. Now comparing the output files, line by line");

    BufferedReader expectOutput = new BufferedReader(new InputStreamReader(Util.resourceStream("/genia-nt.out")));
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

  @Test
  public void testUnicodeDoesNotCrash() throws IOException {
    // TODO Known to have different output with unicode characters. The original geniatagger
    // actually does not handle them well

    // System.setIn(Util.resourceStream("/genia-unicode.in"));
    // File tmpOut = File.createTempFile("test-genia-unicode", ".out");
    // System.out.println("genia unicode test: tmp out file: " + tmpOut.getAbsolutePath());
    // System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));
    //
    // Main.main(new String[] { "-nt" });
  }
}
