package org.snarfed.snipsnap;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for org.snarfed.snipsnap.
 *
 * @author Ryan Barrett <ryan@barrett.name>
 */
public class AllTests extends TestSuite {
  public AllTests(String name) {
    super(name);
  }

  /**
   * Suite of all tests; add your tests here.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite("org.snarfed.snipsnap unit tests");
    suite.addTestSuite(UtilityTest.class);
    suite.addTestSuite(AlbumPicTest.class);
    suite.addTestSuite(ShowAttachmentsTest.class);
    suite.addTestSuite(AttachTest.class);
    return suite;
  }
}
