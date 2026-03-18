# JPAN Command Line Tool

A tool that allows `echo`, `traceroute`, `showpaths`, ... and other functionality, similar to
to the [SCION CLI tool](https://docs.scion.org/en/latest/command/scion/scion.html).
JPAN-CLI is stand-alone based on [JPAN](https://github.com/scionproto-contrib/jpan)
and does not require any locally installed SCION software. 

JPAN-CLI provides several tools:

* `jpan-cli address` - Show (one of) this host’s SCION address(es)

* `jpan-cli ping` - Test connectivity to a remote SCION host using SCMP echo packets

* `jpan-cli ping-responder` - Run a local server that responds to incoming SCION pings

* `jpan-cli showpaths` - Display paths to a SCION AS

* `jpan-cli traceroute` - Trace the SCION route to a remote SCION AS using SCMP traceroute packets

* `jpan-cli version` - Show the SCION version information

## Usage

All tools can be run from the executable jar file which is available in
the [GitHub Releases section](https://github.com/netsec-ethz/jpan-cli/releases/download/v0.2.0/jpan-cli.jar).
It can be executed with:

```
java -jar jpan-cli.jar [tool-command]
```

For example, to get command line help, the tool can be executed with:

```
java -jar jpan-cli.jar help
```

## Known Issues

* In JPAN 0.6.1, the SCMP Responder will always return packets to 30041, regardless of their 
  actual source port, see JPAN issue [#233](https://github.com/scionproto-contrib/jpan/issues/233).
* jpan-cli should either start on 30041 or start a SHIM (if dispatcher portrange is limited)

# Troubleshooting

## No ping/traceroute answers received

In some ASes, border routers will send return packets to port 30041. To receive these packets,
please start the tool with `--port 30041`.


## No DNS search domain found. Please check your /etc/resolv.conf or similar.

This happens, for example, on Windows when using a VPN. One solution is to execute the jar with the
following property (
the example works only for `ethz.ch`):

```
java -Dorg.scion.dnsSearchDomains=ethz.ch. -jar jpan-cli.jar
```

# Acknowledgements

Functionality and documentation borrow heavily from the original
[scion tool](https://github.com/scionproto/scion/tree/master/scion)
([docs](https://docs.scion.org/en/latest/command/scion/scion.html)). 
