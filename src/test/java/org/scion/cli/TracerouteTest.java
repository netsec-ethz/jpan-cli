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

package org.scion.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.scion.cli.util.ExitCodeException;

class TracerouteTest {

  @Test
  void main_noArg() {
    ExitCodeException e;
    e = assertThrows(ExitCodeException.class, () -> Cli.run("tr"));
    assertEquals(2, e.exitCode());
    assertTrue(e.getMessage().contains("missing address or --url"));
  }

  @Test
  void main_badArg() {
    ExitCodeException e;
    e = assertThrows(ExitCodeException.class, () -> Cli.run("tr", "1-123:4,127.0.0.1"));
    assertEquals(2, e.exitCode());
    assertTrue(e.getMessage().contains("ERROR parsing address"));
  }

  @Test
  void logLevel_error() {
    ExitCodeException e;
    e =
        assertThrows(
            ExitCodeException.class,
            () -> Cli.run("traceroute", "1-123,127.0.0.1", "--log.level", "boring"));
    assertEquals(2, e.exitCode());
    assertTrue(e.getMessage().contains("BORING"));
  }
}
