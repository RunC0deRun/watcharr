#!/bin/bash
set -e

# Try to find Android SDK
SDK_DIR="/Users/jan/Library/Android/sdk"
if [ ! -d "$SDK_DIR" ]; then
    SDK_DIR=$(grep sdk.dir ../../../../local.properties | cut -d'=' -f2)
fi

if [ -z "$SDK_DIR" ] || [ ! -d "$SDK_DIR" ]; then
    echo "Android SDK directory not found. Please specify it in local.properties or set SDK_DIR environment variable."
    exit 1
fi

# Find NDK under SDK_DIR/ndk or SDK_DIR/ndk-bundle
NDK_DIR=$(find "$SDK_DIR/ndk" -maxdepth 2 -name "toolchains" -print -quit 2>/dev/null | xargs dirname)

if [ -z "$NDK_DIR" ]; then
    NDK_DIR=$(find "$SDK_DIR/ndk-bundle" -maxdepth 2 -name "toolchains" -print -quit 2>/dev/null | xargs dirname)
fi

if [ -z "$NDK_DIR" ] || [ ! -d "$NDK_DIR" ]; then
    echo "Android NDK not found in $SDK_DIR/ndk or $SDK_DIR/ndk-bundle."
    echo "Please install NDK via Android Studio SDK Manager:"
    echo "  1. Open Android Studio -> Tools -> SDK Manager"
    echo "  2. Go to 'SDK Tools' tab"
    echo "  3. Check 'NDK (Side-by-side)' and click Apply/OK to install it."
    exit 1
fi

echo "Using Android NDK at: $NDK_DIR"

# Detect host OS (darwin-x86_64 for Apple Silicon / Intel Mac host toolchains)
HOST_OS="darwin-x86_64"

# Set up toolchain paths for arm64-v8a (Android API 26+)
TOOLCHAIN="$NDK_DIR/toolchains/llvm/prebuilt/$HOST_OS"
export CC="$TOOLCHAIN/bin/aarch64-linux-android26-clang"
export CXX="$TOOLCHAIN/bin/aarch64-linux-android26-clang++"
export AR="$TOOLCHAIN/bin/llvm-ar"
export GOOS="android"
export GOARCH="arm64"
export CGO_ENABLED=1

OUTPUT_DIR="../jniLibs/arm64-v8a"
mkdir -p "$OUTPUT_DIR"

echo "Compiling tsnet JNI library for arm64-v8a..."
cd "$(dirname "$0")"
go build -buildmode=c-shared -o "$OUTPUT_DIR/libtsnet.so" tsnet_jni.go

echo "--------------------------------------------------------"
echo "Successfully built $OUTPUT_DIR/libtsnet.so"
echo "--------------------------------------------------------"
