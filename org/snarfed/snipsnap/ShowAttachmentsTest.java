package org.snarfed.snipsnap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringWriter;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;
import org.snipsnap.snip.attachment.Attachments;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.radeox.engine.context.BaseRenderContext;

/**
 * Unit test for the ShowAttachments macro.
 *
 * @author Ryan Barrett <ryan@barrett.name>
 */
public class ShowAttachmentsTest extends TestCase {

  public ShowAttachmentsTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(ShowAttachmentsTest.class);
  }

  public void testExecute() throws Exception {
    // make a snip with attachments
    Snip snip = new SnipImpl("blah", "blah");
    Attachments attach = new Attachments();
    attach.addAttachment("fluff.jpg", "image/jpeg", 123, "fluff.jpg");
    attach.addAttachment("fluff.png", "image/png", 123, "fluff.png");
    attach.addAttachment("fluff.css", "text/css", 123, "fluff.css");
    snip.setAttachments(attach);

    // call execute
    StringWriter outWriter = new StringWriter();
    BaseRenderContext context = new BaseRenderContext();
    SnipMacroParameter param = new SnipMacroParameter(snip, context);
    new ShowAttachments().execute(outWriter, param);

    // check output
    String golden = "<a href=\"/space/blah/fluff.jpg\">fluff.jpg</a><br />\n" +
      "<a href=\"/space/blah/fluff.png\">fluff.png</a><br />\n" +
      "<a href=\"/space/blah/fluff.css\">fluff.css</a><br />\n";
    assertEquals(golden, outWriter.toString());
  }
}
