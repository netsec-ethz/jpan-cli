# JPAN Command Line Tool

A tool that allows echo, traceroute, showpaths, ... and other functionality, similar to
to the [SCION CLI tool](https://docs.scion.org/en/latest/command/scion/scion.html).
JPAN-CLI is stand-alone and does not require any locally installed SCION software. 

JPAN-CLI provides several tools:

* `jpan-cli address` - Show (one of) this host’s SCION address(es)

* `jpan-cli ping` - Test connectivity to a remote SCION host using SCMP echo packets

* `jpan-cli showpaths` - Display paths to a SCION AS

* `jpan-cli traceroute` - Trace the SCION route to a remote SCION AS using SCMP traceroute packets

* `jpan-cli version` - Show the SCION version information

## Execution

All tools can be run from the executable jar file which is available in
the [GitHub Releases section](https://github.com/netsec-ethz/jpan-cli/releases/download/v0.1.0/jpan-cli-exec.jar).
It can be executed with:

```
java -jar jpan-cli-exec.jar [tool-command]
```

See also the troubleshooting section below in case of issues.

# Help

To get command line help, the tool can be executed with:

```
java -jar jpan-cli-exec.jar help
```


# Download Assignments

The tool
parses [Anapayas ISD/AS assignment website](https://docs.anapaya.net/en/latest/resources/isd-as-assignments/)
and writes the result to a local file `isd-as-assignments.csv`.
Note: the `isd-as-assignments.csv` output file can be directly used as input file for the PingRepeat

# Ping All


# Troubleshooting

## No DNS search domain found. Please check your /etc/resolv.conf or similar.

This happens, for example, on Windows when using a VPN. One solution is to execute the jar with the
following property (
the example works only for `ethz.ch`):

```
java -Dorg.scion.dnsSearchDomains=ethz.ch. -jar jpan-cli-exec.jar
```
