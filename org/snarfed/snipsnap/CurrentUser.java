/* Copyright 2003 Ryan Barrett <snarfed@ryanb.org>
 * This software is licensed under the GPL. See the LICENSE file for details.
 */
package org.snarfed.snipsnap;

import org.radeox.macro.Macro;
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
 * @author Ryan Barrett <ryan@barrett.name>
 */

public class CurrentUser extends Macro {
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
