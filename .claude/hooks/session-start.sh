#!/bin/bash
# Хук старта облачной сессии Claude Code: ставит то, чего нет в контейнере, - JDK 17 для тулчейна Gradle клиента,
# Android SDK для сборки :app, kotlin-language-server для диагностики Kotlin; подписывает коммиты владельцем и включает
# сторож сообщений коммитов (.githooks) в клиенте и сабмодуле сервера. Идемпотентен: готовое не качает заново; состояние
# контейнера после хука кэшируется. Локально (не в облаке) ничего не делает.
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

# Android SDK: :app собирается против compileSdk 37; лицензии принимаются, local.properties указывает на SDK.
SDK_HOME="$HOME/android-sdk"
CLT_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
if [ ! -x "$SDK_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "session-start: downloading Android command-line tools"
  tmp=$(mktemp -d)
  curl -fsSL "$CLT_URL" -o "$tmp/clt.zip"
  unzip -q "$tmp/clt.zip" -d "$tmp"
  mkdir -p "$SDK_HOME/cmdline-tools" && rm -rf "$SDK_HOME/cmdline-tools/latest" && mv "$tmp/cmdline-tools" "$SDK_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi
if [ ! -d "$SDK_HOME/platforms/android-37.0" ]; then
  yes | "$SDK_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_HOME" --licenses >/dev/null 2>&1 || true
  "$SDK_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_HOME" "platform-tools" "platforms;android-37.0" "build-tools;37.0.0" >/dev/null
fi
echo "sdk.dir=$SDK_HOME" > "$CLAUDE_PROJECT_DIR/local.properties"

# Подпись коммитов и сторож сообщений: клиент и сабмодуль сервера (если он инициализирован).
for repo in "$CLAUDE_PROJECT_DIR" "$CLAUDE_PROJECT_DIR/backend"; do
  if git -C "$repo" rev-parse --git-dir >/dev/null 2>&1; then
    git -C "$repo" config user.name "Sperance"
    git -C "$repo" config user.email "dmitriymalaye@gmail.com"
    git -C "$repo" config core.hooksPath "$CLAUDE_PROJECT_DIR/.githooks"
  fi
done

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "$CLAUDE_ENV_FILE"
fi
echo "session-start: JDK 17 at $JDK_HOME, Android SDK at $SDK_HOME, kotlin-language-server at $BIN_DIR, commits by Sperance"
