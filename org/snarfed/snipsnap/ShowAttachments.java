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
import java.io.IOException;

import org.snipsnap.snip.Snip;
import org.snipsnap.snip.attachment.Attachment;
import org.snipsnap.render.macro.SnipMacro;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;

/**
 * Lists all of this snip's attachments, as links to the attached files.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class ShowAttachments extends SnipMacro {
  public String getName() {
    return "show-attachments";
  }

  public String getDescription() {
    return "Lists all of the snip's attachments as links to the actual files.";
  }

  public String[] getParamDescription() {
    return new String[]{};
  }

  public void execute(Writer writer, SnipMacroParameter params)
      throws IllegalArgumentException, IOException {
    Snip snip = params.getSnipRenderContext().getSnip();
    String snipName = snip.getName();
    Iterator it = snip.getAttachments().iterator();

    while (it.hasNext()) {
      String attachment = ((Attachment)it.next()).getName();
      writer.write("<a href=\"/space/" + snipName + "/" + attachment + "\">" +
                   attachment + "</a><br />\n");
    }
  }
}
