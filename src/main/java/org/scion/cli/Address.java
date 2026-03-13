// Copyright 2023 ETH Zurich
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

package org.scion.cli;

import static org.scion.cli.util.Util.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.scion.cli.util.ExitCodeException;
import org.scion.jpan.*;
import org.scion.jpan.internal.IPHelper;

/**
 * This demo mimics the "scion ping" command available in scionproto (<a
 * href="https://github.com/scionproto/scion">...</a>). This demo also demonstrates different ways
 * of connecting to a network: <br>
 * - JUNIT_MOCK shows how to use the mock network in this library (for JUnit tests) <br>
 * - SCION_PROTO shows how to connect to a local topology from the scionproto go implementation such
 * as "tiny". Note that the constants for "minimal" differ somewhat from the scionproto topology.
 * <br>
 * - PRODUCTION shows different ways how to connect to the production network. Note: While the
 * production network uses the dispatcher, the demo needs to use port 30041.
 *
 * <p>Commented out lines show alternative ways to connect or alternative destinations.
 */
public class Address {

  private static long localIsdAs = 0;
  private static InetAddress localIP = null;
  private static InetSocketAddress daemon;

  public static void main(String... args) {
    handleExit(() -> run(args));
  }

  public static void run(String... args) throws IOException {
    parseArgs(args);
    if (daemon != null) {
      System.setProperty(Constants.PROPERTY_DAEMON, daemon.toString());
    }
    try {
      run();
    } finally {
      Scion.closeDefault();
    }
  }

  private static void parseArgs(String[] argsArray) {
    List<String> args = new ArrayList<>(Arrays.asList(argsArray));
    while (!args.isEmpty()) {
      switch (args.get(0)) {
        case "-h":
        case "--help":
          Cli.printUsageShowpaths();
          throw new ExitCodeException(0);
        case "--isd-as":
          localIsdAs =
              tryParse("isd-as", args.get(1), () -> ScionUtil.parseIA(parseString("isd-as", args)));
          break;
        case "-l":
        case "--local":
          localIP = parseIP("local", args);
          break;
        case "--sciond":
          daemon = parseAddress("sciond", args);
          break;
        default:
          throw new ExitCodeException(2, "Unknown option: " + args.get(0));
      }
      args.remove(0);
    }
  }

  public static void run() {
    ScionService service = Scion.defaultService();
    Iterator<InetAddress> iter = IPHelper.getInterfaceIPs().iterator();
    boolean found = false;
    while (iter.hasNext()) {
      InetAddress ip = iter.next();
      if (ip.isLoopbackAddress()) {
        continue;
      }
      found = true;
      String localIP = ip.getHostAddress();
      if (localIP.contains("%")) {
        localIP = localIP.substring(0, localIP.indexOf('%'));
      }
      if (localIP.contains(":")) {
        localIP = "[" + localIP + "]";
      }
      String isdAs = ScionUtil.toStringIA(service.getLocalIsdAs());
      println(isdAs + "," + localIP);
    }
    if (!found) {
      throw new ExitCodeException(1, "No local interfaces detected.");
    }
  }
}
