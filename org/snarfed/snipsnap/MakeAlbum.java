/* Copyright 2003-2005 Maulik Shah <maulik@cs.stanford.edu>,
 *   Ryan Barrett <snarfed@ryanb.org>
 * This software is licensed under the GPL. See the LICENSE file for details.
 */
package org.snarfed.snipsnap;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import javax.swing.filechooser.FileSystemView;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;

import org.radeox.macro.BaseMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.macro.parameter.BaseMacroParameter;
import org.snipsnap.app.Application;


/**
 * The "make-album" macro for SnipSnap (http://snipsnap.org/). Creates an
 * album, with thumbnails and a slideshow, from a directory of pictures in the
 * images/ subdirectory of your SnipSnap application.
 *
 * @author Ryan Barrett (snarfed@ryanb.org),
 *         Maulik Shah (maulik@cs.stanford.edu)
 */

public class MakeAlbum extends BaseMacro {
  private static final String ERROR_START = "<span style='color: red;'> ";
  private static final String ERROR_END = "</span> ";
  private static final String USAGE =
    ERROR_START + "make-album error: please provide the picture directory " +
    "as an argument, e.g. \\{ make-album:Graduation 2003 \\}" + ERROR_END;
  private static final String THUMB_PREFIX = "thumb_";
  private static final String WEB_PREFIX = "web_";
    
  //needs to be in the config file 
  //////////////////////////////
  private File IMAGE_ROOT;
  
  //maximum dimension for thumbnails and websized versions of your images
  private int THUMB_SIZE;
  private int WEB_SIZE;
    
    
    
  // members
  private Writer writer_ = null;
  File albumDir_ = null;


  //
  // overridden methods in Macro...
  //
  public String getName() {
    return "make-album";
  }

  public String getDescription() {
    return "Generates a picture album (with thumbnails). The parameter is " +
      "the subdirectory of images/ that contains the pictures to display.";
  }

  public String[] getParamDescription() {
    String[] params = { "1: name", "?2:thumb_size", "?3:web_size" };
    return params;
  }
    

  /**
   * Runs the macro. (Overrides execute in Macro.)
   */
  public void execute(Writer writer, MacroParameter params)
      throws IllegalArgumentException, IOException {
    writer_ = writer;
    String relPath = "/images/";
    String absURL = Application.get().getConfiguration().getUrl(relPath);
    String imgPath = Application.get().getConfiguration().getFileStore();
    
    boolean isWindows=(imgPath.indexOf("\\")!=-1);
    
    if (isWindows)
      imgPath = imgPath.replace('\\','/');
    imgPath = imgPath.substring(0,imgPath.lastIndexOf("/"));
    imgPath = imgPath.substring(0,imgPath.lastIndexOf("/"));
    if (isWindows)
      imgPath = imgPath.replace('/','\\');
    
    
    File snipSnapDir = new File(".");
    String snipSnapDirStr = snipSnapDir.getAbsolutePath();
    snipSnapDirStr = snipSnapDirStr.substring(0,snipSnapDirStr.length()-1);
    
    String img_rt = imgPath;
    if ((img_rt.charAt(img_rt.length()-1)!='\\') ||
        (img_rt.charAt(img_rt.length()-1)!='/')) {
      if (img_rt.indexOf("/")!=-1)
        img_rt = img_rt+"/";
      else if (img_rt.indexOf("\\")!=-1)
        img_rt = img_rt+"\\";
    }

    File appRoot = new File(img_rt);    // added by Ryan 10/26/2003
    img_rt += (isWindows) ? "images\\" : "images/";
    IMAGE_ROOT = new File(img_rt);
    
    THUMB_SIZE = 240;
    WEB_SIZE = 800;
    

    if (params.getLength() < 1) {
      writer.write(USAGE);
      return;
    }
    String albumName = params.get("0");
    albumDir_ = new File(IMAGE_ROOT, albumName);

    if (!albumDir_.exists()) {
      String attachDir = (isWindows) ? "WEB-INF\\files\\" : "WEB-INF/files/";
      albumDir_ = new File(appRoot, attachDir + albumName);
      relPath = "/space/";
    }

    
    String thumb_size = params.get("1");
    if ((thumb_size!=null)&&(!thumb_size.equals(""))) {
      THUMB_SIZE = (new Integer(thumb_size)).intValue();
    }
    String web_size = params.get("2");
    if ((web_size!=null)&&(!web_size.equals(""))) {
      WEB_SIZE = (new Integer(web_size)).intValue();
    }


    // we may pass in different commands to this macro
    // entering captions etc.
    String command = "create";
    if (command.equals("create")) {
      createAlbum(albumName);
    } else if (command.equals("view")) {
      // writeAlbum(albumName);
    }
    
    
    
    
    ArrayList thumbs = getThumbs();
    // write out tags for thumbs and links to full images
    Iterator it = thumbs.iterator();
    while (it.hasNext()) {
      String thumb = (String)it.next();
      // albumName = albumName.replace(' ', '+');   // added by Ryan 10/26/2003
      String pic = relPath + albumName + '/' + thumb.substring(6);
      thumb = relPath + albumName + '/' + thumb;
        
      writer.write("<a target =\"_blank\" href='" + pic +".html" +"'>");
      writer.write("<img class='thumb' src='" + thumb + "' /></a>\r\n");
      writer.flush();
    }
    
  }


  //
  // helpers...
  //
  private final void createAlbum(String albumName) throws IOException {
    // get list of thumbnails
    ArrayList thumbs = getThumbs();
    if (thumbs == null) {
      writer_.write(ERROR_START + "make-album error: the directory "
                    + albumDir_ + " does not exist." + ERROR_END);
      return;
    }
    
    if (thumbs.size() == 0) { // no thumbnails! generate them
      makeThumbs();
      thumbs = getThumbs();
    }
    
    ArrayList webs = getWebs();
    if (webs==null) {
      writer_.write(ERROR_START + "make-album error: the directory "
                    + albumDir_ + " does not exist." + ERROR_END);
      return;
    }
    if (webs.size()==0) {
      makeWebImages();
      webs = getWebs();
    }
    
    ArrayList htmlPages = getSuffixedFiles(".html");
    if (htmlPages.size()==0) {
      createSlideShow(thumbs,webs);
    }
    
    
  }

    
  /**
   * generates an .html slide show.
   * it's not xhtml, nor does it do captions yet or use any kind of 
   * stylesheet...feel free to add.
   */
    
  private final void createSlideShow(ArrayList thumbs, ArrayList webs)
      throws IOException {
    writer_.write("Just a few more seconds to generate the HTML<br />\r\n");
    writer_.flush();
    
    ArrayList files = getDirList();

    for (Iterator it= files.iterator(); it.hasNext(); ) {
      String fileName = ((String)it.next()).toLowerCase();
      if (!fileName.endsWith(".jpg") || fileName.startsWith(WEB_PREFIX) ||
          fileName.startsWith(THUMB_PREFIX)) {
        it.remove();
      }
    }
      
    int prevIndex = files.size() - 1;
    int nextIndex = 1;
    if (files.size() == 1) {
      nextIndex = 0;
    }
    String prevImg,currImg,nextImg,bigImg;      

    for (int k = 0; k < files.size(); k++) {
      bigImg = (String)files.get(k);
      prevImg = (String)files.get(prevIndex);
      currImg = WEB_PREFIX+(String)files.get(k);
      nextImg = (String)files.get(nextIndex);
          
        
      BufferedWriter bW = new BufferedWriter(new FileWriter(albumDir_.getPath()+"/"+bigImg+".html"));          
      bW.write("<html><body><div align=center><table border=1>\n");
      bW.write("<tr><td colspan=3><div align=center><img src=\""+currImg+"\"></div></td></tr>\n");
      bW.write("<tr><td><div align=center><a href=\""+prevImg+".html"+"\"><img src=\""+THUMB_PREFIX+prevImg+"\"><br>The One Before</a></div></td>");
      bW.write("<td><a href=\""+bigImg+"\"><div align=center>The Full Size Version</a></div></td>");
      bW.write("<td><div align=center><a href=\""+nextImg+".html"+"\"><img src=\""+THUMB_PREFIX+nextImg+"\"><br>The Next One</a></div></td></tr>");
      bW.write("</table></div></body></html>");
      bW.close();
      prevIndex= k;
      nextIndex++;
      if (nextIndex==files.size()) nextIndex=0;
    }              
  }

  /**
   * Finds thumbnail images in the given
   * directory. (A thumbnail is any file that begins with THUMB_PREFIX.) If there
   * are no thumbnails in the given directory, an empty ArrayList is returned.
   */
  private final ArrayList getThumbs() {
    return getPrefixedFiles(THUMB_PREFIX);
  }
  
  
  /* getThumbs
   *
   * Returns an ArrayList of file names of the websized images in the given
   * directory. (A thumbnail is any file that begins with WEB_PREFIX.) If there
   * are no thumbnails in the given directory, an empty ArrayList is returned.
   */
  private final ArrayList getWebs() {
    return getPrefixedFiles(WEB_PREFIX);
  }


  private final ArrayList getPrefixedFiles(String prefix) {
    // find thumb_ files and add to ArrayList
    ArrayList files = getDirList();

    Iterator it = files.iterator();
    while (it.hasNext()) {
      if (!((String)it.next()).startsWith(prefix))
        it.remove();
    }

    return files;
  }

  private final ArrayList getSuffixedFiles(String suffix) {
  
    // find thumb_ files and add to ArrayList
    ArrayList files = getDirList();

    Iterator it = files.iterator();
    while (it.hasNext()) {
      if (!((String)it.next()).endsWith(suffix))
        it.remove();
    }

    return files;
  }
  

  /**
   * Makes a thumbnail of each image in the given directory.
   *
   * NOTE: Right now, this is hacked - we use convert, the command-line
   * ImageMagick utility, on the server side. This is obviously not very
   * portable.
   * 
   * NOTE: the command line utility is no longer required! Hoorray for 
   * all platforms using java 1.4.1
   */
  private final void makeThumbs() throws IOException  {
    writer_.write(
      "Please wait while make-album generates thumbnails. This may take a " +
      "while (2-5 seconds per picture). This message will only be displayed " +
      "once.");
    writer_.flush();

    ArrayList files = getDirList();

    for (Iterator it= files.iterator(); it.hasNext(); ) {
      String fileName = ((String)it.next()).toLowerCase();
      if (!fileName.endsWith(".jpg") || fileName.startsWith(WEB_PREFIX) ||
          fileName.endsWith(".html")) {
        it.remove();
      }
    }
    
    scaleImages(files, THUMB_PREFIX, THUMB_SIZE);

    writer_.write("done.<br />\r\n");
    writer_.flush();
  }

  
  private final void makeWebImages() throws IOException {
    writer_.write(
      "Please wait while make-album generates web images. This may take a " +
      "while (2-5 seconds per picture). This message will only be displayed " +
      "once.");
    writer_.flush();
      
    ArrayList files = getDirList();

    for (Iterator it= files.iterator(); it.hasNext(); ) {
      String fileName = ((String)it.next()).toLowerCase();
      if (!fileName.endsWith(".jpg") || fileName.startsWith(THUMB_PREFIX) ||
          fileName.endsWith(".html")) {
        it.remove();
      }
    }
    
    scaleImages(files, WEB_PREFIX, WEB_SIZE);
      
    writer_.write("done.<br />\r\n");    
    writer_.flush();
  }

  
  private void scaleImages(ArrayList listOfImages, String prefix, int maxSize)
      throws IOException {
    Iterator it = listOfImages.iterator();

    while (it.hasNext()) {
      String imageName = (String) it.next();
      File scaledFile = new File(albumDir_,prefix + imageName);
      File origFile  = new File(albumDir_,imageName);
      Image origImage = ImageIO.read(new File(albumDir_ + "/" + imageName));
      BufferedImage scaledImage = null;//scaleImage(maxSize, origImage);
      ImageIO.write(scaledImage, "jpg", scaledFile);

      writer_.write(".");
      writer_.flush();              
    }
  }

  /**
   * Returns an ArrayList of the names files in the given directory (just the
   * filenames, as Strings). If the directory does not exist, getDirList
   * returns null.
   */
  private final ArrayList getDirList()
  {
    FileSystemView view = FileSystemView.getFileSystemView();
    
    Vector fileVector = getDirectoryVector(new File(albumDir_.getPath()),"");
    ArrayList fileList = new ArrayList();
    for(int i=0; i<fileVector.size();i++) {
      fileList.add((String) fileVector.elementAt(i));
    }
    return fileList;
  }    


  private final Vector getDirectoryVector(File directory, String endFilter) {
    String endFilterU = endFilter.toUpperCase();
    Vector directoryVector = new Vector();

    if(directory.isDirectory()) {
      //add directory name as the first element of vector
      String path = directory.getParentFile().getAbsolutePath();
      String filePath = directory.getAbsolutePath();
      String fileInfo = directory.getName();
      File[] fileList = directory.listFiles();

      for(int i = 0; i < fileList.length; i++) {
        if(!fileList[i].isDirectory()) {
          String fileName = fileList[i].getName();
      
          if (fileName.toUpperCase().endsWith(endFilterU)) {
            fileInfo = fileName;
            directoryVector.addElement(fileInfo);        
          }
        }
      }
    } 

    return directoryVector;
  }



  /**
   * Test main
   */
  private static void main(String[] args) throws IOException {
    System.out.println("Calling execute...");

    PrintWriter out = new PrintWriter(System.out, true);
    BaseMacroParameter params = new BaseMacroParameter();

    params.setParams(args[0]);          
    new MakeAlbum().execute(out, params);
    out.flush();
  }
}
