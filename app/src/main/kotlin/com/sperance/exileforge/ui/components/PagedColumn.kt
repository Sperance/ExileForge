package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Gold

/**
 * Серверный список страницами - один компонент всех списков, что сервер отдаёт по `rules.pageSize`: ленивый список [state], который
 * дочитывается сам. Когда последняя видимая строка подходит к концу ближе [LOAD_AHEAD] строк, сервер говорит, что есть ещё ([more]), и
 * чтение не идёт ([loading]), просит следующую страницу ([onMore]) - один раз на [count] прочитанных строк: отказ не повторяется сам,
 * а только когда игрок снова долистает до края. Пока страница читается, внизу - кольцо; кнопок «Показать ещё» и страниц нет. Смена
 * поиска, отбора или порядка ([reset]) возвращает список к началу - модель читает его с первой страницы. Над строками - свои строки
 * места ([content]: поиск, чипы), они листаются вместе со списком.
 */
@Composable fun PagedColumn(
    count: Int,
    more: Boolean,
    loading: Boolean,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    reset: Any? = null,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: LazyListScope.() -> Unit,
) {
    val latest by rememberUpdatedState(onMore)
    val near by remember(state) {
        derivedStateOf { state.layoutInfo.let { info -> info.visibleItemsInfo.lastOrNull()?.let { it.index >= info.totalItemsCount - LOAD_AHEAD } == true } }
    }
    var asked by remember { mutableIntStateOf(NONE) }
    LaunchedEffect(reset) {
        asked = NONE
        state.scrollToItem(0)
    }
    LaunchedEffect(near, more, loading, count) {
        if (!near) asked = NONE
        if (near && more && !loading && asked != count) {
            asked = count
            latest()
        }
    }
    LazyColumn(modifier, state, contentPadding, verticalArrangement = verticalArrangement) {
        content()
        if (more) {
            item(key = MORE_KEY) {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Gold, strokeWidth = 2.dp)
                }
            }
        }
    }
}

/** За сколько строк до конца списка читается следующая страница. */
private const val LOAD_AHEAD = 3

/** Ещё ничего не просили на этом счёте строк. */
private const val NONE = -1

/** Ключ строки кольца в конце списка: не сталкивается с ключами строк места. */
private const val MORE_KEY = "paged.more"
