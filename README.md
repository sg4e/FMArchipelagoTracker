# Yu-Gi-Oh! Forbidden Memories Tracker for Archipelago

![screenshot of the tracker](screenshot.png)

## How to get

Builds for Windows (x64) are available on the [Releases page](https://github.com/sg4e/FMArchipelagoTracker/releases/latest).

Linux (x64 and ARM64) and macOS (x64 and ARM64 M1) are also supported by running from source. 32-bit architectures are not supported.

## Running from source

How to build and run from source:

1. Clone the repo with `git clone https://github.com/sg4e/FMArchipelagoTracker.git` or by clicking `"Code" > "Download ZIP"` and extracting.
2. [GraalVM 21 or later](https://www.graalvm.org/downloads/) is required. Download for your operating system. If using the automated scripts in the next step, extract to the root of the repo and rename to `graalvm-jdk`. Directly inside should be folders `bin`, `conf`, `include`, etc.
3. Run (double-click) one of the following scripts for your operating system:
    - For Windows, use `run_windows.bat`
    - For Linux and macOS, use `run_linux_macos.sh`

## Versioning

This project uses the following versioning scheme on releases: `v[major].[minor].[patch]`, for example: `v1.2.4`.

The major version indicates compatibility with the [Forbidden Memories Archipelago `apworld` file](https://github.com/sg4e/Archipelago/releases), such that a major version of the tracker shall be compatible with the same major version of the `apworld`, irrespective of the minor version and patch version.

The minor version is incremented when a new feature or new functionality is added to the tracker.

The patch version is incremented when the release is only a bugfix.

## Copyright

Copyright 2024 sg4e. Licensed under the [MIT License](LICENSE.txt).

This project contains small images of duelists' faces in the [`duelists` folder](src/main/resources/moe/maika/fmaptracker/duelists) that are used in the application. These images are included under the assumption that their inclusion constitutes Fair Use.

This project is neither endorsed by nor affiliated with Konami. All intellectual property rights to the *Yu-Gi-Oh!* franchise and the *Forbidden Memories* game belong to Konami.
