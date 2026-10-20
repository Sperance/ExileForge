package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.run.RunEvent

/**
 * Отклики забега ([ExpeditionRun]) наружу - тому, кто его ведёт: [recorded] - новое событие журнала, [cleared] - забег кончен (выход),
 * [fallen] - гибель героя, [gatherBroken] - сбор узла прерван боем или шагом героя. По умолчанию - никаких.
 */
class RunCallbacks(
    val recorded: (RunEvent) -> Unit = {},
    val cleared: () -> Unit = {},
    val fallen: () -> Unit = {},
    val gatherBroken: () -> Unit = {},
)
