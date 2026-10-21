#!/usr/bin/env bash
# Проверка клиента перед пушем и шаги job `core` и `app` в android.yml - один список задач Gradle (RULES.md, «CI»).
#   check.sh core               :core:ktlintCheck, commentStyle, :core:test                      (job core)
#   check.sh app [--no-compile] :app:ktlintCheck, сторожа :app, commentStyle, Android Lint, компиляция :app (job app - без компиляции)
#   check.sh all [--no-compile] то и другое одним запуском Gradle
# Целиком :app собирают только релизные job: minified, minified-strict, release-apk.
set -euo pipefail
cd "$(dirname "$0")/.."

usage() {
    sed -n '2,6p' "$0" >&2
    exit 2
}

mode=${1:-}
[ $# -gt 0 ] && shift
compile=true
for arg in "$@"; do
    case "$arg" in
        --no-compile) compile=false ;;
        *) usage ;;
    esac
done

core=(:core:ktlintCheck commentStyle :core:test)
app=(:app:ktlintCheck :app:androidOnly :app:forgeButtonsOnly commentStyle :app:lintRelease)
if [ "$compile" = true ]; then
    app+=(:app:compileMinifiedTestKotlin :app:compileMinifiedTestAndroidTestKotlin)
fi

case "$mode" in
    core) tasks=("${core[@]}") ;;
    app) tasks=("${app[@]}") ;;
    all) tasks=("${core[@]}" "${app[@]}") ;;
    *) usage ;;
esac

# Задача, названная дважды (commentStyle в core и app), Gradle отдаётся один раз
unique=()
for task in "${tasks[@]}"; do
    case " ${unique[*]-} " in
        *" $task "*) ;;
        *) unique+=("$task") ;;
    esac
done
exec bash gradlew "${unique[@]}"
