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

import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;
import org.snipsnap.snip.attachment.Attachments;
import org.snipsnap.snip.attachment.Attachment;

/**
 * Unit test for the AlbumPic macro.
 *
 * NOTE: unit test code that uses SnipSpace (and effectively starts up SnipSnap
 * and the McKoi database) was checked in earlier, but it more or less required
 * a full snipsnap application (shockingly enough). I'll think more about this
 * later.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class AlbumPicTest extends TestCase {
  private static final File IMAGE = new File("org/snarfed/snipsnap/test.jpg");
  private static final File THUMB =
    new File("org/snarfed/snipsnap/thumb.test.jpg");

  public AlbumPicTest(String name) {
    super(name);
  }

  public void testGetAttachment() throws Exception {
    try {
      AlbumPic.getAttachment(UtilityTest.makeSnipWithoutAttachment());
      fail("retrieved an attachment from a snip without any!");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().indexOf("has no attachments") >= 0);
    }

    Snip snip = UtilityTest.makeSnipWithAttachment(IMAGE);
    assertEquals(IMAGE.getName(), AlbumPic.getAttachment(snip).getName());
  }

  public void testGetThumb() {
    // try a snip with no attachments
    try {
      AlbumPic.getThumb(UtilityTest.makeSnipWithoutAttachment());
      fail("got thumb from snip with attachments!");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().indexOf("has no attachments") >= 0);
    }

    // try a snip with a non-thumbnail attachment
    try {
      AlbumPic.getThumb(UtilityTest.makeSnipWithAttachment(IMAGE));
      fail("got thumb from snip without thumbnail!");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().indexOf("No thumbnail") >= 0);
    }

    // try a snip with a thumbnail
    File thumb = AlbumPic.getThumb(UtilityTest.makeSnipWithAttachment(THUMB));
    assertEquals(THUMB.getName(), thumb.getName());
  }

  // TODO: makeThumb() is going through some changes, so we won't test it yet
  public void dontTestMakeThumb() throws Exception {
    Snip snip = UtilityTest.makeSnipWithoutAttachment();
    File golden = new File("WEB-INF/files/foo", THUMB.getName());
    File thumb = null;

    try {
      thumb = AlbumPic.makeThumb(IMAGE, snip, 20);
      assertEquals(golden, thumb);
      assertEquals(golden, AlbumPic.getThumb(snip));
    } finally {
      // delete thumb and containing directories
      assertTrue(thumb.delete());
      File dir = thumb.getParentFile();
      assertTrue(dir.delete());                 // WEB-INF/files/foo/
      dir = dir.getParentFile();
      assertTrue(dir.delete());                 // WEB-INF/files/
      dir = dir.getParentFile();
      assertTrue(dir.delete());                 // WEB-INF/
    }
  }

  public void testWriteHtml() throws Exception {
    Snip snip = UtilityTest.makeSnipWithAttachment(THUMB);
    String golden = "<a href=\"/space/" + snip.getName() + "\">" +
      "<img src=\"/space/" + snip.getName() + "/" + THUMB.getName() +
      "\" /></a>";

    StringWriter outWriter = new StringWriter();
    AlbumPic.writeHtml(snip, outWriter);

    assertEquals(golden, outWriter.toString());
  }
}
