/**
 * Snarfed macros for SnipSnap
 * http://snarfed.org/space/snipsnap+macros
 * Copyright 2005 Ryan Barrett <snarfed@ryanb.org>
 *
 * The index-some macro takes a list of snip paths as its content, and displays
 * an index with only those snips and their immediate children. Use / to
 * include all top-level snips. For example, this will include all snips under
 * start (including blog posts), all themes, and all top-level snips:
 *
 *  {index-some}
 *  start
 *  SnipSnap/themes
 *  /
 *  {index-some}
 *
 * BUGS: only the first blog post of each day is considered a child of "start".
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

import org.radeox.util.i18n.ResourceManager;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.snipsnap.render.macro.ListOutputMacro;
import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipSpaceFactory;
import org.snipsnap.util.collection.Collections;
import org.snipsnap.util.collection.Filterator;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/*
 * Index macro that displays only the selected snips and their immediate
 * children. To display all top-level snips, use the "/" string. The lister is
 * user-selectable.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */

public class IndexSome extends ListOutputMacro {
  static final String ROOT_STRING = "/";

  public String getName() {
    return "index-some";
  }

  public String getDescription() {
    return "Takes a list of snip paths as its content, and displays an " +
      "index with only those snips and their immediate children. Use " +
      ROOT_STRING + " to include all top-level snips. For example, this " +
      "will include all snips under start (including blog posts), all " +
      "themes, and all top-level snips:\n" +
      "{code}\n" +
      "&#123;" /* left brace */ + getName() + "&#125;\n" /* right brace */ +
      "start\n" +
      "SnipSnap/themes\n" +
      ROOT_STRING + "\n" +
      "&#123;" + getName() + "&#125;" +
      "{code}\n";
  }

  public String[] getParamDescription() {
    return new String[] { "Lister to render snips" };
  }

  /**
   * A filterator that filters to only snips that are either in the roots list,
   * or are immediate children of a snip in the parents list.
   */
  static class ParentFilterator implements Filterator {
    private HashSet parents = new HashSet();

    public ParentFilterator(String[] parents) {
      for (int i = 0; i < parents.length; i++)
        this.parents.add(parents[i].trim());
    }

    public boolean filter(Object obj) {
      String snipName = ((Snip) obj).getName();

      // if this is a blog post, merge its date with its post number, so that
      // it's included under "start"
      if (snipName.endsWith("/1"))
        snipName = snipName.substring(0, snipName.length() - 2) + "-1";

      // is this snip in the list?
      if (parents.contains(snipName))
        return false;

      // is this snip's parent in the list?
      int lastSlash = snipName.lastIndexOf("/");
      if (lastSlash >= 0 &&
          parents.contains(snipName.substring(0, lastSlash)))
        return false;

      // if the user specified /, is this a top-level snip?
      if (parents.contains(ROOT_STRING) && lastSlash == -1)
        return false;

      return true;

    // namespaces are currently handled by slashes in the snip name. if/when
    // they switch to using parent/child, use the code below instead.
//       Snip parent = snip.getParent();
//       return !(parents.contains(snip.getName()) ||
//                (parent != null && parents.contains(parent.getName())));
    }
  }

  public void execute(Writer writer, SnipMacroParameter params)
    throws IllegalArgumentException, IOException {

    if (params.getLength() > 1) {
      throw new IllegalArgumentException(getName() +
                                         " only takes one parameter");
    }

    String type = params.get("0");
    String[] parents = Utility.split(params.getContent(), '\n');
    ParentFilterator filter = new ParentFilterator(parents);

    final Snip snip = params.getSnipRenderContext().getSnip();

    output(writer,
           snip,
           ResourceManager.getString("i18n.messages", "macro.index.all.snips"),
           Collections.filter(SnipSpaceFactory.getInstance().getAll(), filter),
           ResourceManager.getString("i18n.messages", "macro.index.none"),
           type,
           true /* show size */);
  }
}
