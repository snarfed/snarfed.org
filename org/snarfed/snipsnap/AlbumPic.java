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

import java.util.Iterator;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import org.snipsnap.app.Application;
import org.snipsnap.snip.SnipSpace;
import org.snipsnap.snip.SnipSpaceFactory;
import org.snipsnap.snip.Snip;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.render.macro.SnipMacro;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;

/**
 * Includes a thumbnail of a picture in an album, and links back to the snip
 * with the original picture. You can make an album like so:
 *
 *  {art:foo}
 *  {art:bar}
 *  {art:baz}
 *
 * where foo, bar, and baz are snips with attached pictures. If foo, bar, and
 * baz have foo.jpg, bar.jpg, and baz.jpg attached, respectively, the output
 * generated from the above macros would be:
 *
 *  <a href="/space/foo"><img src="/space/foo/thumb.foo.jpg" /></a>
 *  <a href="/space/bar"><img src="/space/bar/thumb.bar.jpg" /></a>
 *  <a href="/space/baz"><img src="/space/baz/thumb.baz.jpg" /></a>
 *
 * where thumb_* are generated from the original pictures. The thumbnails are
 * only generated once.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class AlbumPic extends SnipMacro {
  static final String THUMB_PREFIX = "thumb.";
  static final int DEFAULT_SIZE = 120;

  public String getName() {
    return "albumpic";
  }

  public String getDescription() {
    return "Includes a picture in an album.";
  }

  public String[] getParamDescription() {
    return new String[]{"the name of a snip with an attached picture"};
  }

  public void execute(Writer writer, SnipMacroParameter params)
      throws IllegalArgumentException, IOException {
    // for debugging
    try{
    Snip snip = params.getSnipRenderContext().getSnip();

    // find and load target snip
    String imgSnipName = params.get(0);
    SnipSpace space = SnipSpaceFactory.getInstance();
    if (!space.exists(imgSnipName)) {
      throw new IllegalArgumentException("No snip named " + imgSnipName);
    }
    Snip target = space.load(imgSnipName);

    // get size (or use default if size isn't given)
    int size = DEFAULT_SIZE;
    if (params.getLength() > 1) {
      size = Integer.parseInt(params.get(1));
    }

    // generate the thumbnail and HTML
    try {
      getThumb(target);
    } catch (IllegalArgumentException e) {
      File thumb = makeThumb(getAttachment(target), target, size);
      Attach.attach(target, thumb);
      target = space.load(imgSnipName); // reload, since we added an atxment
    }

    writeHtml(target, writer);

    // for debugging
    } catch (Exception e) {
      e.printStackTrace(new java.io.PrintWriter(writer));
      throw new RuntimeException(e.toString());
    }
  }


  // helpers (package private for testability)
  /**
   * @param snip the snip to retrieve an attachment from
   * @return the first attachment on the given snip
   * @throws IllegalArgumentException if the snip has no attachments
   */
  static File getAttachment(Snip snip) {
    Utility.verifyHasAttachment(snip);

    Attachment attach = (Attachment)snip.getAttachments().iterator().next();
    return new File(Utility.getFilePath(snip), attach.getName());
  }

  /**
   * @param snip the snip to examine
   * @return the first thumbnail attached to the snip
   * @throws IllegalArgumentException if the snip doesn't have a thumbnail
   */
  static File getThumb(Snip snip) {
    Utility.verifyHasAttachment(snip);

    for (Iterator it = snip.getAttachments().iterator(); it.hasNext(); ) {
      String filename = ((Attachment)it.next()).getName();
      if (filename.startsWith(THUMB_PREFIX)) {
        return new File(Utility.getFilePath(snip), filename);
      }
    }

    throw new IllegalArgumentException("No thumbnail on " + snip.getName());
  }

  /**
   * Generates a thumbnail for the given image and uploads it to the given
   * snip's file space. However, it does *not* attach the thumbnail to the
   * snip. Use Attach.attach() for that.
   * @param image the image to generate a thumbnail for
   * @param snip the target snip
   * @param size the maximum size (of either dimension) of the thumbnail
   * @return the thumbnail
   */
  static File makeThumb(File image, Snip snip, int size) throws IOException {
    File attachDir = Utility.getFilePath(snip);
    File thumb = new File(attachDir, THUMB_PREFIX + image.getName());

    thumb.getParentFile().mkdirs();
    Utility.shrinkImage(image, thumb, size);

    return thumb;
  }

  /**
   * Writes HTML code to display the given snip's thumbnail image, linked to
   * the snip.
   * @param snip the target snip
   * @param writer writes to this writer
   */
  static void writeHtml(Snip snip, Writer writer) throws IOException {
    String snipUrl = "/space/" + Utility.getSnipName(snip);
    File thumb = getThumb(snip);

    writer.write("<a href=\"" + snipUrl + "\"><img src=\"" + snipUrl + "/" +
                 thumb.getName() + "\" /></a>");
  }

}


