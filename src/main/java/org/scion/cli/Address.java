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
import java.nio.channels.DatagramChannel;
import java.util.*;
import org.scion.cli.util.CliScionService;
import org.scion.cli.util.Errors;
import org.scion.cli.util.ExitCodeException;
import org.scion.jpan.*;
import org.scion.jpan.internal.bootstrap.LocalAS;
import org.scion.jpan.internal.util.IPHelper;

/**
 * This demo mimics the "scion address" command available in scionproto (<a
 * href="https://github.com/scionproto/scion">...</a>).
 */
public class Address {

  private long localIsdAs = 0;
  private InetAddress localIP = null;
  private InetSocketAddress daemon;

  public static void main(String... args) {
    handleExit(() -> new Address().run(args));
  }

  public void run(String... args) {
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

  private void parseArgs(String[] argsArray) {
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
        case "--log.level":
          parseAndSetLogLevel(args);
          break;
        case "--sciond":
          daemon = parseAddress("sciond", args);
          break;
        default:
          throw new ExitCodeException(2, Errors.UNKNOWN_OPTION + args.get(0));
      }
      args.remove(0);
    }
  }

  public static void run() {
    HashSet<InetAddress> localIPs = new HashSet<>();
    CliScionService cli = CliScionService.defaultService();
    LocalAS localAs = cli.getLocalAS();
    if (localAs.getIsdAses().isEmpty()) {
      throw new ExitCodeException(1, "No AS detected.");
    }
    if (localAs.getControlServices().isEmpty()) {
      throw new ExitCodeException(1, "No control services detected.");
    }

    for (LocalAS.ServiceNode node : localAs.getControlServices()) {
      try (DatagramChannel dc = DatagramChannel.open()) {
        InetSocketAddress addr = IPHelper.toInetSocketAddress(node.getIpString());
        dc.connect(addr);
        InetSocketAddress localIP = (InetSocketAddress) dc.getLocalAddress();
        localIPs.add(localIP.getAddress());
      } catch (IOException e) {
        throw new ExitCodeException(1, "Error while determining local address: " + e.getMessage());
      }
    }

    boolean found = false;
    for (long isdAs : localAs.getIsdAses()) {
      for (InetAddress ip : localIPs) {
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
        String isdAsString = ScionUtil.toStringIA(isdAs);
        println(isdAsString + "," + localIP);
      }
      if (!found) {
        throw new ExitCodeException(1, "No local interfaces detected.");
      }
    }
  }
}
