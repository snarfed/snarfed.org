/**
 * Snarfed macros for SnipSnap
 * http://snarfed.org/space/snipsnap+macros
 * Copyright 2003-2004 Ryan Barrett <snarfed@ryanb.org>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.snarfed.snipsnap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringWriter;

import org.radeox.engine.context.BaseRenderContext;
import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;
import org.snipsnap.snip.attachment.Attachments;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.snipsnap.render.context.SnipRenderContext;

/**
 * Unit test for the ShowAttachments macro.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class ShowAttachmentsTest extends TestCase {

  public ShowAttachmentsTest(String name) {
    super(name);
  }

  public void testExecute() throws Exception {
    // make a snip with attachments
    Attachments attach = new Attachments();
    attach.addAttachment("fluff.jpg", "image/jpeg", 123, "fluff.jpg");
    attach.addAttachment("fluff.png", "image/png", 123, "fluff.png");
    attach.addAttachment("fluff.css", "text/css", 123, "fluff.css");

    Snip snip = new SnipImpl("blah", "blah");
    snip.setAttachments(attach);

    // call execute
    StringWriter outWriter = new StringWriter();
    // TODO: test temporarily removed for now.
    //  see http://snipsnap.org/macro+unit+tests
//     SnipMacroParameter param = new MockParameter(snip);
//     BaseRenderContext context = new BaseRenderContext();
//     SnipMacroParameter param = new SnipMacroParameter(snip, context);
//     new ShowAttachments().execute(outWriter, param);

    // check output
    String golden = "<a href=\"/space/blah/fluff.jpg\">fluff.jpg</a><br />\n" +
      "<a href=\"/space/blah/fluff.png\">fluff.png</a><br />\n" +
      "<a href=\"/space/blah/fluff.css\">fluff.css</a><br />\n";
//     assertEquals(golden, outWriter.toString());
  }


//   static class MockParameter extends SnipMacroParameter {
//     Snip snip;
//     public MockParameter(Snip snip) { super(""); }
// //     protected SnipRenderContext getSnipRenderContext();
//     public MockContext getSnipRenderContext() { return new MockContext(snip); }
//   }

//   static class MockContext {
//     Snip snip;
//     public MockContext(Snip snip) { this.snip = snip; }
//     public Snip getSnip() { return snip; }
//   }
}
