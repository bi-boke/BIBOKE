package org.bok.core.exception;

public class DupTransactionException extends UnException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
