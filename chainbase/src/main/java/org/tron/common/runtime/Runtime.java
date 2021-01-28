package org.bok.common.runtime;

import org.bok.core.db.TransactionContext;
import org.bok.core.exception.ContractExeException;
import org.bok.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}
