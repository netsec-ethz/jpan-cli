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

import java.util.List;
import java.util.concurrent.Callable;

public class Util {

  public static boolean PRINT = true;
  public static boolean DELAYED_PRINT = false; // print only at newlines
  private static final StringBuilder sb = new StringBuilder();

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

  private static void println(String... strs) {
    for (String m : strs) {
      print(m);
    }
    Util.println();
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
    public T t;

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

  public static void exit2(String... strs) {
    println(strs);
    System.exit(2);
  }

  public static <T> T tryParse(String argName, String argValue, Callable<T> fn) {
    try {
      return fn.call();
    } catch (Exception e) {
      exit2("Error: Invalid --" + argName + " value: ", argValue, " -> ", e.getMessage());
    }
    throw new IllegalStateException(); // Should never happen
  }

  public static Integer parseInt(String argName, List<String> args) {
    if (args.size() < 2) {
      exit2("Error: --" + argName + " requires a number");
    }
    int v = 0;
    try {
      v = Integer.parseInt(args.get(1));
    } catch (NumberFormatException e) {
      exit2("Error: Invalid --\" + argName + \" value: ", args.get(1));
    }
    args.remove(1);
    return v;
  }

  public static String parseString(String argName, List<String> args) {
    if (args.size() < 2) {
      exit2("Error: --" + argName + " requires a string argument");
    }
    return args.remove(1);
  }
}
