<?xml version="1.0" encoding="ISO-8859-1"?> <!-- -*- html -*- -->
<!--
  - snipsnap2svn v0.1
  - http://snarfed.org/space/snipsnap2svn
  - Copyright 2006 Ryan Barrett <snipsnap2svn@ryanb.org>
  -
  - File: snipsnap2svn.xslt
  -
  - snipsnap2svn converts SnipSnap XML database export dumps into Subversion
  - dumpfiles. This allows you to migrate your snips from SnipSnap into
  - Subversion, preserving their change history. You might want to do this if
  - you switch from SnipSnap to another CMS that stores pages as files, e.g.
  - PyBlosxom.
  -
  - Example usage:
  -
  - $ xsltproc snipsnap2svn.xslt snipsnap_db.xml | svnadmin load /path/to/repo
  -
  - By default, snipsnap2svn uses / as the path separator, appends .txt to
  - snips that don't have an extension, and starts revision numbers at 100000.
  - (They will be lowered to start at your current revision when Subversion
  - loads them.)
  -
  - You can change these settings by changing the xsl:variables after the
  - xsl:transform open tag - For example, you'll need to increase
  - BASE-REVISION if your repository has more than 100000 revisions..
  -
  - Also, note that snipsnap2svn recreates your snip hierarchy in the root
  - directory of your repository. to load it into a subdirectory in your
  - repository, use the -parent-dir argument to svnadmin load.
  -
  -
  - Snipsnap2svn has been tested against SnipSnap 1.0b2-uttoxeter and
  - Subversion 1.3.1, but it should work with any compatible versions.
  -
  - TODO: attachments. they're base64-encoded in SnipSnap XML dumps, but
  - they need to be raw binary in Subversion dump files, so we need a base64
  - decoder. unfortunately, i have yet to find one that works with big files.
  - http://gandhimukul.tripod.com/xmlxslt.htm is close, but overflows. :/
  -
  -
  - This program is free software; you can redistribute it and/or modify it
  - under the terms of the GNU General Public License as published by the Free
  - Software Foundation; either version 2 of the License, or (at your option)
  - any later version.
  -
  - This program is distributed in the hope that it will be useful, but WITHOUT
  - ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  - FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
  - more details.
  -
  - You should have received a copy of the GNU General Public License along
  - with this program; if not, write to the Free Software Foundation, Inc., 59
  - Temple Place, Suite 330, Boston, MA 02111-1307 USA
  -->

<xsl:transform version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/02/xpath-functions"
  xmlns:date="http://exslt.org/dates-and-times"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="date exsl set">

<!-- This script uses the EXSLT functions date:add, date:duration,      -->
<!-- exsl:node-set, and set:distinct. If your XSLT processor is missing -->
<!-- support for any of them, uncomment the appropriate import below.   -->
<!-- See http://exslt.org/ for more on EXSLT.                           -->

<!-- <xsl:import href="http://exslt.org/date/functions/add/date.add.template.xsl" /> -->
<!-- <xsl:import href="http://exslt.org/date/functions/duration/date.duration.template.xsl" /> -->
<!-- <xsl:import href="http://exslt.org/exsl/functions/node-set/exsl.node-set.template.xsl" /> -->
<!-- <xsl:import href="http://exslt.org/set/functions/distinct/set.distinct.template.xsl" /> -->


<!-- From http://gandhimukul.tripod.com/xmlxslt.htm. Used in the aborted -->
<!-- attempt to decode SnipSnap's base64-encoded attachments to binary.  -->
<!-- <xsl:include href="base64decoder.xsl" /> -->

<!-- Change these variables as needed to match your Subversion repository -->
<xsl:variable name="BASE-REVISION" select="'100000'" />
<xsl:variable name="PATH-SEPARATOR" select="'/'" />
<xsl:variable name="SNIP-FILE-EXT" select="'.snip'" />

<xsl:output method="text" encoding="utf-8" omit-xml-declaration="yes"
            indent="yes" />

<xsl:template match="/">
  <!-- SVN dumpfile header -->
  <xsl:text>SVN-fs-dump-format-version: 2
</xsl:text>

  <xsl:call-template name="add-all-dirs" />

  <xsl:for-each select="snipspace/snip/versions/snip">
    <xsl:sort select="version" data-type="number" />
    <xsl:call-template name="revision" />
    <xsl:call-template name="node" />
  </xsl:for-each>

  <!-- Left over from the aborted effort to include attachments. -->
<!--   <xsl:for-each select="snipspace/snip/attachments/attachment"> -->
<!--     <xsl:sort select="location" /> -->
<!--     <xsl:call-template name="attachment" /> -->
<!--   </xsl:for-each> -->
</xsl:template>


<!---->
<!-- Outputs nodes to add directories for all snips. -->
<!---->
<xsl:template name="add-all-dirs">
  <!-- add all dirs in a single revision -->
    <xsl:text>
Revision-number: </xsl:text>
    <xsl:number value="$BASE-REVISION" />
    <xsl:text>
Prop-content-length: 10
Content-length: 10

PROPS-END
</xsl:text>

  <!-- create a node set with the dirs to add, including subdirs -->
  <xsl:variable name="dirs-to-add">
    <xsl:for-each select="/snipspace/snip">
      <xsl:call-template name="add-dirs">
        <xsl:with-param name="path" select="name" />
      </xsl:call-template>
    </xsl:for-each>
  </xsl:variable>

  <!-- uniquify it -->
  <xsl:for-each select="set:distinct(exsl:node-set($dirs-to-add)/dir-to-add)">
      <xsl:text>
Node-path: </xsl:text>
      <xsl:value-of select="." />
      <xsl:text>
Node-kind: dir
Node-action: add
Prop-content-length: 10
Content-length: 10

PROPS-END
</xsl:text>
  </xsl:for-each>
</xsl:template>

<xsl:template name="add-dirs">
  <xsl:param name="path" />
  <xsl:param name="_base" value=""/>

  <!-- base case -->
  <xsl:if test="contains($path, $PATH-SEPARATOR)">
    <xsl:variable name="to-add"
      select="concat($_base, substring-before($path, $PATH-SEPARATOR))" />

    <dir-to-add>
      <xsl:value-of select="$to-add" />
    </dir-to-add>

    <!-- recurse! -->
    <xsl:call-template name="add-dirs">
      <xsl:with-param name="path"
                      select="substring-after($path, $PATH-SEPARATOR)" />
      <xsl:with-param name="_base" select="concat($to-add, $PATH-SEPARATOR)" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>


<!---->
<!-- Revision template. Outputs the revision data. Should be called inside -->
<!-- a snipspace/snip/versions/snip. -->
<!---->
<xsl:template name="revision">
    <xsl:text>
Revision-number: </xsl:text>
    <xsl:number value="$BASE-REVISION + position()" />

    <xsl:text>
Prop-content-length: </xsl:text>
    <xsl:number value="72 + string-length(mUser)" />

    <xsl:text>
Content-length: </xsl:text>
    <xsl:number value="72 + string-length(mUser)" />

    <xsl:text>

K 10
svn:author
V </xsl:text> <xsl:number value="string-length(mUser)" format="001" />

    <xsl:text>
</xsl:text>
    <xsl:value-of select="mUser" />

    <xsl:text>
K 8
svn:date
V 27
</xsl:text>
    <!-- use EXSLT's date library to convert from seconds since epoch to an -->
    <!-- ISO datetime string. see http://exslt.org/ and                     -->
    <!-- http://lists.fourthought.com/pipermail/exslt/2001-November/000349.html -->
    <xsl:variable name="epoch" select="'1970-01-01T00:00:00.000000'" />
    <xsl:variable name="secs_since" select="date:duration(mTime div 1000)" />
    <xsl:value-of select="substring(date:add($epoch, $secs_since), 1, 26)" />

    <xsl:text>Z
PROPS-END
</xsl:text>
</xsl:template>


<!---->
<!-- Node template. Outputs a node for a snip. Should be called inside a -->
<!-- snipspace/snip/versions/snip. -->
<!---->
<xsl:template name="node">
    <xsl:text>
Node-path: </xsl:text>

    <!-- if the snip doesn't have an extension, add the default extension -->
    <xsl:choose>
      <xsl:when test="contains(name, '.')">
        <xsl:value-of select="name" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat(name, $SNIP-FILE-EXT)" />
      </xsl:otherwise>
    </xsl:choose>

    <xsl:text>
Node-kind: file
Node-action:</xsl:text>
    <xsl:choose>
      <!-- version 1 is the first version, hence "add" -->
      <xsl:when test="version = 1">
        <xsl:text> add</xsl:text>
      </xsl:when>
      <!-- all other versions are changes -->
      <xsl:otherwise>
        <xsl:text> change</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:text>
Text-content-length: </xsl:text>
    <xsl:number value="string-length(content)" format="00001" />

    <xsl:text>
Content-length: </xsl:text>
    <xsl:number value="string-length(content)" format="00001" />

    <xsl:text>

</xsl:text> <xsl:value-of select="content" />
</xsl:template>


<!---->
<!-- Attachment template. Outputs a node for an attachment. Should be -->
<!-- called inside a snipspace/snip/attachments/attachment. -->
<!---->
<xsl:template name="attachment">
    <xsl:text>
Node-path: </xsl:text>
    <xsl:value-of select="location" />

    <xsl:text>
Node-kind: file
Node-action: add
Text-content-length: </xsl:text>

    <xsl:variable name="data">
      <xsl:call-template name="base64StringToBinary">
        <xsl:with-param name="string" select="data" />
      </xsl:call-template>
    </xsl:variable>

    <xsl:number value="string-length($data)" format="00001" />

    <xsl:text>
Content-length: </xsl:text>
    <xsl:number value="string-length($data)" format="00001" />

    <xsl:text>

</xsl:text> <xsl:value-of select="$data" />
</xsl:template>


</xsl:transform>
