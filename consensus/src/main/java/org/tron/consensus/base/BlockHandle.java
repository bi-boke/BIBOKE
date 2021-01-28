package org.bok.consensus.base;

import org.bok.consensus.base.Param.Miner;
import org.bok.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}