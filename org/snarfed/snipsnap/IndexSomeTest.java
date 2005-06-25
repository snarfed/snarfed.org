/**
 * Snarfed macros for SnipSnap
 * http://snarfed.org/space/snipsnap+macros
 * Copyright 2005 Ryan Barrett <snarfed@ryanb.org>
 *
 * Unit test for the index-some macro.
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;

// import org.snipsnap.interceptor.Aspects;
import org.snipsnap.snip.Snip;
import org.snipsnap.snip.SnipImpl;

import org.snarfed.snipsnap.IndexSome.ParentFilterator;

/**
 * Unit test for the IndexSome macro.
 *
 * @author Ryan Barrett <snarfed@ryanb.org>
 */
public class IndexSomeTest extends TestCase {

  private static final String LEAF = "leaf";
  private static final String PARENT = "parent";
  private static final String CHILD1 = "parent/child1";
  private static final String CHILD2 = "parent/child2";
  private static final String GRANDCHILD = "parent/child2/grandchild2";
  private ArrayList snips;

  public IndexSomeTest(String name) {
    super(name);
  }

  /**
   * Mock snip class. Same as SnipImpl, except that it manages the list of
   * children itself, since SnipImpl requires a snipspace for children, and
   * no snipspace is set up for tests.
   */
  static class MockSnipImpl extends SnipImpl {
    private ArrayList mockChildren = new ArrayList();

    public MockSnipImpl(String name) {
      super(name, "content");
    }

    public void addSnip(Snip snip) {
      mockChildren.add(snip);
    }

    public List getChildren() {
      return mockChildren;
    }
  }

  public void setUp() throws Exception {
    snips = new ArrayList();

    snips.add(new MockSnipImpl(LEAF));
    snips.add(new MockSnipImpl(PARENT));
    snips.add(new MockSnipImpl(CHILD1));
    snips.add(new MockSnipImpl(CHILD2));
    snips.add(new MockSnipImpl(GRANDCHILD));

    // namespaces are currently handled by slashes in the snip name. if/when
    // they switch to using parent/child, use the tests below instead.
//     Snip parent = new MockSnipImpl("parent");
//     snips.add(parent);

//     Snip child1 = new MockSnipImpl("child1");
//     child1.setParent(parent);
//     snips.add(child1);

//     Snip child2 = new MockSnipImpl("child2");
//     child2.setParent(parent);
//     snips.add(child2);

//     Snip grandchild2 = new MockSnipImpl("grandchild2");
//     grandchild2.setParent(child2);
//     snips.add(grandchild2);
  }

  private ArrayList filter(String[] parents) {
    ArrayList filtered = new ArrayList();
    ParentFilterator pf = new ParentFilterator(parents);

    for (Iterator it = snips.iterator(); it.hasNext(); ) {
      Snip snip = (Snip) it.next();
      if (!pf.filter(snip))
        filtered.add(snip.getName());
    }

    return filtered;
  }

  private ArrayList filter(String parent) {
    return filter(new String[] { parent });
  }

  public void testEmpty() throws Exception {
    assertEquals(new ArrayList(), filter(new String[] {}));
  }

  public void testUnknown() throws Exception {
    assertEquals(new ArrayList(), filter("foo"));
  }

  public void testTrim() throws Exception {
    List list = Arrays.asList(new String[] { LEAF });
    assertEquals(list, filter(" \n\t" + LEAF + "  \t  \n"));
  }

  public void testLeaves() throws Exception {
    List list = Arrays.asList(new String[] { LEAF });
    assertEquals(list, filter(LEAF));

    list = Arrays.asList(new String[] { GRANDCHILD});
    assertEquals(list, filter(GRANDCHILD));
  }

  public void testParent() throws Exception {
    List list = Arrays.asList(new String[] { PARENT, CHILD1, CHILD2 });
    assertEquals(list, filter(PARENT));
  }

  public void testChild1() throws Exception {
    List list = Arrays.asList(new String[] { CHILD1 });
    assertEquals(list, filter(CHILD1));
  }

  public void testChild2() throws Exception {
    List list = Arrays.asList(new String[] { CHILD2, GRANDCHILD });
    assertEquals(list, filter(CHILD2));
  }

  public void testRoot() throws Exception {
    List list = Arrays.asList(new String[] { LEAF, PARENT });
    assertEquals(list, filter(IndexSome.ROOT_STRING));
  }

  public void testLeafAndParent() throws Exception {
    List list = Arrays.asList(
      new String[] { LEAF, PARENT, CHILD1, CHILD2 });
    assertEquals(list, filter(new String[] { LEAF, PARENT }));
  }

  public void testParentAndChild2() throws Exception {
    List list = Arrays.asList(
      new String[] { PARENT, CHILD1, CHILD2, GRANDCHILD });
    assertEquals(list, filter(new String[] { PARENT, CHILD2 }));
  }

  public void testRootAndChild2() throws Exception {
    List list = Arrays.asList(
      new String[] { LEAF, PARENT, CHILD2, GRANDCHILD });
    assertEquals(list, filter(new String[] { IndexSome.ROOT_STRING, CHILD2 }));
  }

  public void testBlogPostUnderStart() throws Exception {
    String blogPost = "start/2005-06-25/1";
    snips.add(new MockSnipImpl(blogPost));

    List list = Arrays.asList(new String[] { blogPost });
    assertEquals(list, filter("start"));
  }

  public void testDuplicate() throws Exception {
    List list = Arrays.asList(new String[] { LEAF });
    assertEquals(list, filter(new String[] { LEAF, LEAF }));

    list = Arrays.asList(new String[] { LEAF, GRANDCHILD });
    assertEquals(list, filter(new String[] { LEAF, GRANDCHILD, LEAF }));
  }
}
