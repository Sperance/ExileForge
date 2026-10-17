package com.sperance.exileforge.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Полнота списка правок баланса (`.github/balance-paths.txt`): job `balance` CI идёт только при правке этих путей, поэтому каждый пакет
 * ядра, что симуляция сторожей читает транзитивно по импортам, обязан в нём быть - иначе правка боя прошла бы без сторожей.
 */
class BalancePathsTest {
    @Test
    fun everyPackageTheSimulationReadsIsABalancePath() {
        val patterns = File("../.github/balance-paths.txt").readLines().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("backend:") }
        val missing = SimulationReach.packages().map { MAIN + "/" + it.replace('.', '/') + "/" }.filterNot { dir -> patterns.any { covers(it, dir) } }
        assertTrue(missing.isEmpty(), "пакеты, что читает симуляция, не в .github/balance-paths.txt: $missing")
    }

    /** Шаблон папки со звёздами на конце покрывает папку пакета и всё под ней. */
    private fun covers(pattern: String, dir: String): Boolean = pattern.endsWith("/**") && dir.startsWith(pattern.removeSuffix("**"))

    /** Пакеты ядра, достижимые по импортам от исходников симуляции и сторожей. */
    private object SimulationReach {
        private val main = File("src/main/kotlin")
        private val starts = listOf(File("src/sim/kotlin"), File("src/test/kotlin/com/sperance/exileforge/core/sim"))
        private val packageLine = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
        private val coreImport = Regex("""^import\s+(com\.sperance\.exileforge\.core\.[\w.]+)""", RegexOption.MULTILINE)

        fun packages(): Set<String> {
            val byPackage = main.walk().filter { it.extension == "kt" }.groupBy { packageLine.find(it.readText())?.groupValues?.get(1).orEmpty() }
            fun importsOf(files: List<File>): Set<String> = files.flatMap { coreImport.findAll(it.readText()).map { m -> owner(m.groupValues[1], byPackage.keys) } }.filterNotNull().toSet()
            val reached = HashSet<String>()
            val queue = ArrayDeque(importsOf(starts.flatMap { it.walk().filter { f -> f.extension == "kt" }.toList() }))
            while (queue.isNotEmpty()) {
                val next = queue.removeFirst()
                if (reached.add(next)) queue.addAll(importsOf(byPackage[next].orEmpty()) - reached)
            }
            return reached
        }

        private fun owner(name: String, known: Set<String>): String? = generateSequence(name) { it.substringBeforeLast('.', "").ifEmpty { null } }.firstOrNull { it in known }
    }

    private companion object {
        const val MAIN = "core/src/main/kotlin"
    }
}
