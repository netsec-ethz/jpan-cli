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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.scion.cli.util.Errors;
import org.scion.cli.util.ExitCodeException;
import org.scion.cli.util.Prober;
import org.scion.jpan.*;

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
public class Showpaths {

  private long localIsdAs = 0;
  private InetAddress localIP = null;
  private InetSocketAddress daemon;
  private Long isdAs;
  private boolean extended = false;
  private int maxPaths = 10;
  private Integer port;
  private final int timeoutMs = 5000;
  private boolean probePath = true;

  public static void main(String... args) {
    handleExit(() -> new Showpaths().run(args));
  }

  public void run(String... args) throws IOException {
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
      if (!args.get(0).startsWith("-")) {
        if (isdAs == null) {
          isdAs = parseIsdAs(args);
          continue;
        }
        throw new ExitCodeException(2, Errors.UNEXPECTED_NON_FLAG + args.get(0));
      }

      switch (args.get(0)) {
        case "-e":
        case "--extended":
          extended = true;
          break;
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
        case "-m":
        case "--maxpaths":
          maxPaths = parseInt("--maxpaths", args);
          break;
        case "--no-probe":
          probePath = false;
          break;
        case "--sciond":
          daemon = parseAddress("sciond", args);
          break;
        default:
          throw new ExitCodeException(2, Errors.UNKNOWN_OPTION + args.get(0));
      }
      args.remove(0);
    }
    if (isdAs == null) {
      throw new ExitCodeException(2, "Please provide a destination ISD/AS.");
    }
  }

  public void run() throws IOException {
    ScionService service = Scion.defaultService();
    // dummy address
    InetSocketAddress destinationAddress =
        new InetSocketAddress(InetAddress.getLoopbackAddress(), 12345);
    List<Path> paths = service.getPaths(isdAs, destinationAddress);
    if (paths.isEmpty()) {
      String src = ScionUtil.toStringIA(service.getLocalIsdAs());
      String dst = ScionUtil.toStringIA(isdAs);
      throw new ExitCodeException(1, "No path found from " + src + " to " + dst);
    }

    Map<Integer, Prober.Status> isActive;
    if (probePath) {
      isActive = Prober.probe(port, timeoutMs, paths);
    } else {
      isActive = new HashMap<>();
    }

    println("Available paths to " + ScionUtil.toStringIA(isdAs));

    int id = 0;
    for (Path path : paths) {
      if (id >= maxPaths) {
        break;
      }
      String localIP;
      try (ScionDatagramChannel channel = ScionDatagramChannel.open()) {
        channel.connect(path);
        localIP = channel.getLocalAddress().getAddress().getHostAddress();
      }
      PathMetadata meta = path.getMetadata();
      String header = "[" + id++ + "] Hops: " + ScionUtil.toStringPath(meta);
      if (extended) {
        println(header);
        printExtended(path, localIP, isActive.getOrDefault(id, Prober.Status.Unknown));
      } else {
        String compact =
            " MTU: "
                + meta.getMtu()
                + " NextHop: "
                + path.getFirstHopAddress().getHostString()
                + ":"
                + path.getFirstHopAddress().getPort();
        if (probePath) {
          compact += " Status: " + isActive.getOrDefault(id, Prober.Status.Unknown);
        }
        compact += " LocalIP: " + localIP;
        println(header + compact);
      }
    }

    if (probePath && isActive.isEmpty()) {
      throw new ExitCodeException(2, "Error: no path alive");
    }
  }

  private static void printExtended(Path path, String localIP, Prober.Status status) {
    StringBuilder sb = new StringBuilder();
    String NL = System.lineSeparator();

    PathMetadata meta = path.getMetadata();
    sb.append("    MTU: ").append(meta.getMtu()).append(NL);
    sb.append("    NextHop: ").append(path.getFirstHopAddress().getHostString()).append(NL);
    sb.append("    Expires: ").append(toStringExpiry(meta)).append(NL);
    sb.append("    Latency: ").append(toStringLatency(meta)).append(NL);
    sb.append("    Bandwidth: ").append(toStringBandwidth(meta)).append(NL);
    sb.append("    Geo: ").append(toStringGeo(meta)).append(NL);
    sb.append("    LinkType: ").append(toStringLinkType(meta)).append(NL);
    sb.append("    Notes: ").append(toStringNotes(meta)).append(NL);
    sb.append("    SupportsEPIC: ").append(toStringEPIC(meta)).append(NL);
    sb.append("    Status: ").append(status).append(NL);
    // TODO use destination IP from returned packet from probe
    sb.append("    LocalIP: ").append(localIP).append(NL);

    println(sb.toString());
  }

  private static String toStringExpiry(PathMetadata meta) {
    Instant exp = Instant.ofEpochSecond(meta.getExpiration());
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z").withZone(ZoneId.of("UTC"));
    Instant now = Instant.now();
    long s = Duration.between(now, exp).getSeconds();
    String ds = String.format("(%dh%02dm%02ds)", s / 3600, (s % 3600) / 60, (s % 60));
    return formatter.format(exp) + " UTC " + ds;
  }

  private static String toStringLatency(PathMetadata meta) {
    int latencyMs = 0;
    boolean latencyComplete = true;
    for (int l : meta.getLatencyList()) {
      if (l >= 0) {
        latencyMs += l;
      } else {
        latencyComplete = false;
      }
    }
    if (latencyComplete) {
      return latencyMs + "ms";
    } else {
      return ">" + latencyMs + "ms (information incomplete)";
    }
  }

  private static String toStringBandwidth(PathMetadata meta) {
    long bw = Long.MAX_VALUE;
    boolean bwComplete = true;
    for (long l : meta.getBandwidthList()) {
      if (l > 0) {
        bw = Math.min(bw, l);
      } else {
        bwComplete = false;
      }
    }
    bw = bw == Long.MAX_VALUE ? 0 : bw;
    String bwString = bw + "KBit/s";
    if (!bwComplete) {
      bwString += " (information incomplete)";
    }
    return bwString;
  }

  private static String toStringGeo(PathMetadata meta) {
    StringBuilder s = new StringBuilder("[");
    for (PathMetadata.GeoCoordinates g : meta.getGeoList()) {
      if (s.length() > 1) {
        s.append(" > ");
      }
      if (g.getLatitude() == 0 && g.getLongitude() == 0 && g.getAddress().isEmpty()) {
        s.append("N/A");
      } else {
        s.append(g.getLatitude()).append(",").append(g.getLongitude());
        String addr = g.getAddress().replace("\n", ", ");
        s.append(" (\"").append(addr).append("\")");
      }
    }
    s.append("]");
    return s.toString();
  }

  private static String toStringLinkType(PathMetadata meta) {
    StringBuilder s = new StringBuilder("[");
    for (PathMetadata.LinkType lt : meta.getLinkTypeList()) {
      if (s.length() > 1) {
        s.append(", ");
      }
      switch (lt) {
        case UNSPECIFIED:
          s.append("unset");
          break;
        case DIRECT:
          s.append("direct");
          break;
        case MULTI_HOP:
          s.append("multihop");
          break;
        case OPEN_NET:
          s.append("opennet");
          break;
        default:
          s.append("unset");
          break;
      }
    }
    s.append("]");
    return s.toString();
  }

  private static String toStringEPIC(PathMetadata meta) {
    PathMetadata.EpicAuths ea = meta.getEpicAuths();
    if (ea == null) {
      return "false";
    }
    if (ea.getAuthLhvf() != null && ea.getAuthLhvf().length == 16) {
      return "true";
    }
    if (ea.getAuthPhvf() != null && ea.getAuthPhvf().length == 16) {
      return "true";
    }
    return "false";
  }

  private static String toStringNotes(PathMetadata meta) {
    StringBuilder s = new StringBuilder("[");
    int i = 0;
    for (String note : meta.getNotesList()) {
      if (note != null && !note.isEmpty()) {
        if (s.length() > 1) {
          s.append(", ");
        }
        long isdAs = meta.getInterfacesList().get(Math.max(0, i * 2 - 1)).getIsdAs();
        s.append(ScionUtil.toStringIA(isdAs));
        s.append(": \"").append(note).append("\"");
      }
      i++;
    }
    s.append("]");
    return s.toString();
  }
}
