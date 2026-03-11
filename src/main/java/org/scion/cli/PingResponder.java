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

import static org.scion.cli.util.Util.PRINT;
import static org.scion.cli.util.Util.println;

import java.io.IOException;
import org.scion.jpan.*;
import org.scion.cli.util.Util;

/** A simple echo responder that responds to SCMP echo requests. */
public class PingResponder {

  private static int localPort = 30041;

  public static void main(String[] args) throws IOException {
    PRINT = true;

    try (ScmpResponder responder =
        Scmp.newResponderBuilder().setLocalPort(localPort).build()) {
      responder.setScmpErrorListener(PingResponder::printError);
      responder.setOption(ScionSocketOptions.SCION_API_THROW_PARSER_FAILURE, true);
      responder.setScmpEchoListener(PingResponder::print);
      responder.start();
    }
  }

  private static boolean print(Scmp.EchoMessage msg) {
    Util.print(
        "Received: "
            + msg.getTypeCode().getText()
            + " from "
            + msg.getPath().getRemoteAddress()
            + "via ");
    Util.println(ScionUtil.toStringPath(msg.getPath().getMetadata()));
    return true;
  }

  private static void printError(Scmp.Message msg) {
    println("ERROR: " + msg.getTypeCode().getText());
  }
}
