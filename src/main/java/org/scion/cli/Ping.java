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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.scion.cli.util.Util;
import org.scion.jpan.*;
import org.scion.jpan.internal.util.IPHelper;

import static org.scion.cli.util.Util.*;

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
public class Ping {

  static int localPort = Constants.SCMP_PORT;
  private static int count = 10;
  private static int intervalMs = 1000;
  private static boolean startShim = false;
  private static long localIsdAs = 0;
  private static InetAddress localIP = null;
  private static int payloadSize = 0;

  public static void main(String... args) throws IOException {
    parseArgs(args);
    System.setProperty(Constants.PROPERTY_SHIM, startShim ? "true" : "false"); // disable SHIM
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
        case "-c":
        case "--count":
          count = parseInt("count", args);
          break;
        case "--interval":
          intervalMs = parseInt("interval", args);
          break;
        case "--isd-as":
          localIsdAs =
              tryParse("isd-as", args.get(1), () -> ScionUtil.parseIA(parseString("isd-as", args)));
          break;
        case "--local":
          localIP =
              tryParse(
                  "local", args.get(1), () -> IPHelper.toInetAddress(parseString("local", args)));
          break;
        case "-s":
        case "--payload-size":
          payloadSize = parseInt("payload-size", args);
          break;
        case "--help":
          Cli.printUsagePing();
          System.exit(0);
          break;
        case "--shim":
          startShim = true;
          break;
        case "--port":
          localPort = parseInt("port", args);
          break;
        default:
          Util.println("Unknown option: " + args.get(0));
          Cli.printUsagePing();
          System.exit(1);
      }
      args.remove(0);
    }
  }

  public static int run() throws IOException {
    ScionService service = Scion.defaultService();
    return runDemo(service.lookupPaths("ethz.ch", Constants.SCMP_PORT));
  }

  private static int runDemo(List<Path> paths) throws IOException {
    Path path = paths.get(0);
    ByteBuffer data = ByteBuffer.allocate(0);

    String localAddress;
    try (ScionDatagramChannel channel = ScionDatagramChannel.open()) {
      channel.connect(path);
      // We determine the address separately because SCMP will always have 0.0.0.0 as local address
      localAddress = channel.getLocalAddress().getAddress().getHostAddress();
    }

    int n = 0;
    try (ScmpSender sender = Scmp.newSenderBuilder().build()) {
      println("Listening on port " + sender.getLocalAddress().getPort() + " ...");
      println("Resolved local address: ");
      println("  " + localAddress);
      printPath(path);

      for (int i = 0; i < count; i++) {
        Scmp.EchoMessage msg = sender.sendEchoRequest(path, data);
        if (i == 0) {
          printHeader(path.getRemoteSocketAddress(), data, msg);
        }
        String millis = String.format("%.3f", msg.getNanoSeconds() / (double) 1_000_000);
        String echoMsgStr = msg.getSizeReceived() + " bytes from ";
        InetAddress addr = msg.getPath().getRemoteAddress();
        echoMsgStr += ScionUtil.toStringIA(path.getRemoteIsdAs()) + "," + addr.getHostAddress();
        echoMsgStr += ": scmp_seq=" + msg.getSequenceNumber();
        if (msg.isTimedOut()) {
          echoMsgStr += " Timed out after";
        } else {
          n++;
        }
        echoMsgStr += " time=" + millis + "ms";
        println(echoMsgStr);
        if (i < count - 1) {
          Util.sleep(intervalMs);
        }
      }
    }
    return n;
  }

  private static void printPath(Path path) {
    String nl = System.lineSeparator();
    String sb = "Using path:" + nl + "  Hops: " + ScionUtil.toStringPath(path.getMetadata());
    sb += " MTU: " + path.getMetadata().getMtu();
    sb += " NextHop: " + path.getMetadata().getLocalInterface().getAddress() + nl;
    println(sb);
  }

  private static void printHeader(ScionSocketAddress dst, ByteBuffer data, Scmp.EchoMessage msg) {
    String sb = "PING " + ScionUtil.toStringIA(dst.getIsdAs()) + ",";
    sb += dst.getHostString() + ":" + dst.getPort() + " pld=" + data.remaining();
    sb += "B scion_pkt=" + msg.getSizeSent() + "B";
    println(sb);
  }
}
