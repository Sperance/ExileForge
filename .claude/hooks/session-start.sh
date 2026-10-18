#!/bin/bash
# Хук старта облачной сессии Claude Code: ставит то, чего нет в контейнере, - JDK 17 для тулчейна Gradle клиента и
# kotlin-language-server для плагина KotlinSense. Идемпотентен: готовое не качает заново; состояние контейнера после
# хука кэшируется. Локально (не в облаке) ничего не делает.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

JDK_HOME="$HOME/.jdk/temurin-17"
LS_HOME="$HOME/.kotlin-language-server"
BIN_DIR="$HOME/.local/bin"
LS_VERSION="1.3.13"
mkdir -p "$BIN_DIR"

# JDK 17: core/build.gradle.kts требует jvmToolchain(17), а foojay не умеет скачать его сам в этом контейнере.
if [ ! -x "$JDK_HOME/bin/java" ]; then
  echo "session-start: downloading Temurin JDK 17"
  tmp=$(mktemp -d)
  curl -fsSL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$tmp/jdk.tar.gz"
  mkdir -p "$tmp/jdk" && tar xzf "$tmp/jdk.tar.gz" -C "$tmp/jdk" --strip-components=1
  rm -rf "$JDK_HOME" && mkdir -p "$(dirname "$JDK_HOME")" && mv "$tmp/jdk" "$JDK_HOME"
  rm -rf "$tmp"
fi
mkdir -p "$HOME/.gradle"
if ! grep -qs "org.gradle.java.installations.paths=.*$JDK_HOME" "$HOME/.gradle/gradle.properties"; then
  echo "org.gradle.java.installations.paths=$JDK_HOME" >> "$HOME/.gradle/gradle.properties"
fi

# kotlin-language-server: бинарник плагина KotlinSense (диагностика Kotlin в сессии).
if [ ! -x "$LS_HOME/bin/kotlin-language-server" ]; then
  echo "session-start: downloading kotlin-language-server $LS_VERSION"
  tmp=$(mktemp -d)
  curl -fsSL "https://github.com/fwcd/kotlin-language-server/releases/download/$LS_VERSION/server.zip" -o "$tmp/server.zip"
  unzip -q "$tmp/server.zip" -d "$tmp"
  rm -rf "$LS_HOME" && mv "$tmp/server" "$LS_HOME" && chmod +x "$LS_HOME/bin/kotlin-language-server"
  rm -rf "$tmp"
fi
ln -sf "$LS_HOME/bin/kotlin-language-server" "$BIN_DIR/kotlin-language-server"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "$CLAUDE_ENV_FILE"
fi
echo "session-start: JDK 17 at $JDK_HOME, kotlin-language-server at $BIN_DIR"
