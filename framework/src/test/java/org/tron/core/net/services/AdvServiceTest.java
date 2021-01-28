package org.bok.core.net.services;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.bok.common.application.UnApplicationContext;
import org.bok.common.overlay.server.SyncPool;
import org.bok.common.parameter.CommonParameter;
import org.bok.common.utils.ReflectUtils;
import org.bok.common.utils.Sha256Hash;
import org.bok.core.Constant;
import org.bok.core.capsule.BlockCapsule;
import org.bok.core.config.DefaultConfig;
import org.bok.core.config.args.Args;
import org.bok.core.net.message.BlockMessage;
import org.bok.core.net.message.TransactionMessage;
import org.bok.core.net.peer.Item;
import org.bok.core.net.peer.PeerConnection;
import org.bok.core.net.service.AdvService;
import org.bok.protos.Protocol;
import org.bok.protos.Protocol.Inventory.InventoryType;

//@Ignore
public class AdvServiceTest {

  protected UnApplicationContext context;
  private AdvService service;
  private PeerConnection peer;
  private SyncPool syncPool;

  /**
   * init context.
   */
  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
    context = new UnApplicationContext(DefaultConfig.class);
    service = context.getBean(AdvService.class);
  }

  /**
   * destroy.
   */
  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
  }

  @Test
  public void test() {
    testAddInv();
    testBroadcast();
    testFastSend();
    testUnBroadcast();
  }

  private void testAddInv() {
    boolean flag;
    Item itemUn = new Item(Sha256Hash.ZERO_HASH, InventoryType.UN);
    flag = service.addInv(itemUn);
    Assert.assertTrue(flag);
    flag = service.addInv(itemUn);
    Assert.assertFalse(flag);

    Item itemBlock = new Item(Sha256Hash.ZERO_HASH, InventoryType.BLOCK);
    flag = service.addInv(itemBlock);
    Assert.assertTrue(flag);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);

    service.addInvToCache(itemBlock);
    flag = service.addInv(itemBlock);
    Assert.assertFalse(flag);
  }

  private void testBroadcast() {

    try {
      peer = context.getBean(PeerConnection.class);
      syncPool = context.getBean(SyncPool.class);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(syncPool, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.broadcast(msg);
      Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
      Assert.assertNotNull(service.getMessage(item));

      peer.close();
      syncPool.close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void testFastSend() {

    try {
      peer = context.getBean(PeerConnection.class);
      syncPool = context.getBean(SyncPool.class);

      List<PeerConnection> peers = Lists.newArrayList();
      peers.add(peer);
      ReflectUtils.setFieldValue(syncPool, "activePeers", peers);
      BlockCapsule blockCapsule = new BlockCapsule(1, Sha256Hash.ZERO_HASH,
          System.currentTimeMillis(), Sha256Hash.ZERO_HASH.getByteString());
      BlockMessage msg = new BlockMessage(blockCapsule);
      service.fastForward(msg);
      Item item = new Item(blockCapsule.getBlockId(), InventoryType.BLOCK);
      //Assert.assertNull(service.getMessage(item));

      peer.getAdvInvRequest().put(item, System.currentTimeMillis());
      service.onDisconnect(peer);

      peer.close();
      syncPool.close();
    } catch (NullPointerException e) {
      System.out.println(e);
    }
  }

  private void testUnBroadcast() {
    Protocol.Transaction trx = Protocol.Transaction.newBuilder().build();
    CommonParameter.getInstance().setValidContractProtoThreadNum(1);
    TransactionMessage msg = new TransactionMessage(trx);
    service.broadcast(msg);
    Item item = new Item(msg.getMessageId(), InventoryType.UN);
    Assert.assertNotNull(service.getMessage(item));
  }

}
