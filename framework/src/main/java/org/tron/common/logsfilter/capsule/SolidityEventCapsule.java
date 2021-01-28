package org.bok.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.bok.common.logsfilter.EventPluginLoader;
import org.bok.common.logsfilter.trigger.ContractEventTrigger;

public class SolidityEventCapsule extends TriggerCapsule {
  @Getter
  @Setter
  private ContractEventTrigger solidityEventTrigger;

  public SolidityEventCapsule(ContractEventTrigger solidityEventTrigger) {
    this.solidityEventTrigger = solidityEventTrigger;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postSolidityEventTrigger(solidityEventTrigger);
  }
}