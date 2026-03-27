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

// Much of the text of this file was copied from https://github.com/scionproto/scion
// Copyright ETH Zurich, Anapaya, et al.

package org.scion.cli;

import static org.scion.cli.util.Util.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import org.scion.cli.util.ExitCodeException;

public class Cli {

  private static final String VERSION = "0.2.1 (using JPAN 0.6.1)";

  public static void main(String... args) {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
    handleExit(() -> run(args));
    // handleExit(() -> run(new String[] {"traceroute", "66-2:0:18,10.0.0.1"}));
    // handleExit(() -> run(new String[] {"showpaths", "67-401500"}));
  }

  public static void run(String... args) throws IOException {
    checkArgs(args, 1, Integer.MAX_VALUE);
    String mode = args[0].toLowerCase(Locale.ROOT);
    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
    switch (mode) {
      case "address":
        new Address().run(newArgs);
        return;
      case "h":
      case "help":
        printHelp(args.length == 1 ? "" : args[1]);
        return;
      case "ping":
        new Ping().run(newArgs);
        return;
      case "pr":
      case "ping-responder":
        new PingResponder().run(newArgs);
        return;
      case "sp":
      case "showpaths":
        new Showpaths().run(newArgs);
        return;
      case "tr":
      case "traceroute":
        new Traceroute().run(newArgs);
        return;
      case "v":
      case "version":
        printVersion();
        return;
      default:
        printUsage();
        throw new ExitCodeException(2, "Unknown command: \"" + mode + "\"");
    }
  }

  private static void checkArgs(String[] args, int minArgs, int maxArgs) {
    if (args.length < minArgs || args.length > maxArgs) {
      println("Invalid number of arguments.");
      printUsage();
      throw new ExitCodeException(2, "Invalid number of arguments.");
    }
  }

  private static void printHelp(String mode) {
    switch (mode) {
      case "address":
        printUsageAddress();
        return;
      case "ping":
        printUsagePing();
        return;
      case "tr":
      case "traceroute":
        printUsageTraceroute();
        return;
      case "sp":
      case "showpaths":
        printUsageShowpaths();
        return;
      case "ping-responder":
        printUsagePingResponder();
        return;
      default:
        printUsage();
    }
  }

  private static void printVersion() {
    println("jpan-cli version: " + VERSION);
  }

  private static void printUsage() {
    println("Usage:");
    println("  jpan-cli [command]");
    println();
    println("Available commands: ");
    println("  address           Show (one of) this host's SCION address(es)");
    println("  help              Help about any command");
    println("  ping              Test connectivity to a remote SCION host using SCMP echo packets");
    println("  ping-responder    Starting a server that responds to incoming echo requests.");
    println("  showpaths         Display paths to a SCION AS");
    println("  traceroute        Trace the SCION route to a remote SCION AS using SCMP traceroute");
    println("  version           Show the SCION version information");
    println("");
    println("Flags:");
    println("  -h, --help   help for jpan-cli");
    println("");
    println("Use \"jpan-cli [command] --help\" for more information about a command.");
  }

  private static void printUsageAddress() {
    println("'address' show address information about this SCION host.");
    println("");
    println("This command returns the relevant SCION address information for this host.");
    println("");
    println("Currently, this returns a sensible but arbitrary local address. In the general");
    println("case, the host could have multiple SCION addresses.");
    println("");
    println("Usage:");
    println("  jpan-cli address [flags]");
    println("");
    println("Examples:");
    println("  jpan-cli address");
    println("");
    println("Flags:");
    println("  -h, --help                   help for address");
    // println("      --isd-as isd-as   The local ISD-AS to use. (default 0-0)");
    // println("      --json            Write the output as machine readable json");
    println("  -l, --local ip               Local IP address to listen on. (default invalid IP)");
    println(
        "      --log.level string       Console logging level verbosity (debug|info|warn|error)");
    println("      --sciond string          SCION Daemon address. (default \"127.0.0.1:30255\")");
  }

  static void printUsagePing() {
    println("'ping' test connectivity to a remote SCION host using SCMP echo packets.");
    println("");
    println("When the --count option is set, ping sends the specified number of SCMP echo packets");
    println("and reports back the statistics.");
    println("");
    println(
        "When the --healthy-only option is set, ping first determines healthy paths through "
            + "probing and");
    println("chooses amongst them.");
    println("");
    println("If no reply packet is received at all, ping will exit with code 1.");
    println("On other errors, ping will exit with code 2.");
    println("");
    // printSequenceHelp();
    // println("");
    println("");
    println("Usage:");
    println("  jpan-cli ping [flags] <remote>");
    println("");
    println("Examples:");
    println("  jpan-cli ping 1-ff00:0:110,10.0.0.1");
    println("  jpan-cli ping 1-ff00:0:110,10.0.0.1 -c 5");
    println("  jpan-cli ping --url ethz.ch -c 5");
    println("");
    println("Flags:");
    println("  -c, --count uint16           total number of packets to send");
    //    println("      --epic                   Enable EPIC for path probing.");
    //    println("      --format string          Specify the output format (human|json|yaml)
    // (default \"human\")");
    println("      --healthy-only           only use healthy paths");
    println("  -h, --help                   help for ping");
    //    println("  -i, --interactive            interactive mode");
    println("      --interval duration      time between packets (default 1s)");
    // println("      --isd-as isd-as          The local ISD-AS to use. (default 0-0)");
    println("  -l, --local ip               Local IP address to listen on. (default invalid IP)");
    println(
        "      --log.level string       Console logging level verbosity (debug|info|warn|error)");
    //    println("      --max-mtu                choose the payload size such that the sent SCION
    // packet including the SCION Header,");
    //    println("                               SCMP echo header and payload are equal to the MTU
    // of the path. This flag overrides the");
    //    println("                               'payload_size' and 'packet_size' flags.");
    //    println("      --no-color               disable colored output");
    println("      --no-shim                do not start SHIM dispatcher");
    //    println("      --packet-size uint       number of bytes to be sent including the SCION
    // Header and SCMP echo header,");
    //    println("                               the desired size must provide enough space for the
    // required headers. This flag");
    //    println("                               overrides the 'payload_size' flag.");
    println("      --port uint16            use specified local port");
    println(
        "  -s, --payload-size uint      number of bytes to be sent in addition to the SCION Header and SCMP echo header;");
    println(
        "                               the total size of the packet is still variable size due to the variable size of");
    //    println("      --refresh                set refresh flag for path request");
    println("      --sciond string          SCION Daemon address. (default \"127.0.0.1:30255\")");
    //    println("      --sequence string        Space separated list of hop predicates");
    // println("      --shim                   start with SHIM enabled (default disabled).");
    println("      --timeout uint16         timeout per packet in milliseconds (default 1s)");
    println("      --url url                use and resolve a url as destination address");
  }

  static void printUsageTraceroute() {
    println("'traceroute' traces the SCION path to a remote AS using");
    println("SCMP traceroute packets.");
    println("");
    println(
        "When the --healthy-only option is set, traceroute first determines healthy paths "
            + "through probing and");
    println("chooses amongst them.");
    println("");
    println("If any packet is dropped, traceroute will exit with code 1.");
    println("On other errors, traceroute will exit with code 2.");
    //    printSequenceHelp();
    println("");
    println("Usage:");
    println("  jpan-cli traceroute [flags] <remote>");
    println("");
    println("Aliases:");
    println("  traceroute, tr");
    println("");
    println("Examples:");
    println("  jpan-cli traceroute 1-ff00:0:110,10.0.0.1");
    println("");
    println("Flags:");
    //    println("      --epic                   Enable EPIC.");
    //    println("      --format string          Specify the output format (human|json|yaml)
    // (default \"human\")");
    println("      --healthy-only           only use healthy paths");
    println("  -h, --help                   help for traceroute");
    //    println("  -i, --interactive            interactive mode");
    //    println("      --isd-as isd-as          The local ISD-AS to use. (default 0-0)");
    //    println("  -l, --local ip               Local IP address to listen on. (default invalid
    // IP)");
    println(
        "      --log.level string       Console logging level verbosity (debug|info|warn|error)");
    //    println("      --no-color               disable colored output");
    println("      --no-shim                do not start SHIM dispatcher");
    println("      --port uint16            use specified local port");
    //    println("      --refresh                set refresh flag for path request");
    println("      --sciond string          SCION Daemon address. (default \"127.0.0.1:30255\")");
    //    println("      --sequence string        Space separated list of hop predicates");
    //    println("      --shim                   start with SHIM enabled (default disabled).");
    println("      --timeout uint16         timeout per packet in milliseconds (default 1s)");
    println("      --url url                use and resolve a url as destination address");
  }

  static void printUsageShowpaths() {
    println("'showpaths' lists available paths between the local and the specified");
    println("SCION AS.");
    println("");
    println("By default, the paths are probed. Paths served from the SCION Daemon's might not");
    println("forward traffic successfully (e.g. if a network link went down, or there is a black");
    println("hole on the path). To disable path probing, set the appropriate flag.");
    println("");
    // println("If no alive path is discovered, json output is not enabled, and probing is not");
    println("If no alive path is discovered, and probing is not");
    println("disabled, showpaths will exit with the code 1.");
    println("On other errors, showpaths will exit with code 2.");
    //    println("");
    //    printSequenceHelp();
    //    println("");
    println("");
    println("Usage:");
    println("  jpan-cli showpaths ISD-AS [flags]");
    println("");
    println("Aliases:");
    println("  showpaths, sp");
    println("");
    println("Examples:");
    println("  jpan-cli showpaths 1-ff00:0:110 --extended");
    // println("  jpan-cli showpaths 1-ff00:0:110 --local 127.0.0.55 --json");
    // println("  jpan-cli showpaths 1-ff00:0:111 --sequence=\"0-0#2 0*\" # outgoing IfID=2");
    // println(
    //    "  jpan-cli showpaths 1-ff00:0:111 --sequence=\"0* 0-0#41\" # incoming IfID=41 at dstIA");
    // println(
    //     "  jpan-cli showpaths 1-ff00:0:111 --sequence=\"0* 1-ff00:0:112 0*\" # 1-ff00:0:112 on
    // the path");
    // println("  jpan-cli showpaths 1-ff00:0:110 --no-probe");
    println("");
    println("Flags:");
    //    println("      --epic                   Enable EPIC.");
    println("  -e, --extended               Show extended path meta data information");
    //    println(
    //        "      --format string          Specify the output format (human|json|yaml) (default
    // \"human\")");
    println("  -h, --help                   help for showpaths");
    //    println("      --isd-as isd-as          The local ISD-AS to use. (default 0-0)");
    println("  -l, --local ip               Local IP address to listen on. (default invalid IP)");
    println(
        "      --log.level string       Console logging level verbosity (debug|info|warn|error)");
    println(
        "  -m, --maxpaths int           maximum number of paths that are displayed (default 10)");
    //    println("      --no-color               disable colored output");
    println("      --no-probe               Do not probe the paths and print the health status");
    println("      --no-shim                do not start SHIM dispatcher");
    println("      --port uint16            Use specified local port for probing");
    //    println("  -r, --refresh                Set refresh flag for SCION Daemon path request");
    println("      --sciond string          SCION Daemon address. (default \"127.0.0.1:30255\")");
    //    println("      --sequence string        Space separated list of hop predicates");
    println("      --timeout uint16         Probing timeout in milliseconds (default 5s)");
    //    println("      --tracing.agent string   Tracing agent address");
  }

  static void printUsagePingResponder() {
    println("`ping-responder` starts a server process that responds to incoming echo requests.");
    println("");

    println("Usage:");
    println("  jpan-cli ping-responder [flags]");
    println();
    println("Aliases:");
    println("  ping-responder, pr");
    println("");
    println("Examples:");
    println("  jpan-cli ping-responder 1-ff00:0:110");
    println("  jpan-cli ping-responder 1-ff00:0:110 --port 12345");
    // println("  jpan-cli ping-responder 1-ff00:0:110 --local 127.0.0.55");
    println("");
    println("Flags:");
    // println("  -l, --local ip               Local IP address to listen on. (default invalid
    // IP)");
    println(
        "      --log.level string       Console logging level verbosity (debug|info|warn|error)");
    println("      --port uint16            use specified local port.");
  }

  private static void printSequenceHelp() {
    println("The paths can be filtered according to a sequence. A sequence is a string of");
    println("space separated HopPredicates. A Hop Predicate (HP) is of the form");
    println("'ISD-AS#IF,IF'. The first IF means the inbound interface (the interface where");
    println("packet enters the AS) and the second IF means the outbound interface (the");
    println("interface where packet leaves the AS).  0 can be used as a wildcard for ISD, AS");
    println("and both IF elements independently.");
    println("");
    println("HopPredicate Examples:");
    println("");
    println(" ");
    println(" Match any:                               0");
    println(" Match ISD 1:                             1");
    println(" Match AS 1-ff00:0:133:                   1-ff00:0:133");
    println(" Match IF 2 of AS 1-ff00:0:133:           1-ff00:0:133#2");
    println(" Match inbound IF 2 of AS 1-ff00:0:133:   1-ff00:0:133#2,0");
    println(" Match outbound IF 2 of AS 1-ff00:0:133:  1-ff00:0:133#0,2");
    println(" ");
    println("");
    println("Sequence Examples:");
    println("");
    println(" ");
    println(" sequence: \"1-ff00:0:133#0 1-ff00:0:120#2,1 0 0 1-ff00:0:110#0\"");
    println(" ");
    println("");
    println("The above example specifies a path from any interface in AS 1-ff00:0:133 to");
    println("two subsequent interfaces in AS 1-ff00:0:120 (entering on interface 2 and");
    println("exiting on interface 1), then there are two wildcards that each match any AS.");
    println("The path must end with any interface in AS 1-ff00:0:110.");
    println("");
    println("");
    println(" sequence: \"1-ff00:0:133#1 1+ 2-ff00:0:1? 2-ff00:0:233#1\"");
    println("");
    println("");
    println("The above example includes operators and specifies a path from interface");
    println("1-ff00:0:133#1 through multiple ASes in ISD 1, that may (but does not need to)");
    println("traverse AS 2-ff00:0:1 and then reaches its destination on 2-ff00:0:233#1.");
    println("");
    println("Available operators:");
    println("");
    println("");
    println("  ?     (the preceding HopPredicate may appear at most once)");
    println("  +     (the preceding ISD-level HopPredicate must appear at least once)");
    println("  *     (the preceding ISD-level HopPredicate may appear zero or more times)");
    println("  |     (logical OR)");
  }
}
