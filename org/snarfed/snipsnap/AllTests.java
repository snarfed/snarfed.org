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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for org.snarfed.snipsnap.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class AllTests extends TestSuite {
  public AllTests(String name) {
    super(name);
  }

  /**
   * Suite of all tests; add your tests here.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite("org.snarfed.snipsnap unit tests");
    suite.addTestSuite(UtilityTest.class);
    suite.addTestSuite(AlbumPicTest.class);
    suite.addTestSuite(ShowAttachmentsTest.class);
    suite.addTestSuite(AttachTest.class);
    return suite;
  }
}
