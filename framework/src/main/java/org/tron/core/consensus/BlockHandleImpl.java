package org.bok.core.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.common.backup.BackupManager;
import org.bok.common.backup.BackupManager.BackupStatusEnum;
import org.bok.consensus.Consensus;
import org.bok.consensus.base.BlockHandle;
import org.bok.consensus.base.Param.Miner;
import org.bok.consensus.base.State;
import org.bok.core.capsule.BlockCapsule;
import org.bok.core.db.Manager;
import org.bok.core.net.UnNetService;
import org.bok.core.net.message.BlockMessage;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private UnNetService unNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    if (blockCapsule == null) {
      return null;
    }
    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      unNetService.fastForward(blockMessage);
      manager.pushBlock(blockCapsule);
      unNetService.broadcast(blockMessage);
    } catch (Exception e) {
      logger.error("Handle block {} failed.", blockCapsule.getBlockId().getString(), e);
      return null;
    }
    return blockCapsule;
  }
}
