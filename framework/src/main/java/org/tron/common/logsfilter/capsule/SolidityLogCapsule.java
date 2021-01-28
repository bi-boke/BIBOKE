package org.bok.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.bok.common.logsfilter.EventPluginLoader;
import org.bok.common.logsfilter.trigger.ContractLogTrigger;

public class SolidityLogCapsule extends TriggerCapsule {
  @Getter
  @Setter
  private ContractLogTrigger solidityLogTrigger;

  public SolidityLogCapsule(ContractLogTrigger solidityLogTrigger) {
    this.solidityLogTrigger = solidityLogTrigger;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postSolidityLogTrigger(solidityLogTrigger);
  }
}