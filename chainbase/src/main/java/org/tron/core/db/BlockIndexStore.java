package org.bok.core.db;

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bok.common.utils.ByteArray;
import org.bok.common.utils.Sha256Hash;
import org.bok.core.capsule.BlockCapsule.BlockId;
import org.bok.core.capsule.BytesCapsule;
import org.bok.core.exception.ItemNotFoundException;

@Component
public class BlockIndexStore extends UnStoreWithRevoking<BytesCapsule> {


  @Autowired
  public BlockIndexStore(@Value("block-index") String dbName) {
    super(dbName);

  }

  public void put(BlockId id) {
    put(ByteArray.fromLong(id.getNum()), new BytesCapsule(id.getBytes()));
  }

  public BlockId get(Long num)
      throws ItemNotFoundException {
    BytesCapsule value = getUnchecked(ByteArray.fromLong(num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException("number: " + num + " is not found!");
    }
    return new BlockId(Sha256Hash.wrap(value.getData()), num);
  }

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException("number: " + Arrays.toString(key) + " is not found!");
    }
    return new BytesCapsule(value);
  }
}