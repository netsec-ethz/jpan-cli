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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.scion.cli.util.Errors;
import org.scion.cli.util.ExitCodeException;
import org.scion.cli.util.Util;
import org.scion.jpan.*;
import org.scion.jpan.internal.ScionAddress;

/**
 * This demo mimics the "scion ping" command available in scionproto (<a
 * href="https://github.com/scionproto/scion">...</a>).
 */
public class Ping {

  private Integer localPort = -1;
  private int count = 10;
  private int intervalMs = 1000;
  private boolean startShim = false;
  private long localIsdAs = 0;
  private InetAddress localIP = null;
  private int payloadSize = 0;
  private ScionAddress dstAddress;
  private String dstUrl;
  private InetSocketAddress daemon;
  private int timeoutMs = 1000;

  public static void main(String... args) {
    handleExit(() -> new Ping().run(args));
  }

  public void run(String... args) throws IOException {
    parseArgs(args);
    System.setProperty(Constants.PROPERTY_SHIM, startShim ? "true" : "false"); // disable SHIM
    if (daemon != null) {
      System.setProperty(Constants.PROPERTY_DAEMON, daemon.toString());
    }
    if (dstUrl == null && dstAddress == null) {
      throw new ExitCodeException(2, "Error: missing address or --url");
    }
    try {
      ScionService service = Scion.defaultService();
      if (dstUrl != null) {
        run(service.lookupPaths(dstUrl, Constants.SCMP_PORT));
      } else {
        run(
            service.getPaths(
                dstAddress.getIsdAs(), dstAddress.getInetAddress(), Constants.SCMP_PORT));
      }
    } finally {
      Scion.closeDefault();
    }
  }

  private void parseArgs(String[] argsArray) {
    List<String> args = new ArrayList<>(Arrays.asList(argsArray));
    while (!args.isEmpty()) {
      if (!args.get(0).startsWith("-")) {
        if (dstAddress == null) {
          dstAddress = parseScionAddress(args);
          continue;
        }
        throw new ExitCodeException(2, Errors.UNEXPECTED_NON_FLAG + args.get(0));
      }
      switch (args.get(0)) {
        case "-c":
        case "--count":
          count = parseInt("count", args);
          break;
        case "-h":
        case "--help":
          Cli.printUsagePing();
          throw new ExitCodeException(0);
        case "--interval":
          intervalMs = parseInt("interval", args);
          break;
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
        case "--port":
          localPort = parseInt("port", args);
          break;
        case "-s":
        case "--payload-size":
          payloadSize = parseInt("payload-size", args);
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
          throw new ExitCodeException(2, Errors.UNKNOWN_OPTION + args.get(0));
      }
      args.remove(0);
    }
  }

  private void run(List<Path> paths) throws IOException {
    Path path = paths.get(0);
    ByteBuffer data = ByteBuffer.allocate(payloadSize);
    for (int i = 0; i < payloadSize; i++) {
      data.put((byte) i);
    }

    String localAddress;
    try (ScionDatagramChannel channel = ScionDatagramChannel.open()) {
      channel.connect(path);
      // We determine the address separately because SCMP will always have 0.0.0.0 as local address
      localAddress = channel.getLocalAddress().getAddress().getHostAddress();
    }

    int nTimeouts = 0;
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
          nTimeouts++;
        }
        echoMsgStr += " time=" + millis + "ms";
        println(echoMsgStr);
        if (i < count - 1) {
          Util.sleep(intervalMs);
        }
      }
    }
    if (nTimeouts > 0) {
      String msg = "";
      if (localPort == null || localPort != 30041) {
        msg = ". Try using \"--port 30041\"";
      }
      throw new ExitCodeException(1, "Number of timeouts: " + nTimeouts + msg);
    }
  }

  private static void printPath(Path path) {
    String nl = System.lineSeparator();
    String sb = "Using path:" + nl + "  Hops: " + ScionUtil.toStringPath(path.getMetadata());
    sb += " MTU: " + path.getMetadata().getMtu();
    sb += " NextHop: " + path.getMetadata().getInterface().getAddress() + nl;
    println(sb);
  }

  private static void printHeader(ScionSocketAddress dst, ByteBuffer data, Scmp.EchoMessage msg) {
    String sb = "PING " + ScionUtil.toStringIA(dst.getIsdAs()) + ",";
    sb += dst.getHostString() + ":" + dst.getPort() + " pld=" + data.remaining();
    sb += "B scion_pkt=" + msg.getSizeSent() + "B";
    println(sb);
  }
}
