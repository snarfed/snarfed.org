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

import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.attachment.Attachments;
import org.snipsnap.app.Application;

/**
 * Misc utilities for our SnipSnap macros.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class Utility {
  private static String DEFAULT_FILE_STORE = "WEB-INF/files";

  // prevent instantiation
  private Utility() {
  }

  /**
   * @return the file store path for the given snip (in the current
   * application).
   */
  public static File getFilePath(Snip snip) {
    File app = new File(DEFAULT_FILE_STORE);

    // TODO: getFilePath() is unhappy in 1.0b1. why?
//     try {
//       app = Application.get().getConfiguration().getFilePath();
//     } catch (NullPointerException e) {
//       // SnipSnap must not be running; use default
//       app = new File(DEFAULT_FILE_STORE);
//     }

    return new File(app, getSnipName(snip));
  }

  /**
   * @param snip a snip
   * @return the snip's full, human-readable name
   */
  public static String getSnipName(Snip snip) {
    return snip.getNameEncoded().replace('+', ' ');
  }

  /**
   * Splits a string into tokens by the given delimiter. Each token is a string
   * of non-delimiter characters followed by a delimiter character or the end
   * of the string. The delimiter characters are not included in tokens. The
   * empty string will never be considered a token.
   * @param line the string to split
   * @param delim the delimiter to split by
   * @return an array of tokens
   */
  public static String[] split (String line, char delim) {
    ArrayList tokens = new ArrayList();
    char[] chars = line.toCharArray();
    boolean isRepeat=false;

    StringBuffer token = new StringBuffer();
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == delim) {
        if (token.length() > 0) {
          tokens.add(token.toString());
          token = new StringBuffer();
        }

      } else {                  // not a delimiter
        token.append(chars[i]);
      }
    }
    
    if (token.length() > 0)
      tokens.add(token.toString());

    String[] tokensAsStrings = new String[tokens.size()];
    for (int i = 0; i < tokens.size(); i++) {
      tokensAsStrings[i] = (String)tokens.get(i);
    }

    return tokensAsStrings;
  }


  /**
   * Scales the specified image such that it has a maximum dimension of maxSize
   * (on either side), while preserving the original aspect ratio. If maxSize
   * is longer than the longest side of the image, the image is copied
   * unchanged.
   *
   * The output image format is inferred from the filename extension. Uses
   * ImageIO which requires Java 1.4.1.
   *
   * @param in a file to read the original image from
   * @param out a file to write the scaled image to
   * @param int maxSize maximum size (width or height), in pixels
   * @see javax.imageio.ImageIO.getWriterFormatNames
   * @throws IOException if the image cannot be read or written
   */
  public static void shrinkImage(File in, File out, int maxSize)
      throws IOException {
    BufferedImage original = ImageIO.read(in);
    int maxDimension = Math.max(original.getWidth(), original.getHeight());

    scaleImage(in, out, Math.min(maxSize, maxDimension));
  }
    

  /**
   * Scales the specified image such that it has a maximum dimension of maxSize
   * (on either side), while preserving the original aspect ratio. The output
   * image format is inferred from the filename extension. Uses ImageIO which
   * requires Java 1.4.1.
   *
   * Lifted and modified from:
   http://forum.java.sun.com/thread.jsp?thread=331419&forum=20$message=13506670
   *
   * @param in a file to read the original image from
   * @param out a file to write the scaled image to
   * @param int maxSize maximum size (width or height), in pixels
   * @see javax.imageio.ImageIO.getWriterFormatNames
   * @throws IOException if the image cannot be read or written
   */
  public static void scaleImage(File in, File out, int maxSize)
      throws IOException {
    if (out.exists()) {
      throw new IOException(out.getName() +
                            " exists, cowardly refusing to overwrite.");
    }

    BufferedImage original = ImageIO.read(in);

    // calculate scaling factor
    double scale;
    if (original.getWidth() > original.getHeight()) {
      scale = (double)maxSize / original.getWidth();
    } else {
      scale = (double)maxSize / original.getHeight();
    }

    // determine new dimensions
    int width = (int)(scale * original.getWidth());
    int height = (int)(scale * original.getHeight());

    // scale image (w/antialiasing)
    BufferedImage scaledBuf =
      new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = scaledBuf.createGraphics();
    Image scaledImg =
      original.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
    graphics.drawImage(scaledImg, 0, 0, null);
    graphics.dispose();

    String format = Utility.getExtension(out);
    if (!ImageIO.write(scaledBuf, format, out)) {
      throw new IOException("Image format '" + format + "' is not supported.");
    }
  }


  /**
   * @param path a file or directory name. May be fully qualified, relative, or
   * just a filename.
   * @return the extension (e.g. ".jpg" or ".html") of file, or the empty
   * string if no extension. If the file has two or more extensions (e.g.
   * "foo.tar.gz"), only the last extension (".gz") is returned.
   */
  public static String getExtension(String path) {
    return getExtension(new File(path));
  }

  /**
   * @param file a file or directory
   * @return the extension (e.g. "jpg" or "html") of file, or the empty string
   * if no extension. If the file has two or more extensions (e.g.
   * "foo.tar.gz"), only the last extension ("gz") is returned.
   */
  public static String getExtension(File file) {
    String name = file.getName();
    int period = name.lastIndexOf(".");

    if (period == -1) {
      return "";
    } else {
      return name.substring(period + 1);   // don't include the period
    }
  }


  /**
   * @param snip checks whether this snip has attachments
   * @throws IllegalArgumentException if the given snip has no attachments
   */
  public static void verifyHasAttachment(Snip snip) {
    String snipName = getSnipName(snip);
    Attachments atxs = snip.getAttachments();

    if (atxs == null) {
      throw new IllegalArgumentException(snipName + "has no attachments.");
    }

    // hack, to force snipsnap to show us a snip's attachments
    atxs.getAttachment("asdf");

    if (atxs.isEmpty()) {
      throw new IllegalArgumentException(snipName + "has no attachments.");
    }
  }


  /**
   * Writes the contents of an input stream to a file. Copied directly from
   * {@link:org.snipsnap.net.FileUploadServlet.storeAttachment}.
   *
   * NOTE: since the code is unchanged from the code in FileUploadServlet, I
   * haven't written a unit test. Shame on me.
   * @param in the input stream to read from
   * @param file the file to write to
   * @return the size of the file, in bytes
   */
  public static int write(InputStream in, File file) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    byte[] buf = new byte[4096];
    int length = 0, size = 0;
    while ((length = in.read(buf)) != -1) {
      out.write(buf, 0, length);
      size += length;
    }
    out.close();
    in.close();
    return size;
  }


  /**
   * @return true if SnipSnap is up and running and serving pages, false
   * otherwise (e.g. if we're running inside a JUnit unit test).
   */
  // TODO: this doesn't work...but would be great for testability. is there
  // another way to get this to work?
//   public static boolean snipsnapIsRunning() {
//     int apps = ApplicationLoader.getApplicationCount() +
//       ApplicationLoader.getApplicationErrorCount();

//     return (apps > 0);
//   }
}

