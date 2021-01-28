package org.bok.core.net.messagehandler;

import static org.bok.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.common.overlay.discover.node.statistics.MessageCount;
import org.bok.common.overlay.message.Message;
import org.bok.common.utils.Sha256Hash;
import org.bok.core.capsule.BlockCapsule.BlockId;
import org.bok.core.config.Parameter.NetConstants;
import org.bok.core.exception.P2pException;
import org.bok.core.exception.P2pException.TypeEnum;
import org.bok.core.net.UnNetDelegate;
import org.bok.core.net.message.BlockMessage;
import org.bok.core.net.message.FetchInvDataMessage;
import org.bok.core.net.message.MessageTypes;
import org.bok.core.net.message.TransactionMessage;
import org.bok.core.net.message.TransactionsMessage;
import org.bok.core.net.message.UnMessage;
import org.bok.core.net.peer.Item;
import org.bok.core.net.peer.PeerConnection;
import org.bok.core.net.service.AdvService;
import org.bok.core.net.service.SyncService;
import org.bok.protos.Protocol.Inventory.InventoryType;
import org.bok.protos.Protocol.ReasonCode;
import org.bok.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements UnMsgHandler {

  private static final int MAX_SIZE = 1_000_000;
  @Autowired
  private UnNetDelegate unNetDelegate;
  @Autowired
  private SyncService syncService;
  @Autowired
  private AdvService advService;

  @Override
  public void processMessage(PeerConnection peer, UnMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = unNetDelegate.getData(hash, type);
        } catch (Exception e) {
          logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type == InventoryType.BLOCK) {
        BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance()
            .getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (!transactions.isEmpty()) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.UN) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.UN)) == null) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.unInUnFetchInvDataElement
          .getCount(10);
      int maxCount = advService.getUnCount().getCount(60);
      if (fetchCount > maxCount) {
        logger.error("maxCount: " + maxCount + ", fetchCount: " + fetchCount);
        //        throw new P2pException(TypeEnum.BAD_MESSAGE,
        //            "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount unOutAdvBlock = peer.getNodeStatistics().messageStatistics.unOutAdvBlock;
        unOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = unOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          logger.error("producedBlockIn2min: " + producedBlockIn2min + ", outBlockCountIn1min: "
              + outBlockCountIn1min);
          //throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: "
          // + producedBlockIn2min
          //  + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * NetConstants.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                new BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}
