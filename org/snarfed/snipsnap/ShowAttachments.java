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
 * @author Ryan Barrett <ryan@barrett.name>
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
    String snipName = params.getSnip().getName();
    Iterator it = params.getSnip().getAttachments().iterator();

    while (it.hasNext()) {
      String attachment = ((Attachment)it.next()).getName();
      writer.write("<a href=\"/space/" + snipName + "/" + attachment + "\">" +
                   attachment + "</a><br />\n");
    }
  }
}
