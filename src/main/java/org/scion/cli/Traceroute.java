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

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.scion.cli.util.ExitCodeException;
import org.scion.jpan.*;
import org.scion.jpan.internal.ScionAddress;

import static org.scion.cli.util.Util.*;

/**
 * This demo mimics the "scion traceroute" command available in scionproto (<a
 * href="https://github.com/scionproto/scion">...</a>).
 */
public class Traceroute {

  private static Integer localPort;
  private static boolean startShim = false;
  private static long localIsdAs = 0;
  private static InetAddress localIP = null;
  private static ScionAddress dstAddress;
  private static String dstUrl;
  private static InetSocketAddress daemon;
  private static int timeoutMs = 1000;

  public static void main(String... args) {
    handleExit(() -> run(args));
  }

  public static void run(String... args) throws IOException {
    parseArgs(args);
    System.setProperty(Constants.PROPERTY_SHIM, startShim ? "true" : "false"); // disable SHIM
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
          Cli.printUsagePing();
          throw new ExitCodeException(0);
        case "--isd-as":
          localIsdAs =
                  tryParse("isd-as", args.get(1), () -> ScionUtil.parseIA(parseString("isd-as", args)));
          break;
        case "-l":
        case "--local":
          localIP = parseIP("local", args);
          break;
        case "--port":
          localPort = parseInt("port", args);
          break;
        case "--shim":
          startShim = true;
          break;
        case "--sciond":
          daemon = parseAddress("sciond", args);
          break;
        case "--timeout":
          timeoutMs = parseInt("timeout", args);
          break;
        case "--url":
          dstUrl = parseString("url", args);
          break;
        default:
          if (dstAddress == null) {
            dstAddress = parseScionAddress(args);
            if (dstAddress != null) {
              continue;
            }
          }
          throw new ExitCodeException(2, "Unknown option: " + args.get(0));
      }
      args.remove(0);
    }
    if (dstAddress == null) {
      throw new ExitCodeException(2, "Please provide a destination address.");
    }
  }

  public static void run() throws IOException {
    ScionService service = Scion.defaultService();

    List<Path> paths;
    if (dstUrl != null) {
      paths = service.lookupPaths(dstUrl, Constants.SCMP_PORT);
    } else if (dstAddress != null) {
      paths = service.getPaths(dstAddress.getIsdAs(), dstAddress.getInetAddress(), Constants.SCMP_PORT);
    } else {
      throw new ExitCodeException(2, "Error: missing address or --url");
    }

    if (paths.isEmpty()) {
      String src = ScionUtil.toStringIA(service.getLocalIsdAs());
      String dst = ScionUtil.toStringIA(dstAddress.getIsdAs());
      throw new IOException("No path found from " + src + " to " + dst);
    }
    Path path = paths.get(0);

    String localAddress;
    try (ScionDatagramChannel channel = ScionDatagramChannel.open()) {
      channel.connect(path);
      // We determine the address separately because SCMP will always have 0.0.0.0 as local address
      localAddress = channel.getLocalAddress().getAddress().getHostAddress();
    }

    ScmpSender.Builder builder = ScmpSender.newBuilder();
    if (localPort != null) {
      builder.setLocalPort(localPort);
    }
    try (ScmpSender sender = builder.build()) {
      sender.setTimeOut(timeoutMs);
      println("Listening on port " + sender.getLocalAddress().getPort() + " ...");
      println("Resolved local address: ");
      println("  " + localAddress);
      printPath(path);

      List<Scmp.TracerouteMessage> results = sender.sendTracerouteRequest(path);
      int n = 0;
      for (Scmp.TracerouteMessage msg : results) {
        if (!msg.isTimedOut()) {
          n++;
        }
        String millis = String.format("%.4f", msg.getNanoSeconds() / (double) 1_000_000);
        String out = "" + msg.getSequenceNumber();
        out += " " + ScionUtil.toStringIA(msg.getIsdAs());
        out += " " + msg.getPath().getRemoteAddress().getHostAddress();
        out += " IfID=" + msg.getIfID();
        out += " " + millis + "ms";
        println(out);
      }
      if (n > 0) {
        throw new ExitCodeException(1, "Number of timeouts: " + n);
      }
    }
  }

  private static void printPath(Path path) {
    String nl = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    // sb.append("Actual local address:").append(nl);
    // sb.append("  ").append(channel.getLocalAddress().getAddress().getHostAddress()).append(nl);
    sb.append("Using path:").append(nl);
    sb.append("  Hops: ").append(ScionUtil.toStringPath(path.getMetadata()));
    sb.append(" MTU: ").append(path.getMetadata().getMtu());
    sb.append(" NextHop: ").append(path.getMetadata().getInterface().getAddress()).append(nl);
    println(sb.toString());
  }
}
