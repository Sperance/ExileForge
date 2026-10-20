package com.sperance.exileforge.core.filter

/**
 * Включённое сужение списка - чип под строкой поиска: подпись [title] словами («Ур. 27–37», «Лучше надетого», «Мод: здоровье») и
 * фильтр [without] - тот же, но без этого сужения; снять чип - применить [without]. Сужения считает модель фильтра, экран их только
 * рисует.
 */
data class Narrowing<out F>(val title: String, val without: F)
