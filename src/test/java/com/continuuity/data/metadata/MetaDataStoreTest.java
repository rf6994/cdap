package com.continuuity.data.metadata;

import com.continuuity.api.data.MetaDataEntry;
import com.continuuity.api.data.MetaDataException;
import com.continuuity.api.data.MetaDataStore;
import com.continuuity.api.data.OperationException;
import com.continuuity.data.operation.ClearFabric;
import com.continuuity.data.operation.executor.OperationExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract public class MetaDataStoreTest {

  static OperationExecutor opex;
  static MetaDataStore mds;


  // test that a write followed by a read returns identity
  void testOneAddGet(boolean update, String account,
                     String application, String type, String id,
                     String field, String text,
                     String binaryField, byte[] binary)
      throws MetaDataException {

    MetaDataEntry meta = new MetaDataEntry(account, application, type, id);
    if (field != null) meta.addField(field, text);
    if (binaryField != null) meta.addField(binaryField, binary);
    if (update) mds.update(meta); else mds.add(meta);
    MetaDataEntry meta2 = mds.get(account, application, type, id);
    Assert.assertEquals(meta, meta2);
  }

  @Test
  public void testAddAndGet() throws MetaDataException {
    testOneAddGet(false, "a", "b", "name", "type",
        "a", "b", "abc", new byte[]{'x'});
    // test names and values with non-Latin characters
    testOneAddGet(false, "\1", null, "\0", "\u00FC",
        "\u1234", "", "\uFFFE", new byte[]{});
    // test text and binary fields with the same name
    testOneAddGet(false, "a", "b", "n", "t",
        "a", "b", "a", new byte[]{'x'});
  }

  // test that consecutive writes overwrite
  @Test public void testOverwrite() throws Exception {
    testOneAddGet(false, "a", null, "x", "1", "a", "b", "p", new byte[]{'q'});
    testOneAddGet(true, "a", null, "x", "1", "a", "c", "p", new byte[]{'r'});
    testOneAddGet(false, "a", "b", "x", "1", "a", "b", "p", new byte[]{'q'});
    testOneAddGet(true, "a", "b", "x", "1", "a", "c", "p", new byte[]{'r'});
  }

  // test that update with fewer columns deletes old columns
  @Test public void testFewerFields() throws Exception {
    testOneAddGet(false, "a", null, "y", "1", "a", "b", "p", new byte[]{'q'});
    testOneAddGet(true, "a", null, "y", "1", "a", "c", null, null);
  }

  // test that update fails if not existent
  @Test(expected = MetaDataException.class)
  public void testUpdateNonExisting() throws Exception {
    testOneAddGet(true, "a", null, "z", "1", "a", "c", null, null);
  }

  // test that insert fails if existent
  @Test(expected = MetaDataException.class)
  public void testAddExisting() throws Exception {
    testOneAddGet(false, "a", "a", "zz", "1", "a", "c", null, null);
    testOneAddGet(false, "a", "a", "zz", "1", "a", "c", null, null);
  }

  @Test
  public void testList() throws MetaDataException, OperationException {
    opex.execute(new ClearFabric(true, false, false));

    testOneAddGet(false, "a", "p", "x", "1", "a", "1", null, null);
    testOneAddGet(false, "a", "p", "y", "2", "a", "2", null, null);
    testOneAddGet(false, "a", "q", "x", "3", "b", "1", null, null);
    testOneAddGet(false, "a", null,"x", "4", "a", "2", null, null);
    testOneAddGet(false, "b", null,"x", "1", "b", "2", null, null);
    testOneAddGet(false, "b", "r", "y", "2", "b", "2", null, null);

    MetaDataEntry meta1 = mds.get("a", "p", "x", "1");
    MetaDataEntry meta2 = mds.get("a", "p", "y", "2");
    MetaDataEntry meta3 = mds.get("a", "q", "x", "3");
    MetaDataEntry meta4 = mds.get("a", null,"x", "4");
    MetaDataEntry meta5 = mds.get("b", null,"x", "1");
    MetaDataEntry meta6 = mds.get("b", "r", "y", "2");

    List<MetaDataEntry> entries;

    // list with account a for type x should yield meta1, meta3 and meta4
    entries = mds.list("a", null, "x", null);
    Assert.assertEquals(3, entries.size());
    Assert.assertTrue(entries.contains(meta1));
    Assert.assertTrue(entries.contains(meta3));
    Assert.assertTrue(entries.contains(meta4));

    // list type x for account a and app p should yield only meta1
    entries = mds.list("a", "p", "x", null);
    Assert.assertEquals(1, entries.size());
    Assert.assertTrue(entries.contains(meta1));

    // list type z for account b should yield nothing
    entries = mds.list("b", null, "z", null);
    Assert.assertTrue(entries.isEmpty());

    // list type y for account b should yield meta6
    entries = mds.list("b", null, "y", null);
    Assert.assertEquals(1, entries.size());
    Assert.assertTrue(entries.contains(meta6));

    // list all type x in acount a that have field a should yield 1,4
    Map<String, String> hasFieldA = Collections.singletonMap("a", null);
    entries = mds.list("a", null, "x", hasFieldA);
    Assert.assertEquals(2, entries.size());
    Assert.assertTrue(entries.contains(meta1));
    Assert.assertTrue(entries.contains(meta4));

    // list all type y that have field a should yield only 2
    entries = mds.list("a", "p", "y", hasFieldA);
    Assert.assertEquals(1, entries.size());
    Assert.assertTrue(entries.contains(meta2));

    // list all that have field a=1 should yield 1
    Map<String, String> hasFieldA1 = Collections.singletonMap("a", "1");
    entries = mds.list("a", null, "x", hasFieldA1);
    Assert.assertEquals(1, entries.size());
    Assert.assertTrue(entries.contains(meta1));
  }

  // test delete
  @Test
  public void testDelete() throws MetaDataException {
    // add an entry with a text and binary field
    MetaDataEntry meta = new MetaDataEntry("u", "q", "tbd", "whatever");
    meta.addField("text", "some text");
    meta.addField("binary", new byte[] { 'b', 'i', 'n' });
    mds.add(meta);

    // verify it was written
    Assert.assertEquals(meta, mds.get("u", "q", "tbd", "whatever"));

    // delete it
    mds.delete("u", "q", "tbd", "whatever");

    // verify it's gone
    Assert.assertNull(mds.get("u", "q", "tbd", "whatever"));
    Assert.assertFalse(mds.list("u", null, "tbd", null).contains(meta));

    // add another entry with same name and type
    MetaDataEntry meta1 = new MetaDataEntry("u", "q", "tbd", "whatever");
    meta1.addField("other", "other text");
    // add should succeed, update should fail
    try {
      mds.update(meta1);
      Assert.fail("update should fail");
    } catch (MetaDataException e) {
      //expected
    }
    mds.add(meta1);

    // read back entry and verify that it does not contain spurious
    // fields from the old meta data entry
    // verify it was written
    Assert.assertEquals(meta1, mds.get("u", "q", "tbd", "whatever"));
  }

  // test clear
  @Test
  public void testClear() throws MetaDataException {
    testOneAddGet(false, "a", "p", "a", "x", "a", "1", null, null);
    testOneAddGet(false, "a", "q", "b", "y", "a", "2", null, null);
    testOneAddGet(false, "a", null, "c", "z", "a", "1", null, null);
    testOneAddGet(false, "b", "q", "c", "z", "a", "1", null, null);

    // clear account a, app p
    mds.clear("a", "p");
    Assert.assertNull(mds.get("a", "p", "a", "x"));
    Assert.assertNotNull(mds.get("a", "q", "b", "y"));
    Assert.assertNotNull(mds.get("a", null, "c", "z"));

    // clear all for account a
    mds.clear("a", null);
    Assert.assertNull(mds.get("a", "q", "b", "y"));
    Assert.assertNull(mds.get("a", null, "c", "z"));

    // make sure account b is still there
    Assert.assertNotNull(mds.get("b", "q", "c", "z"));
  }

}
