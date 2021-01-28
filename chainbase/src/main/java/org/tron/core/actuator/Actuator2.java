package org.bok.core.actuator;

import org.bok.core.exception.ContractExeException;
import org.bok.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}