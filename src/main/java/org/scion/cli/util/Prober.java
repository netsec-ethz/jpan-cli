// Copyright 2026 ETH Zurich
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.scion.cli.util;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.scion.jpan.*;

public class Prober {

  public enum Status {
    // StatusUnknown indicates that it is not clear what state the path is in.
    Unknown,
    // StatusTimeout indicates that a reply did come back in time for the path.
    Timeout,
    // StatusAlive indicates that the expected reply did come back in time.
    Alive,
    // StatusSCMP indicates that an unexpected SCMP packet came in the reply.
    SCMP;
  }

  private Prober() {}

  public static Map<Integer, Status> probe(int port, int timeoutMs, List<Path> paths) {
    PingResponseHandler handler = new PingResponseHandler(paths.size());

    // Send all requests
    try (ScmpSenderAsync sender = Scmp.newSenderAsyncBuilder(handler).setLocalPort(port).build()) {
      sender.setTimeOut(timeoutMs);
      for (Path path : paths) {
        sender.sendTracerouteLast(path);
      }

      // Wait for all messages to be received, BEFORE closing the "sender".
      handler.await();
    } catch (IOException e) {
      throw new ExitCodeException(2, e.getMessage());
    }

    if (handler.result.size() < paths.size()) {
      throw new ExitCodeException(
          2, "Could not probe all paths: " + handler.result.size() + "/" + paths.size());
    }
    return handler.result;
  }

  private static class PingResponseHandler implements ScmpSenderAsync.ResponseHandler {
    private final Map<Integer, Status> result = new ConcurrentHashMap<>();
    private final CountDownLatch barrier;
    private final AtomicInteger errors = new AtomicInteger();
    private final int nPaths;

    private PingResponseHandler(int nPaths) {
      this.nPaths = nPaths;
      barrier = new CountDownLatch(nPaths);
    }

    @Override
    public void onResponse(Scmp.TimedMessage msg) {
      barrier.countDown();
      result.put(msg.getSequenceNumber(), Status.Alive);
    }

    @Override
    public void onTimeout(Scmp.TimedMessage msg) {
      barrier.countDown();
      result.put(msg.getSequenceNumber(), Status.Timeout);
    }

    @Override
    public void onError(Scmp.ErrorMessage msg) {
      errors.incrementAndGet();
      barrier.countDown();
      result.put(msg.getSequenceNumber(), Status.SCMP);
    }

    @Override
    public void onException(Throwable t) {
      errors.incrementAndGet();
      barrier.countDown();
    }

    void await() {
      try {
        if (!barrier.await(1100, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException("Missing messages: " + barrier.getCount() + "/" + nPaths);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }

    public boolean hasErrors() {
      return errors.get() > 0;
    }
  }
}
