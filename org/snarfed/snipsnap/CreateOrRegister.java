/*
 * CreateOrRegister.java
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
 * If a user is logged in, a \"Create page\" text box is shown. Otherwise,
 * nothing is shown.
 * @author Ryan Barrett
 */

public class CreateOrRegister extends Macro {
  private final String CREATE_FORM_OPEN = 
    "<form class=\"form\" action=\"../exec/edit\" method=\"get\">\n";
  private final String CREATE_FORM_CLOSE = "</form>\n";
  private final String JAVASCRIPT =
    "<script type=\"text/javascript\" src=\"/snarfed.js\"></script>\n";
  private final String CREATE_TEXT_BOX =
    "<input size=\"18\" id=\"createpage\" name=\"name\" value=\"Create page\"" +
    " onfocus=\"set_if('createpage', 'Create page', '')\" " +
    " onblur=\"set_if('createpage', '', 'Create page')\" />";
  private final String LOGIN_OR_REGISTER =
    "Not logged in. <a href=\"/exec/login.jsp\"> Login </a> or " +
    "<a href=\"/exec/register.jsp\"> register</a>?\n";



  public String getName() {
    return "create-or-register";
  }

  public String getDescription() {
    return "If a user is logged in, a \"Create page\" text box is shown. If " +
      "not, a message is shown with links to login or register.";
  }

  public String[] getParamDescription() {
    return new String[]{"none"};
  }

  public void execute(Writer writer, MacroParameter params)
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



  /* test main
   */
  public static void main(String[] args) throws IOException {
    System.out.println("Calling execute...");

    PrintWriter out = new PrintWriter(System.out, true);
    BaseMacroParameter params = new BaseMacroParameter();

    new CreateOrRegister().execute(out, params);
    out.flush();
  }
}
