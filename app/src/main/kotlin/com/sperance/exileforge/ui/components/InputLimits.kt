package com.sperance.exileforge.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted

/** The rules' input limits (3.73.0): every field cuts what is typed to them; their defaults stand until the content is read. */
val GameUi.inputs: InputLimits get() = index?.rules?.inputs ?: DefaultInputs
val AccountUi.inputs: InputLimits get() = index?.rules?.inputs ?: DefaultInputs

/** The limits where no state is at hand: the rules' own defaults, the same the server starts from. */
val DefaultInputs = InputLimits()

/** «12/24» under a field: how much of its limit is used, red once it is reached. */
@Composable fun LengthCounter(value: String, limit: Int) = Text("${value.length}/$limit", color = if (value.length >= limit) LifeRed else Muted, style = MaterialTheme.typography.labelSmall)
