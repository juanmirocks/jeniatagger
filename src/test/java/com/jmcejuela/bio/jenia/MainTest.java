package com.jmcejuela.bio.jenia;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class MainTest {

  @Test
  public void testNothingCrashes() throws IOException, InterruptedException {
    Main.main(new String[] { "-nt" }); // keeps waiting forever for input
    // Thread.sleep(20 * 1000);
    return;
  }

}
