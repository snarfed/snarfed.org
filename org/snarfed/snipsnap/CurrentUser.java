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

import org.radeox.macro.BaseMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.macro.parameter.BaseMacroParameter;
import org.snipsnap.app.Application;
import org.snipsnap.user.User;

import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;

/*
 * Macro that displays the current user, or "none" if the current user is not
 * logged in.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */

public class CurrentUser extends BaseMacro {
  public String getName() {
    return "current-user";
  }

  public String getDescription() {
    return "Displays the current user, or \"none\" if the current user is " +
           "not logged in..";
  }

  public String[] getParamDescription() {
    return new String[]{"none"};
  }

  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {

    User user = Application.get().getUser();
    writer.write("[");
    writer.write((user == null) ? "none" : user.getLogin());
    writer.write("]");
  }


  /* test main
   */
  public static void main(String[] args) throws IOException {
    System.out.println("Calling execute...");

    PrintWriter out = new PrintWriter(System.out, true);
    BaseMacroParameter params = new BaseMacroParameter();

    new CurrentUser().execute(out, params);
    out.flush();
  }
}
