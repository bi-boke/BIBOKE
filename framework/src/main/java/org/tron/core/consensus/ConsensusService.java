package org.bok.core.consensus;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bok.common.crypto.SignUtils;
import org.bok.common.parameter.CommonParameter;
import org.bok.common.utils.ByteArray;
import org.bok.consensus.Consensus;
import org.bok.consensus.base.Param;
import org.bok.consensus.base.Param.Miner;
import org.bok.core.capsule.WitnessCapsule;
import org.bok.core.config.args.Args;
import org.bok.core.store.WitnessStore;

@Slf4j(topic = "consensus")
@Component
public class ConsensusService {

  @Autowired
  private Consensus consensus;

  @Autowired
  private WitnessStore witnessStore;

  @Autowired
  private BlockHandleImpl blockHandle;

  @Autowired
  private PbftBaseImpl pbftBaseImpl;

  private CommonParameter parameter = Args.getInstance();

  public void start() {
    Param param = Param.getInstance();
    param.setEnable(parameter.isWitness());
    param.setGenesisBlock(parameter.getGenesisBlock());
    param.setMinParticipationRate(parameter.getMinParticipationRate());
    param.setBlockProduceTimeoutPercent(Args.getInstance().getBlockProducedTimeOut());
    param.setNeedSyncCheck(parameter.isNeedSyncCheck());
    param.setAgreeNodeCount(parameter.getAgreeNodeCount());
    List<Miner> miners = new ArrayList<>();
    byte[] privateKey = ByteArray
        .fromHexString(Args.getLocalWitnesses().getPrivateKey());
    byte[] privateKeyAddress = SignUtils.fromPrivate(privateKey,
        Args.getInstance().isECKeyCryptoEngine()).getAddress();
    byte[] witnessAddress = Args.getLocalWitnesses().getWitnessAccountAddress(Args
        .getInstance().isECKeyCryptoEngine());
    WitnessCapsule witnessCapsule = witnessStore.get(witnessAddress);
    if (null == witnessCapsule) {
      logger.warn("Witness {} is not in witnessStore.", Hex.encodeHexString(witnessAddress));
    } else {
      Miner miner = param.new Miner(privateKey, ByteString.copyFrom(privateKeyAddress),
          ByteString.copyFrom(witnessAddress));
      miners.add(miner);
    }
    param.setMiners(miners);
    param.setBlockHandle(blockHandle);
    param.setPbftInterface(pbftBaseImpl);
    consensus.start(param);
    logger.info("consensus service start success");
  }

  public void stop() {
    consensus.stop();
  }

}
