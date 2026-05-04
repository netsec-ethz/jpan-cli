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

import static org.scion.jpan.Constants.*;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.stream.Collectors;
import org.scion.jpan.*;
import org.scion.jpan.internal.*;
import org.scion.jpan.internal.bootstrap.DNSHelper;
import org.scion.jpan.internal.bootstrap.LocalAS;
import org.scion.jpan.internal.bootstrap.ScionBootstrapper;
import org.scion.jpan.internal.paths.*;
import org.scion.jpan.internal.util.Config;
import org.scion.jpan.internal.util.IPHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CliScionService provides information such as: <br>
 * - Paths from A to B - The local ISD/AS numbers <br>
 * - Lookup op ISD/AS for host names via DNS. <br>
 *
 * <p>The CliScionService is intended as singleton. There should usually be only one instance that
 * is shared by all users. However, it may sometimes be desirable to have multiple instances, e.g.
 * for connecting to a different daemon or for better concurrency.
 *
 * <p>The default instance is of type CliScionService. All other CliScionService are of type {@code
 * Scion.CloseableService} which extends {@code AutoCloseable}.
 *
 * @see Scion.CloseableService
 */
public class CliScionService {

  private static final Logger LOG = LoggerFactory.getLogger(CliScionService.class.getName());

  private static final Object LOCK = new Object();
  private static CliScionService defaultService = null;

  private final LocalAS localAS;
  private final ControlServiceGrpc controlService;
  private final DaemonServiceGrpc daemonService;

  private Thread shutdownHook;

  protected enum Mode {
    DAEMON,
    BOOTSTRAP_SERVER_IP,
    BOOTSTRAP_VIA_DNS,
    BOOTSTRAP_TOPO_FILE,
    BOOTSTRAP_PATH_SERVICE
  }

  protected CliScionService(String addressOrHost, Mode mode) {
    if (mode == Mode.DAEMON) {
      LOG.info("Bootstrapping with daemon service: {}", addressOrHost);
      addressOrHost = IPHelper.ensurePortOrDefault(addressOrHost, DEFAULT_DAEMON_PORT);
      daemonService = DaemonServiceGrpc.create(addressOrHost);
      controlService = null;
      try {
        localAS = ScionBootstrapper.fromDaemon(daemonService);
      } catch (RuntimeException e) {
        // If this fails for whatever reason we want to make sure that the channel is closed.
        close();
        throw new ScionRuntimeException("Could not connect to daemon at: " + addressOrHost, e);
      }
    } else if (mode == Mode.BOOTSTRAP_PATH_SERVICE) {
      LOG.info("Bootstrapping with path service: {}", addressOrHost);
      localAS = ScionBootstrapper.fromPathService(addressOrHost);
      daemonService = null;
      controlService = null;
    } else {
      LOG.info("Bootstrapping with control service: mode={} target={}", mode.name(), addressOrHost);
      if (mode == Mode.BOOTSTRAP_VIA_DNS) {
        localAS = ScionBootstrapper.fromDns(addressOrHost);
      } else if (mode == Mode.BOOTSTRAP_SERVER_IP) {
        localAS = ScionBootstrapper.fromBootstrapServerIP(addressOrHost);
      } else if (mode == Mode.BOOTSTRAP_TOPO_FILE) {
        localAS = ScionBootstrapper.fromTopoFile(addressOrHost);
      } else {
        throw new UnsupportedOperationException();
      }
      daemonService = null;
      controlService = ControlServiceGrpc.create(localAS);
    }
    shutdownHook = addShutdownHook();
    try {
      checkStartShim();
    } catch (RuntimeException e) {
      // If this fails for whatever reason we want to make sure that the channel is closed.
      try {
        close();
      } catch (Exception ex) {
        // Ignore, we just want to get out.
      }
      throw e;
    }
  }

  private void checkStartShim() {
    // Start SHIM unless we have port range 'ALL'. However, config overrides this setting.
    String config = ScionUtil.getPropertyOrEnv(Constants.PROPERTY_SHIM, Constants.ENV_SHIM);
    boolean hasAllPorts = getLocalPortRange().hasPortRangeALL();
    boolean start = config != null ? Boolean.parseBoolean(config) : !hasAllPorts;
    if (start) {
      Shim.install();
    }
  }

  protected static void setDefaultService(CliScionService newDefaultService) {
    synchronized (LOCK) {
      defaultService = newDefaultService;
    }
  }

  /**
   * Returns the default instance of the CliScionService. The default instance is connected to the
   * daemon that is specified by the default properties or environment variables.
   *
   * @return default instance
   */
  public static CliScionService defaultService() {
    synchronized (LOCK) {
      // This is not 100% thread safe, but the worst that can happen is that
      // we call close() on a Service that has already been closed.
      if (defaultService != null) {
        return defaultService;
      }
      // try bootstrap service IP
      String fileName =
          ScionUtil.getPropertyOrEnv(PROPERTY_BOOTSTRAP_TOPO_FILE, ENV_BOOTSTRAP_TOPO_FILE);
      if (fileName != null) {
        defaultService = new CliScionService(fileName, Mode.BOOTSTRAP_TOPO_FILE);
        return defaultService;
      }

      String pathService = Config.getPathService();
      if (pathService != null) {
        defaultService = new CliScionService(pathService, Mode.BOOTSTRAP_PATH_SERVICE);
        return defaultService;
      }

      String server = ScionUtil.getPropertyOrEnv(PROPERTY_BOOTSTRAP_HOST, ENV_BOOTSTRAP_HOST);
      if (server != null) {
        defaultService = new CliScionService(server, Mode.BOOTSTRAP_SERVER_IP);
        return defaultService;
      }

      String naptrName =
          ScionUtil.getPropertyOrEnv(PROPERTY_BOOTSTRAP_NAPTR_NAME, ENV_BOOTSTRAP_NAPTR_NAME);
      if (naptrName != null) {
        defaultService = new CliScionService(naptrName, Mode.BOOTSTRAP_VIA_DNS);
        return defaultService;
      }

      // try daemon
      String daemon = ScionUtil.getPropertyOrEnv(PROPERTY_DAEMON, ENV_DAEMON, DEFAULT_DAEMON);
      try {
        defaultService = new CliScionService(daemon, Mode.DAEMON);
        return defaultService;
      } catch (ScionRuntimeException e) {
        LOG.info(e.getMessage());
        if (ScionUtil.getPropertyOrEnv(PROPERTY_DAEMON, ENV_DAEMON) != null) {
          throw e;
        }
      }

      // try normal network
      String searchDomain =
          ScionUtil.getPropertyOrEnv(PROPERTY_DNS_SEARCH_DOMAINS, ENV_DNS_SEARCH_DOMAINS);
      if (ScionUtil.getPropertyOrEnv(
              PROPERTY_USE_OS_SEARCH_DOMAINS,
              ENV_USE_OS_SEARCH_DOMAINS,
              DEFAULT_USE_OS_SEARCH_DOMAINS)
          || searchDomain != null) {
        String dnsResolver = DNSHelper.searchForDiscoveryService();
        if (dnsResolver != null) {
          defaultService = new CliScionService(dnsResolver, Mode.BOOTSTRAP_SERVER_IP);
          return defaultService;
        }
        LOG.info("No DNS record found for bootstrap server.");
        throw new ScionRuntimeException(
            "No DNS record found for bootstrap server. This means "
                + "the DNS server may not have NAPTR records for the bootstrap server or your host "
                + "may not have the search domains configured in /etc/resolv.conf or similar.");
      }
      throw new ScionRuntimeException("Could not connect to daemon, DNS or bootstrap resource.");
    }
  }

  public static void closeDefault() {
    synchronized (LOCK) {
      if (defaultService != null) {
        defaultService.close();
        defaultService = null;
      }
    }
  }

  private Thread addShutdownHook() {
    Thread hook =
        new Thread(
            () -> {
              if (defaultService != null) {
                defaultService.shutdownHook = null;
                defaultService.close();
              }
            });
    Runtime.getRuntime().addShutdownHook(hook);
    return hook;
  }

  public void close() {
    if (daemonService != null) {
      daemonService.close();
    }
    if (controlService != null) {
      controlService.close();
    }
    if (shutdownHook != null) {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
  }

  public Set<Long> getLocalIsdAses() {
    return localAS.getIsdAses();
  }

  /**
   * @param hostName hostName of the host to resolve
   * @return The ISD/AS code for a hostname
   * @throws ScionException if the DNS/TXT lookup did not return a (valid) SCION address.
   */
  public long getIsdAs(String hostName) throws ScionException {
    return AddressLookupService.getIsdAs(hostName);
  }

  /**
   * Determine the IPs that should be used as SRC address in a SCION header. These may differ from
   * the external IP in case we are behind a NAT. The source address should be the NAT mapped
   * address.
   *
   * @param channel channel
   * @return Mapping of external addresses, potentially one for each border router.
   */
  NatMapping getNatMapping(DatagramChannel channel) {
    List<InetSocketAddress> interfaces =
        localAS.getBorderRouters().stream()
            .map(LocalAS.BorderRouter::getInternalAddress)
            .collect(Collectors.toList());
    return NatMapping.createMapping(channel, interfaces);
  }

  LocalAS.DispatcherPortRange getLocalPortRange() {
    return localAS.getPortRange();
  }

  InetSocketAddress getBorderRouterAddress(int interfaceID) {
    return localAS.getBorderRouterAddress(interfaceID);
  }

  ControlServiceGrpc getControlServiceConnection() {
    return controlService;
  }

  DaemonServiceGrpc getDaemonConnection() {
    return daemonService;
  }

  public LocalAS getLocalAS() {
    return localAS;
  }

  public List<LocalAS.BorderRouter> getBorderRouters() {
    return localAS.getBorderRouters();
  }

  public List<LocalAS.ServiceNode> getControlServices() {
    return localAS.getControlServices();
  }
}
