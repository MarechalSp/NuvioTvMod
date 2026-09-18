# macOS libmpv runtime

This directory contains the ARM64 and x86_64 dynamic-library runtime sets used
by the macOS desktop player. They require macOS 12 or newer.

Each architecture directory contains the complete transitive `@rpath`
dependency closure of `libmpv.2.dylib`. Refresh each closure as a unit; never
mix libraries across versions or architectures.

The Gradle build selects the directory matching `nuvio.macos.arch`, copies only
that runtime into the desktop JAR, generates `runtime-files.txt`, and extracts
the files beside `libplayer_bridge.dylib`. The bridge resolves them through its
`@loader_path` rpath.

Preserve all applicable third-party license notices and source-availability
obligations when distributing these binaries.

The dylibs are tracked with Git LFS. Ensure Git LFS is installed and initialized
before staging or checking out this directory.
