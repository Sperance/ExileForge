package com.sperance.exileforge.core.campaign

/**
 * Зерно скалы карты: шум края массива [edge] - и для поля стен ([WallField]), и для узора сцены. «Обточенный камень» - залы и
 * тёсаные биомы, почти ровный край; «Живая скала» - пещеры и пустоши, рваный край; «Заросли» - лесные биомы, мягкие кроны.
 */
enum class WallGrain(val edge: EdgeNoise) {
    ASHLAR(EdgeNoise(amplitude = .06f, scale = 1.6f, rough = .2f)),
    LIVE_ROCK(EdgeNoise(amplitude = .2f, scale = 2.4f, rough = .45f)),
    THICKET(EdgeNoise(amplitude = .17f, scale = 1.7f, rough = .15f)),
    ;

    companion object {
        /** Биомы зарослей и тёсаного камня; прочие - по раскладке карты: залы - камень, пещеры и пустоши - живая скала. */
        private val THICKETS = setOf("FOREST", "MIRE", "JUNGLE", "HIVE", "BLIGHT")
        private val DRESSED = setOf("SUNKEN", "CORAL", "TIDEVAULT", "GODHALL", "ASTRAL", "OBLIVION")

        /** Зерно скалы биома [biome] при раскладке [layout] (утверждено владельцем). */
        fun of(biome: String, layout: MapStyle): WallGrain = when (biome) {
            in THICKETS -> THICKET
            in DRESSED -> ASHLAR
            else -> if (layout == MapStyle.HALLS) ASHLAR else LIVE_ROCK
        }
    }
}
