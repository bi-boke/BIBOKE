package org.bok.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
