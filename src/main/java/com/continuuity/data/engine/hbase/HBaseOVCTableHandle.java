/*
 * Copyright (c) 2012 Continuuity Inc. All rights reserved.
 */
package com.continuuity.data.engine.hbase;

import com.continuuity.api.data.OperationException;
import com.continuuity.data.engine.hbase.HBaseNativeOVCTable.IOExceptionHandler;
import com.continuuity.data.table.OrderedVersionedColumnarTable;
import com.continuuity.data.table.SimpleOVCTableHandle;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HBaseOVCTableHandle extends SimpleOVCTableHandle {

  private static final Logger Log
      = LoggerFactory.getLogger(HBaseOVCTableHandle.class);

  protected final Configuration conf;
  protected final HBaseAdmin admin;

  protected static final IOExceptionHandler exceptionHandler =
      new HBaseIOExceptionHandler();

  protected static final byte [] FAMILY = Bytes.toBytes("fam");

  @Inject
  public HBaseOVCTableHandle(
      @Named("HBaseOVCTableHandleConfig")Configuration conf)
          throws IOException {
    this.conf = conf;
    this.admin = new HBaseAdmin(conf);
  }

  @Override
  public OrderedVersionedColumnarTable createNewTable(byte[] tableName)
      throws OperationException {
    HBaseNativeOVCTable table = null;
    try {
      createTable(tableName, FAMILY);
      table = new HBaseNativeOVCTable(this.conf, tableName, FAMILY,
          new HBaseIOExceptionHandler());
    } catch (IOException e) {
      exceptionHandler.handle(e);
    }
    return table;
  }

  @Override
  public OrderedVersionedColumnarTable openTable(byte[] tableName) throws OperationException {
    try {
      if (this.admin.tableExists(tableName)) {
        return new HBaseNativeOVCTable(this.conf, tableName, FAMILY,
            new HBaseIOExceptionHandler());
      }
    } catch (IOException e) {
      exceptionHandler.handle(e);
    }
    return null;
  }

  protected HTable createTable(byte [] tableName, byte [] family)
      throws IOException {
    if (this.admin.tableExists(tableName)) {
      Log.debug("Attempt to creating table '" + tableName + "', " +
          "which already exists. Opening existing table instead.");
      return new HTable(this.conf, tableName);
    }
    HTableDescriptor htd = new HTableDescriptor(tableName);
    HColumnDescriptor hcd = new HColumnDescriptor(family);
    htd.addFamily(hcd);
    try {
      Log.info("Creating table '" + new String(tableName) + "'");
      this.admin.createTable(htd);
    } catch (TableExistsException e) {
      // table may exist because someone else is creating it at the same
      // time. But it may not be available yet, and opening it might fail.
      Log.info("Creating table '" + new String(tableName) + "' failed with: "
          + e.getMessage() + ".");
      // Wait at most 2 seconds for table to materialize
      long waitAtMost = 5000;
      long giveUpTime = System.currentTimeMillis() + waitAtMost;
      boolean exists = false;
      while (!exists && System.currentTimeMillis() < giveUpTime) {
        if (this.admin.tableExists(tableName)) {
          exists = true;
        } else {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e1) {
            Log.error("Thread interrupted: " + e1.getMessage(), e1);
            break;
          }
        }
      }
      if (exists) {
        Log.info("Table '" + new String(tableName) + "' exists now. Assuming " +
            "that another process concurrently created it. ");
      } else {
        Log.error("Table '" + new String(tableName) + "' does not exist after" +
            " waiting " + waitAtMost + " ms. Giving up. ");
        throw e;
      }
    }
    return new HTable(this.conf, tableName);
  }

  @Override
  public String getName() {
    return "hbase";
  }

  public static class HBaseIOExceptionHandler implements IOExceptionHandler {
    @Override
    public void handle(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
