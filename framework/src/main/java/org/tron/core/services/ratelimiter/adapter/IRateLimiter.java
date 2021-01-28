package org.bok.core.services.ratelimiter.adapter;

import org.bok.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}
