# ExileForge

Android-клиент (Kotlin, Jetpack Compose) ARPG в духе Path of Exile для сервера **ktor-bestgame**. Сервер закреплён
сабмодулем `backend/`; правила игры (`backend/rules`) - общий код клиента и сервера: роллы, лист героя, текст модификаторов, забеги.

Сборка: JDK 17, Android SDK 37. Клонировать с `git clone --recurse-submodules`, затем

```bash
./gradlew :core:test :app:assembleDebug
```

- `RULES.md` - инварианты игры, архитектура, версии и CI; `CLAUDE.md` - порядок работы.
- `docs/` - описание механик для игрока.
