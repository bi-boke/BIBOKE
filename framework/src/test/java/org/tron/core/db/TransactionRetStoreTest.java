package org.bok.core.db;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.bok.common.application.UnApplicationContext;
import org.bok.common.utils.ByteArray;
import org.bok.common.utils.FileUtil;
import org.bok.core.Constant;
import org.bok.core.capsule.TransactionCapsule;
import org.bok.core.capsule.TransactionInfoCapsule;
import org.bok.core.capsule.TransactionRetCapsule;
import org.bok.core.config.DefaultConfig;
import org.bok.core.config.args.Args;
import org.bok.core.exception.BadItemException;
import org.bok.core.store.TransactionRetStore;
import org.bok.protos.Protocol.Transaction;

public class TransactionRetStoreTest {

  private static final byte[] transactionId = TransactionStoreTest.randomBytes(32);
  private static final byte[] blockNum = ByteArray.fromLong(1);
  private static String dbPath = "output_TransactionRetStore_test";
  private static String dbDirectory = "db_TransactionRetStore_test";
  private static String indexDirectory = "index_TransactionRetStore_test";
  private static UnApplicationContext context;
  private static TransactionRetStore transactionRetStore;
  private static Transaction transaction;
  private static TransactionStore transactionStore;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--storage-db-directory", dbDirectory,
        "--storage-index-directory", indexDirectory}, Constant.TEST_CONF);
    context = new UnApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    transactionRetStore = context.getBean(TransactionRetStore.class);
    transactionStore = context.getBean(TransactionStore.class);
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();

    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    transactionRetStore.put(blockNum, transactionRetCapsule);
    transaction = Transaction.newBuilder().build();
    TransactionCapsule transactionCapsule = new TransactionCapsule(transaction);
    transactionCapsule.setBlockNum(1);
    transactionStore.put(transactionId, transactionCapsule);
  }

  @Test
  public void get() throws BadItemException {
    TransactionInfoCapsule resultCapsule = transactionRetStore.getTransactionInfo(transactionId);
    Assert.assertNotNull("get transaction ret store", resultCapsule);
  }

  @Test
  public void put() {
    TransactionInfoCapsule transactionInfoCapsule = new TransactionInfoCapsule();
    transactionInfoCapsule.setId(transactionId);
    transactionInfoCapsule.setFee(1000L);
    transactionInfoCapsule.setBlockNumber(100L);
    transactionInfoCapsule.setBlockTimeStamp(200L);

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule();
    transactionRetCapsule.addTransactionInfo(transactionInfoCapsule.getInstance());
    Assert.assertNull("put transaction info error",
        transactionRetStore.getUnchecked(transactionInfoCapsule.getId()));
    transactionRetStore.put(transactionInfoCapsule.getId(), transactionRetCapsule);
    Assert.assertNotNull("get transaction info error",
        transactionRetStore.getUnchecked(transactionInfoCapsule.getId()));
  }
}