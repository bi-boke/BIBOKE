package org.bok.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.consensus.base.ConsensusInterface;
import org.bok.consensus.base.Param;
import org.bok.consensus.dpos.DposService;
import org.bok.core.capsule.BlockCapsule;

@Slf4j(topic = "consensus")
@Component
public class Consensus {

  @Autowired
  private DposService dposService;

  private ConsensusInterface consensusInterface;

  public void start(Param param) {
    consensusInterface = dposService;
    consensusInterface.start(param);
  }

  public void stop() {
    consensusInterface = dposService;
    consensusInterface.stop();
  }

  public void receiveBlock(BlockCapsule blockCapsule) {
    consensusInterface.receiveBlock(blockCapsule);
  }

  public boolean validBlock(BlockCapsule blockCapsule) {
    return consensusInterface.validBlock(blockCapsule);
  }

  public boolean applyBlock(BlockCapsule blockCapsule) {
    return consensusInterface.applyBlock(blockCapsule);
  }

}