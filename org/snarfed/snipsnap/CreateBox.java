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

import org.snipsnap.app.Application;
import org.snipsnap.render.macro.SnipMacro;
import org.snipsnap.render.macro.parameter.SnipMacroParameter;
import org.snipsnap.user.User;

import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;

/*
 * If a user is logged in, a \"Create page\" text box is shown. Otherwise,
 * nothing is shown.
 * @author Ryan Barrett
 */

public class CreateBox extends SnipMacro {
  private final String CREATE_FORM_OPEN = 
    "<form class=\"form\" action=\"../exec/edit\" method=\"get\">\n";
  private final String CREATE_FORM_CLOSE = "</form>\n";
  private final String JAVASCRIPT =
    "<script type=\"text/javascript\" src=\"/snarfed.js\"></script>\n";
  private final String CREATE_TEXT_BOX =
    "<input size=\"18\" id=\"createpage\" value=\"Create page\"" +
    " name=\"name\" onfocus=\"set_if('createpage', 'Create page', '')\" " +
    " onblur=\"set_if('createpage', '', 'Create page')\" />";
  private final String LOGIN_OR_REGISTER =
    "Not logged in. <a href=\"/exec/login.jsp\"> Login </a> or " +
    "<a href=\"/exec/register.jsp\"> register</a>?\n";


  public String getName() {
    return "create-box";
  }

  public String getDescription() {
    return "If a user is logged in, a \"Create page\" text box is shown. " +
      "Otherwise, nothing is shown.";
  }

  public String[] getParamDescription() {
    return new String[] { "none" };
  }

  public void execute(Writer writer, SnipMacroParameter params)
      throws IllegalArgumentException, IOException {

    writer.write(JAVASCRIPT);

    User user = Application.get().getUser();
    if (user != null && !user.getLogin().equals("Guest")) {
      // show create page text box
      writer.write(CREATE_FORM_OPEN);
      writer.write(CREATE_TEXT_BOX);
      writer.write(CREATE_FORM_CLOSE);
    }
  }
}
