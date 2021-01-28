package org.bok.core.exception;

public class TooBigTransactionException extends UnException {

  public TooBigTransactionException() {
    super();
  }

  public TooBigTransactionException(String message) {
    super(message);
  }
}
