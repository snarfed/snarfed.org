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
 * Copyright 2003-2004 Ryan Barrett <snarfed@ryanb.org>
 * This software is licensed under the GPL. See the LICENSE file for details.
 */
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
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class AttachTest extends TestCase {
  private static final File JPEG = new File("org/snarfed/snipsnap/test.jpg");
  private static final File PNG = new File("org/snarfed/snipsnap/test.png");

  public AttachTest(String name) {
    super(name);
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
