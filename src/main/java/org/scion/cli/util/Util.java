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

import org.scion.jpan.ScionRuntimeException;
import org.scion.jpan.ScionSocketAddress;
import org.scion.jpan.ScionUtil;
import org.scion.jpan.internal.IPHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;

public class Util {

  public static boolean PRINT = true;
  public static boolean DELAYED_PRINT = false; // print only at newlines
  private static final StringBuilder sb = new StringBuilder();

  private Util() {}

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

  private static String toString(String... strs) {
    StringBuilder sb = new StringBuilder();
    for (String m : strs) {
      sb.append(m);
    }
    return sb.toString();
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

  public static class Ref<T> {
    private T t;

    private Ref(T t) {
      this.t = t;
    }

    public static <T> Ref<T> empty() {
      return new Ref<>(null);
    }

    public static <T> Ref<T> of(T t) {
      return new Ref<>(t);
    }

    public T get() {
      return t;
    }

    public void set(T t) {
      this.t = t;
    }
  }

  public interface Runner {
    void run() throws IOException;
  }

  public static void handleExit(Runner runner) {
    try {
      runner.run();
    } catch (ExitCodeException e) {
      println(e.getMessage());
      System.exit(e.exitCode());
    } catch (IOException e) {
      throw new ScionRuntimeException(e);
    }
  }

  public static void exit2(String... strs) {
    String msg = toString(strs);
    println(msg);
    throw new ExitCodeException(2, msg);
  }

  public static <T> T tryParse(String argName, String argValue, Callable<T> fn) {
    try {
      return fn.call();
    } catch (Exception e) {
      throw new ExitCodeException(2, "Error: Invalid --" + argName + " value: " + argValue + " -> " + e.getMessage());
    }
  }

  public static Integer parseInt(String argName, List<String> args) {
    if (args.size() < 2) {
      exit2("Error: --" + argName + " requires a number");
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
      throw new ExitCodeException(2, "Error: --" + argName + " requires a string argument");
    }
    try {
      return IPHelper.toInetAddress(args.remove(1));
    } catch (UnknownHostException e) {
      throw new ExitCodeException(2, "Error: --" + argName + " requires an IP address argument");
    }
  }

  public static ScionSocketAddress parseScionAddress(List<String> args) {
    String s = args.get(1);
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
      return ScionSocketAddress.from(null, isdIa, inetAddr, 30041);
    } catch (IndexOutOfBoundsException | IllegalArgumentException | UnknownHostException e) {
      println("ERROR parsing address " + s + ": error=\"" + e.getMessage() + "\"");
    }

    if (args.size() != 1) {
      exit2("Error: could not parse destination address: " + args.get(1));
    }
    args.remove(1);
    return null;
  }

  private static void check(boolean pass, String msg) {
    if (!pass) {
      throw new IllegalArgumentException(msg);
    }
  }
}
