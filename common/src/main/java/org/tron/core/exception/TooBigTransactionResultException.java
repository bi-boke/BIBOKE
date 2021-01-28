package org.bok.core.exception;

public class TooBigTransactionResultException extends UnException {

  public TooBigTransactionResultException() {
    super("too big transaction result");
  }

  public TooBigTransactionResultException(String message) {
    super(message);
  }
}
