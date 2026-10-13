package mx.pagos.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Store.load(this)
        setContent { AppTheme { Root(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
        EdgeHandleService.sync(this)
    }
}

@Composable
private fun Root(tick: Int) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var filterState by remember { mutableStateOf(Filter()) }
    var accent by remember { mutableStateOf<Color?>(null) }
    var settings by remember { mutableStateOf(false) }
    var budgetFor by remember { mutableStateOf<String?>(null) }
    var addingAccount by remember { mutableStateOf(false) }

    // Si se borró la cuenta filtrada, se vuelve a "Todas las cuentas".
    val filter = filterState.account?.let { a ->
        if (a in Config.accounts) filterState else filterState.copy(account = null)
    } ?: filterState
    val setFilter: (Filter) -> Unit = { filterState = it }
    val openSettings = { settings = true }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val top = when (tab) {
        0 -> Ui.TopActivity
        1 -> accent?.let { lerp(it, Color.Black, 0.12f) } ?: Ui.TopSummary
        2 -> Ui.TopBudget
        3 -> Ui.TopAnalysis
        else -> Ui.TopAccounts
    }

    BackHandler(enabled = settings) { settings = false }

    CompositionLocalProvider(LocalBottomPad provides navInset + 100.dp) {
        AppBackground(top) {
            Crossfade(targetState = tab, animationSpec = tween(260), label = "pestaña") { t ->
                when (t) {
                    0 -> ActivityScreen(filter, setFilter, openSettings)
                    1 -> SummaryScreen(filter, setFilter, { accent = it }, openSettings)
                    2 -> BudgetScreen(openSettings) { budgetFor = it }
                    3 -> AnalysisScreen(filter, setFilter, openSettings)
                    else -> AccountsScreen(openSettings) { addingAccount = true }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = navInset + 92.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xF2F2E8D8))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { ctx.startActivity(Intent(ctx, QuickCaptureActivity::class.java)) },
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(R.drawable.ic_add, Color(0xFF173A86), 24.dp)
            }

            NavBar(
                selected = tab,
                onSelect = {
                    if (it != tab) accent = null
                    tab = it
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navInset + 10.dp),
            )

            AnimatedVisibility(
                visible = settings,
                enter = slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutHorizontally(tween(250)) { it } + fadeOut(tween(250)),
            ) {
                AppBackground(Ui.TopAnalysis) {
                    SettingsScreen(tick, onBack = { settings = false })
                }
            }

            budgetFor?.let { c ->
                AmountPrompt(
                    title = "Presupuesto para $c",
                    initial = Config.budgets[c]?.let { plainAmount(it) } ?: "",
                    onDismiss = { budgetFor = null },
                    onConfirm = {
                        Config.setBudget(ctx, c, it)
                        budgetFor = null
                    },
                )
            }

            if (addingAccount) {
                TextPrompt(
                    title = "Nueva cuenta",
                    placeholder = "Nombre de la cuenta",
                    onDismiss = { addingAccount = false },
                    onConfirm = {
                        Config.addAccount(ctx, it)
                        addingAccount = false
                    },
                )
            }
        }
    }
}
