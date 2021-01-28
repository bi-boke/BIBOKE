package org.bok.core.exception;

public class VMMemoryOverflowException extends UnException {

  public VMMemoryOverflowException() {
    super("VM memory overflow");
  }

  public VMMemoryOverflowException(String message) {
    super(message);
  }

}
