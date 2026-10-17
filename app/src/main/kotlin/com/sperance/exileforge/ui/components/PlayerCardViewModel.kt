package com.sperance.exileforge.ui.components

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.player.PlayerCardModel

/** Карточка игрока: модель ядра [model] живёт, пока жив хозяин карточки. */
class PlayerCardViewModel(val model: PlayerCardModel) : ViewModel()
