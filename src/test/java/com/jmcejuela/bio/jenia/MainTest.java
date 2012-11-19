package com.jmcejuela.bio.jenia;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import com.jmcejuela.bio.jenia.util.Util;

public class MainTest {

  @Test
  public void testNothingCrashes() throws IOException, InterruptedException {
    System.setIn(Util.resourceStream("/genia-nt.in"));
    File tmpOut = File.createTempFile("test-genia-nt", ".out");
    System.out.println("tmp out file: " + tmpOut.getAbsolutePath());
    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

    Main.main(new String[] { "-nt" });

    System.err.println("Done");
    return;
  }

}
