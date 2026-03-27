# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

-- Nothing yet

## [0.2.2] - 2026-03-27

### Fixed

- Fixed local port option fow showpaths probing, document --healthy-only for ping and traceroute.
  [#11](https://github.com/netsec-ethz/scion-java-multiping/pull/11)
- Enable SHIM by default. Fix for #8.
  [#12](https://github.com/netsec-ethz/scion-java-multiping/pull/12)
- Improve SCMP error handling.
  [#13](https://github.com/netsec-ethz/scion-java-multiping/pull/13)
- Local AS causes exception.
  [#15](https://github.com/netsec-ethz/scion-java-multiping/pull/15)

## [0.2.1] - 2026-03-27

### Fixed

- Fixed showpaths showing wrong path status (of by 1 error).
  [#9](https://github.com/netsec-ethz/scion-java-multiping/pull/9)
- Fixed exception when path probes did no return.
  [#10](https://github.com/netsec-ethz/scion-java-multiping/pull/10)

## [0.2.0] - 2026-03-17

### Added
- Support for --log.level
  [#4](https://github.com/netsec-ethz/scion-java-multiping/pull/4)
- Added path probing for showpaths and --no-probe
  [#5](https://github.com/netsec-ethz/scion-java-multiping/pull/5)
- Added path probing for ping and --healthy-only
  [#6](https://github.com/netsec-ethz/scion-java-multiping/pull/6)

### Fixed

- Fixed showpaths issues.
  [#2](https://github.com/netsec-ethz/scion-java-multiping/pull/2)

## [0.1.0] - 2026-03-11

### Added

- Everything
  [#1](https://github.com/netsec-ethz/scion-java-multiping/pull/1)


### Changed

- Nothing

### Fixed

- Nothing

### Removed

- Nothing

[Unreleased]: https://github.com/netsec-ethz/jpan-cli/compare/v0.2.2...HEAD
[0.2.2]: https://github.com/netsec-ethz/jpan-cli/compare/v0.2.1...v0.2.2
[0.2.1]: https://github.com/netsec-ethz/jpan-cli/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/netsec-ethz/jpan-cli/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/netsec-ethz/jpan-cli/compare/init_root_commit...v0.1.0
