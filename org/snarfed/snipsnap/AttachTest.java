package org.snarfed.snipsnap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.radeox.engine.context.BaseRenderContext;

/**
 * Unit test for the Attach macro.
 *
 * @author Ryan Barrett <ryan@barrett.name>
 */
public class AttachTest extends TestCase {
  private static final File JPEG = new File("org/snarfed/snipsnap/test.jpg");
  private static final File PNG = new File("org/snarfed/snipsnap/test.png");

  public AttachTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(AttachTest.class);
  }

  public void testUpload() throws Exception {
    FileInputStream in = new FileInputStream(JPEG);
    File out = new File("bar/foo.jpg");

    try {
      assertTrue(!out.exists());
      Attach.upload(in, out);
      assertTrue(out.exists());
      assertEquals(JPEG.length(), out.length());
 
      try {
        Attach.upload(in, out);
        fail("Allowed clobbering an existing file!");
      } catch (IOException e) {
        assertTrue(e.getMessage().indexOf("refusing to overwrite") >= 0);
      }

    } finally {
      assertTrue(out.delete());
      assertTrue(out.getParentFile().delete());
    }
  }

  public void testAttachWithoutStore() throws Exception {
    Snip snip = new SnipImpl("blah", "blah");

    assertTrue(snip.getAttachments() == null);

    // try w/no previous attachments
    Attach.attachWithoutStore(snip, JPEG);
    Attachment attached =
      ((Attachment)snip.getAttachments().getAll().iterator().next());
//     assertEquals("", attached.getName() + " " + attached.getLocation());
    assertTrue(snip.getAttachments().getAttachment(JPEG.getName()) != null);

    // try w/a previous attachment
    Attach.attachWithoutStore(snip, PNG);
    assertTrue(snip.getAttachments().getAttachment(PNG.getName()) != null);

    // try attaching the same file
    try {
      Attach.attachWithoutStore(snip, PNG);
      fail("allowed attaching an already attached file");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().indexOf("is already attached") >= 0);
    }
  }
}
