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

import java.io.Writer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipSpaceFactory;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.snip.attachment.Attachments;
import org.snipsnap.app.Application;
import org.snipsnap.render.macro.SnipMacro;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.radeox.util.logging.Logger;

/**
 * Attaches a file to the current snip.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class Attach extends SnipMacro {
  // TODO: infer content type from extension? code for this must have already
  // been written, somewhere or other
  private static final String CONTENT_TYPE = "unknown";

  public String getName() {
    return "attach";
  }

  public String getDescription() {
    return "Attach a file to the snip.";
  }

  public String[] getParamDescription() {
    return new String[]{"The path of a file to attach"};
  }

  /**
   * Uploads and attaches the file whose path is given in the macro parameters
   * to this snip.
   * @throws IllegalArgumentException if a file with that filename is already
   * attached.
   */
  public void execute(Writer writer, SnipMacroParameter params)
      throws IllegalArgumentException, IOException {
    File inFile = new File(params.get(0));
    if (!inFile.exists()) {
      throw new IllegalArgumentException("File " + inFile + " doesn't exist!");
    }

    Snip snip = params.getSnipRenderContext().getSnip();
    FileInputStream in = new FileInputStream(inFile);
    // note that snipsnap's Configuration class is generated from a "template":
    // org/snipsnap/config/Configuration.java.tmpl
    File fileStore = Application.get().getConfiguration().getFilePath();
    File out = new File(new File(fileStore, snip.getName()), inFile.getName());

    upload(in, out);
    attach(snip, out);

    writer.write("Uploaded, attached, and stored " + inFile + " .");
  }


  /**
   * Uploads a file to the server. Based on parts of
   * {@link:org.snipsnap.net.FileUploadServlet.doPost}.
   * @param in the contents of the file to attach
   * @param out where to store the attachment
   */
  public static void upload(InputStream in, File out) throws IOException {
    Logger.log(Logger.DEBUG, "Uploading to " + out.getCanonicalPath());

    if (out.exists()) {
      throw new IOException(out.getName() +
                            " exists, cowardly refusing to overwrite.");
    }

    // check and create the directory, where to store the snip attachments
    if (!out.getParentFile().isDirectory()) {
      out.getParentFile().mkdirs();
    }

    Utility.write(in, out);
  }


  /**
   * Attaches a file to a snip. The file should already be on the server. Based
   * on parts of {@link:org.snipsnap.net.FileUploadServlet.doPost}.
   *
   * @param snip the snip to attach the file to
   * @param attachment the attachment file
   */
  public static void attach(Snip snip, File attachment) {
    attachWithoutStore(snip, attachment);
    SnipSpaceFactory.getInstance().store(snip);
  }


  /**
   * This is here solely for testability.
   *
   * @param snip the snip to attach the file to
   * @param attachment the attachment file
   */
  static void attachWithoutStore(Snip snip, File attachment) {
    String filename = attachment.getName();
    Logger.log(Logger.DEBUG,
               "Attaching " + filename + " to the snip " + snip.getName());

    Attachments attachments = snip.getAttachments();
    if (attachments == null) {
      // no Attachments holder; make one
      attachments = new Attachments();
      snip.setAttachments(attachments);
    }

    if (attachments.getAttachment(filename) != null) {
      throw new IllegalArgumentException(filename + " is already attached!");
    }

    if (attachment.length() > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(filename + " is too big!");
    }
    int size = (int)attachment.length();

    String location = new File(snip.getName(), filename).getPath();
    attachments.addAttachment(filename, CONTENT_TYPE, size, location);
  }
}
