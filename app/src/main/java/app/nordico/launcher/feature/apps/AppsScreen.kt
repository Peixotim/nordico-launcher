package app.nordico.launcher.feature.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nordico.launcher.AppContainer
import app.nordico.launcher.data.apps.AppEntry
import app.nordico.launcher.data.apps.InternalRoute
import app.nordico.launcher.designsystem.LocalNordic
import app.nordico.launcher.designsystem.NText
import app.nordico.launcher.designsystem.Sym
import app.nordico.launcher.designsystem.components.PageHeader
import app.nordico.launcher.designsystem.nordicText
import app.nordico.launcher.designsystem.pressClick
import app.nordico.launcher.navigation.SharedKeys
import app.nordico.launcher.navigation.sharedSheet

/** Gaveta de apps: Seus apps / Do carro / Ocultos, busca e edição (spec 10.5). */
@Composable
fun AppsScreen(c: AppContainer, onBack: () -> Unit, onRoute: (InternalRoute) -> Unit) {
    val vm: AppsViewModel = viewModel { AppsViewModel(c) }
    val sections by vm.sections.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val n = LocalNordic.current

    BackHandler(enabled = editing) { vm.setEditing(false) }
    DisposableEffect(Unit) { onDispose { vm.setEditing(false) } }

    fun tap(app: AppEntry) = when {
        editing -> vm.toggleHidden(app.id)
        app.route != null -> onRoute(app.route)
        else -> vm.launch(app)
    }

    Column(Modifier.fillMaxSize().sharedSheet(SharedKeys.SHEET_APPS).background(n.bg)) {
        PageHeader("Apps", onBack) {
            Row(
                Modifier.size(340.dp, 48.dp).clip(RoundedCornerShape(24.dp)).background(n.fill3).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Sym("search", tint = n.label2)
                BasicTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    singleLine = true,
                    textStyle = nordicText(17).copy(color = n.label),
                    cursorBrush = SolidColor(n.accent),
                    modifier = Modifier.weight(1f),
                    decorationBox = { field ->
                        if (query.isEmpty()) NText("Buscar", nordicText(17), color = n.label2)
                        field()
                    },
                )
            }
            NText(
                if (editing) "Concluir" else "Editar",
                nordicText(17, FontWeight.SemiBold, align = TextAlign.End),
                color = n.accent,
                modifier = Modifier.widthIn(min = 92.dp).pressClick { vm.setEditing(!editing) },
            )
        }
        val base = LocalViewConfiguration.current
        val longPress550 = object : ViewConfiguration by base {
            override val longPressTimeoutMillis: Long = 550
        }
        CompositionLocalProvider(LocalViewConfiguration provides longPress550) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                section("Seus apps", "mine", sections.mine, editing, false, ::tap) { vm.setEditing(true) }
                section("Do carro", "car", sections.car, editing, false, ::tap) { vm.setEditing(true) }
                section("Ocultos", "hidden", sections.hidden, editing, true, ::tap) { vm.setEditing(true) }
            }
        }
    }
}

private fun LazyGridScope.section(
    title: String,
    key: String,
    apps: List<AppEntry>,
    editing: Boolean,
    hidden: Boolean,
    onTap: (AppEntry) -> Unit,
    onLongPress: () -> Unit,
) {
    if (apps.isEmpty()) return
    item(key = "header-$key", span = { GridItemSpan(maxLineSpan) }) {
        NText(
            title,
            nordicText(15, FontWeight.SemiBold),
            color = LocalNordic.current.label2,
            modifier = Modifier.animateItem().padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
        )
    }
    itemsIndexed(apps, key = { _, app -> app.id }) { i, app ->
        AppIcon(app, i, editing, hidden, onTap = { onTap(app) }, onLongPress = onLongPress, modifier = Modifier.animateItem())
    }
}
