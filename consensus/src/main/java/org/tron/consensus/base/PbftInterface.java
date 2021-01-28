package org.bok.consensus.base;

import org.bok.consensus.pbft.message.PbftBaseMessage;
import org.bok.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}