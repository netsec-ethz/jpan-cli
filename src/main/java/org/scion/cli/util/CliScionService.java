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

package org.scion.cli.util;

import org.scion.jpan.*;
import org.scion.jpan.internal.bootstrap.LocalAS;

public class CliScionService extends ScionService {

  protected CliScionService(Builder builder) {
    super(builder);
  }

  public static CliScionService defaultService() {
    return ScionService.defaultService(new Builder());
  }

  protected static class Builder extends ScionService.Builder<CliScionService> {
    @Override
    public CliScionService build() {
      return new CliScionService(this);
    }
  }

  @Override
  public LocalAS getLocalAS() {
    return super.getLocalAS();
  }
}
