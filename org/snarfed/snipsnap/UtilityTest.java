package org.snarfed.snipsnap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;
import org.snipsnap.snip.attachment.Attachments;

/**
 * Unit test for the Utility class.
 *
 * @author Ryan Barrett <ryan@barrett.name>
 */
public class UtilityTest extends TestCase {
  private static final String SNIP_NAME = "foo";
  private static final String TEST_PNG = "org/snarfed/snipsnap/test.png";
  private static final String TEST_JPG = "org/snarfed/snipsnap/test.jpg";
  private static final String CONTENT_TYPE = "image/jpeg";
  private static final int SIZE = 123;

  public UtilityTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(UtilityTest.class);
  }

  public void testGetSnipName() {
    Snip snip = new SnipImpl("foo", "bar");
    assertEquals("foo", Utility.getSnipName(snip));

    snip.setName("a b c");
    assertEquals("a b c", Utility.getSnipName(snip));
  }

  public void testSplit() {
    String[] tokens1 = {"a", "b", "c"};
    assertEquals(tokens1, Utility.split("aXbXcX", 'X'));

    String[] tokens2 = {"a", "b", "c"};
    assertEquals(tokens2, Utility.split("aXbXc", 'X'));

    String[] tokens3 = {"a", "b", "c"};
    assertEquals(tokens3, Utility.split("XaXbXc", 'X'));

    String[] tokens4 = {"a", "b", "c"};
    assertEquals(tokens4, Utility.split("aXXbXXXc", 'X'));

    String[] tokens5 = {};
    assertEquals(tokens5, Utility.split("XXX", 'X'));

    String[] tokens6 = {"abc"};
    assertEquals(tokens6, Utility.split("abc", 'X'));

    String[] tokens7 = {"a"};
    assertEquals(tokens7, Utility.split("a", 'X'));

    String[] tokens8 = {};
    assertEquals(tokens8, Utility.split("X", 'X'));

    String[] tokens9 = {};
    assertEquals(tokens9, Utility.split("", 'X'));
  }

  public void testGetExtension() {
    // file with extension
    File file = new File("fluff.jpg");
    assertEquals("jpg", Utility.getExtension(file));

    // file without extension
    file = new File("fluff");
    assertEquals("", Utility.getExtension(file));

    // file with multiple extensions
    file = new File("fluff.jpga.jpgb.jpgc");
    assertEquals("jpgc", Utility.getExtension(file));

    // fully qualified file with extension
    file = new File("/usr/home/fluff/fluff.jpg");
    assertEquals("jpg", Utility.getExtension(file));

    // fully qualified directory with extension
    file = new File("/usr/home/fluff/fluff.tar/");
    assertEquals("tar", Utility.getExtension(file));

    // fully qualified directory without extension
    file = new File("/usr/home/fluff/fluff/");
    assertEquals("", Utility.getExtension(file));

    // fully qualified directory with extension in middle
    file = new File("/usr/home/fluff/a.b.c/fluff/");
    assertEquals("", Utility.getExtension(file));
  }

  public void testShrinkImage() throws Exception {
    File in = new File(TEST_PNG);
    File out = new File(TEST_PNG + ".scaled.png");

    // try shrinking the png
    try {
      Utility.shrinkImage(in, out, 5);
      BufferedImage small = ImageIO.read(out);
      assertEquals(5, small.getWidth());
      assertEquals(5, small.getHeight());
    } finally {
      assertTrue(out.delete());
    }

    // try enlarging the png (it should stay the same size)
    try {
      Utility.shrinkImage(in, out, 100);
      BufferedImage same = ImageIO.read(out);
      assertEquals(50, same.getWidth());
      assertEquals(50, same.getHeight());
    } finally {
      assertTrue(out.delete());
    }
  }

  public void testScaleImage() throws Exception {
    File in = new File(TEST_JPG);
    File out = new File(TEST_JPG + ".scaled.jpg");
    BufferedImage buf;

    try {
      // try enlarging the jpg
      Utility.scaleImage(in, out, 50);
      buf = ImageIO.read(out);
      assertEquals(50, buf.getWidth());
      assertEquals(50, buf.getHeight());

      // try again (file exists)
      Utility.scaleImage(in, out, 50);
      fail("allowed overwriting an existing file!");
    } catch (IOException e) {
      assertTrue(e.getMessage().indexOf("refusing to overwrite") >= 0);
    } finally {
      assertTrue(out.delete());
    }

    try {
      // try shrinking the jpg
      Utility.scaleImage(in, out, 5);
      buf = ImageIO.read(out);
      assertEquals(5, buf.getWidth());
      assertEquals(5, buf.getHeight());
    } finally {
      assertTrue(out.delete());
    }

    // try shrinking the png
    in = new File(TEST_PNG);
    out = new File(TEST_PNG + ".scaled.png");
    try {
      Utility.scaleImage(in, out, 5);
      buf = ImageIO.read(out);
      assertEquals(5, buf.getWidth());
      assertEquals(5, buf.getHeight());
    } finally {
      assertTrue(out.delete());
    }
  }

  public void testVerifyHasAttachments() {
    Utility.verifyHasAttachment(makeSnipWithAttachment(new File(TEST_JPG)));

    try {
      Utility.verifyHasAttachment(makeSnipWithoutAttachment());
      fail("verified that a snip had attachment, but it didn't!");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().indexOf("has no attachments") >= 0);
    }
  }


  // TODO: snipsnapIsRunning() doesn't work yet
//   public void testSnipsnapIsRunning() {
//     assertTrue(!Utility.snipsnapIsRunning());
//   }

  // helpers (also used in other test classes)
  static Snip makeSnipWithoutAttachment() {
    return new SnipImpl(SNIP_NAME, "blah");
  }

  static Snip makeSnipWithAttachment(File attach) {
    Snip snip = makeSnipWithoutAttachment();

    Attachments attachments = new Attachments();
    attachments.addAttachment(attach.getName(), CONTENT_TYPE, SIZE,
                              attach.getPath());
    snip.setAttachments(attachments);
    return snip;
  }

                             // helpers
  private static void assertEquals(String[] arr1, String[] arr2) {
    assertEquals(arr1.length, arr2.length);

    for (int i = 0; i < arr1.length; i++) {
      assertEquals(arr1[i], arr2[i]);
    }
  }
}
