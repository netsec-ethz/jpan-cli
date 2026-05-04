# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- `address` was giving too many address.
  [#20](https://github.com/netsec-ethz/jpan-cli/pull/20)
- Fixed missing `--timeout` for `showpaths`. Fixed timeout message.
  [#21](https://github.com/netsec-ethz/jpan-cli/pull/21)

### Changed

- Update to JPAN 0.7.0 and improve SCMP error handling.
  [#18](https://github.com/netsec-ethz/jpan-cli/pull/18)
- Adapt to JPAN 0.7.0 and improve SCMP error handling.
  [#14](https://github.com/netsec-ethz/jpan-cli/pull/14)
  [#19](https://github.com/netsec-ethz/jpan-cli/pull/19)

## [0.2.2] - 2026-03-27

### Fixed

- Fixed local port option fow showpaths probing, document --healthy-only for ping and traceroute.
  [#11](https://github.com/netsec-ethz/jpan-cli/pull/11)
- Enable SHIM by default. Fix for #8.
  [#12](https://github.com/netsec-ethz/jpan-cli/pull/12)
- Improve SCMP error handling.
  [#13](https://github.com/netsec-ethz/jpan-cli/pull/13)
- Local AS causes exception.
  [#15](https://github.com/netsec-ethz/jpan-cli/pull/15)

## [0.2.1] - 2026-03-27

### Fixed

- Fixed showpaths showing wrong path status (of by 1 error).
  [#9](https://github.com/netsec-ethz/jpan-cli/pull/9)
- Fixed exception when path probes did no return.
  [#10](https://github.com/netsec-ethz/jpan-cli/pull/10)

## [0.2.0] - 2026-03-17

### Added
- Support for --log.level
  [#4](https://github.com/netsec-ethz/jpan-cli/pull/4)
- Added path probing for showpaths and --no-probe
  [#5](https://github.com/netsec-ethz/jpan-cli/pull/5)
- Added path probing for ping and --healthy-only
  [#6](https://github.com/netsec-ethz/jpan-cli/pull/6)

### Fixed

- Fixed showpaths issues.
  [#2](https://github.com/netsec-ethz/jpan-cli/pull/2)

## [0.1.0] - 2026-03-11

### Added

- Everything
  [#1](https://github.com/netsec-ethz/jpan-cli/pull/1)


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
