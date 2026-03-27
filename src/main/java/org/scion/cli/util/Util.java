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

package org.scion.cli.util;

import static org.scion.cli.util.Errors.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.scion.jpan.Constants;
import org.scion.jpan.Scion;
import org.scion.jpan.ScionService;
import org.scion.jpan.ScionUtil;
import org.scion.jpan.internal.IPHelper;
import org.scion.jpan.internal.ScionAddress;
import org.scion.jpan.internal.Shim;

public class Util {

  public static boolean PRINT = true;
  public static boolean DELAYED_PRINT = false; // print only at newlines
  private static final StringBuilder sb = new StringBuilder();
  private static Supplier<ScionService> serviceSupplier = Scion::defaultService;

  private Util() {}

  public static void setServiceSupplier(Supplier<ScionService> serviceSupplier) {
    Util.serviceSupplier = serviceSupplier;
  }

  public static ScionService defaultService() {
    return serviceSupplier.get();
  }

  public static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  public static void print(String msg) {
    if (PRINT) {
      if (DELAYED_PRINT) {
        sb.append(msg);
      } else {
        System.out.print(msg);
      }
    }
  }

  public static void println(String msg) {
    print(msg);
    println();
  }

  public static void println() {
    if (PRINT) {
      if (DELAYED_PRINT) {
        System.out.println(sb);
        sb.setLength(0);
      } else {
        System.out.println();
      }
    }
  }

  public static void clearPrintQueue() {
    sb.setLength(0);
  }

  public static double round(double d, int nDigits) {
    double div = Math.pow(10, nDigits);
    return Math.round(d * div) / div;
  }

  public static void prepareShim(boolean startShim, Integer port) {
    if (startShim && port != null && port.equals(Constants.SCMP_PORT)) {
      throw new ExitCodeException(
          2,
          "Cannot use port 30041 while SHIM is enabled. "
              + "Please choose a different port or use --no-shim");
    }
    System.setProperty(Constants.PROPERTY_SHIM, Boolean.toString(startShim));
    if (startShim) {
      Shim.install();
      if (!Shim.isInstalled()) {
        throw new ExitCodeException(2, "Could not start SHIM, port 30041 is already in use.");
      }
    }
  }

  public interface Runner {
    void run() throws IOException;
  }

  public static void handleExit(Runner runner) {
    try {
      runner.run();
    } catch (ExitCodeException e) {
      if (e.getMessage() != null && !e.getMessage().isEmpty()) {
        println(e.getMessage());
      }
      System.exit(e.exitCode());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  public static <T> T tryParse(String argName, String argValue, Callable<T> fn) {
    try {
      return fn.call();
    } catch (Exception e) {
      throw new ExitCodeException(
          2, "Error: Invalid --" + argName + " value: " + argValue + " -> " + e.getMessage());
    }
  }

  public static Integer parseInt(String argName, List<String> args) {
    if (args.size() < 2) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires a number");
    }
    int v;
    try {
      v = Integer.parseInt(args.get(1));
    } catch (NumberFormatException e) {
      throw new ExitCodeException(2, "Error: Invalid --\" + argName + \" value: " + args.get(1));
    }
    args.remove(1);
    return v;
  }

  public static String parseString(String argName, List<String> args) {
    if (args.size() < 2) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires a string argument");
    }
    return args.remove(1);
  }

  public static InetAddress parseIP(String argName, List<String> args) {
    if (args.size() < 2) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires an IP address argument");
    }
    try {
      return IPHelper.toInetAddress(args.remove(1));
    } catch (UnknownHostException e) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires an IP address argument");
    }
  }

  public static InetSocketAddress parseAddress(String argName, List<String> args) {
    if (args.size() < 2) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires an IP:port argument");
    }
    try {
      return IPHelper.toInetSocketAddress(args.remove(1));
    } catch (RuntimeException e) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires an IP:port argument");
    }
  }

  public static Long parseIsdAs(List<String> args) {
    String v = args.get(0);
    try {
      return ScionUtil.parseIA(args.remove(0));
    } catch (RuntimeException e) {
      throw new ExitCodeException(2, "Error: ISD/AS code is invalid: " + v);
    }
  }

  public static ScionAddress parseScionAddress(List<String> args) {
    String s = args.get(0);
    try {
      String[] addrParts = s.split(",");
      check(addrParts.length == 2, "Expected `,`");
      long isdIa = ScionUtil.parseIA(addrParts[0]);

      String addrStr;
      if (addrParts[1].startsWith("[")) {
        check(addrParts[1].startsWith("["), "Expected `[` before address");
        check(addrParts[1].endsWith("]"), "Expected `]` after address");
        addrStr = addrParts[1].substring(1, addrParts[1].length() - 1).trim();
      } else {
        addrStr = addrParts[1].trim();
      }
      check(!addrStr.isEmpty(), "Address is empty");

      byte[] addrBytes = IPHelper.toByteArray(addrStr);
      check(addrBytes != null, "Address string is not a legal address");
      InetAddress inetAddr = InetAddress.getByAddress(addrStr, addrBytes);
      ScionAddress scionAddress = ScionAddress.create(isdIa, inetAddr);
      args.remove(0);
      return scionAddress;
    } catch (IndexOutOfBoundsException | IllegalArgumentException | UnknownHostException e) {
      throw new ExitCodeException(2, PARSING_ADDRESS + s + ": error=\"" + e.getMessage() + "\"");
    }
  }

  private static void check(boolean pass, String msg) {
    if (!pass) {
      throw new IllegalArgumentException(msg);
    }
  }

  public static void parseAndSetLogLevel(List<String> args) {
    String ll = parseString("--log.level", args).toUpperCase();
    switch (ll) {
      case "ERROR":
      case "INFO":
      case "WARN":
      case "DEBUG":
        System.out.println("------------ log level: " + ll);
        System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, ll);
        break;
      default:
        throw new ExitCodeException(2, "Illegal log level: \"" + ll + "\"");
    }
  }
}
