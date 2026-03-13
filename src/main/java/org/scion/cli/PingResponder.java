// Copyright 2024 ETH Zurich
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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.scion.cli.util.ExitCodeException;
import org.scion.cli.util.Util;
import org.scion.jpan.*;

/** A simple echo responder that responds to SCMP echo requests. */
public class PingResponder {

  private static int localPort = 30041;
  private static InetAddress localIP = null;

  public static void main(String[] args) {
    handleExit(() -> run(args));
  }

  public static void run(String[] args) throws IOException {
    parseArgs(args);
    if (localPort == 30041) {
      System.setProperty(Constants.PROPERTY_SHIM, "false"); // disable SHIM
    }

    try (ScmpResponder responder = Scmp.newResponderBuilder().setLocalPort(localPort).build()) {
      responder.setScmpErrorListener(PingResponder::logError);
      responder.setOption(ScionSocketOptions.SCION_API_THROW_PARSER_FAILURE, true);
      responder.setScmpEchoListener(PingResponder::log);
      println("Starting ping responder on port: " + localPort);
      println("Stop with \"ctrl-c\"");
      responder.start();
    }
  }

  private static void parseArgs(String[] argsArray) {
    List<String> args = new ArrayList<>(Arrays.asList(argsArray));
    while (!args.isEmpty()) {
      switch (args.get(0)) {
        case "h":
        case "--help":
          Cli.printUsagePingResponder();
          throw new ExitCodeException(0);
        case "l":
        case "--local":
          localIP = parseIP("local", args);
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

  private static boolean log(Scmp.EchoMessage msg) {
    Util.print(
        "Received: "
            + msg.getTypeCode().getText()
            + " from "
            + msg.getPath().getRemoteAddress().getHostAddress()
            + " via ");
    Util.println(ScionUtil.toStringPath(msg.getPath().getRawPath()));
    return true;
  }

  private static void logError(Scmp.Message msg) {
    println("ERROR: " + msg.getTypeCode().getText());
  }
}
