package com.example.worktimer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent as AKE
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ---------- Activity ---------- */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { Theme { Root() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handle(intent) }
    private fun handle(i: Intent?) {
        when (i?.action) { "wt.RESUME" -> Store.resumeLast(); "wt.STOP" -> Store.stop() }
    }
}

/* ---------- ערכת נושא ורכיבים ---------- */
val Teal = Color(0xFF0F766E)
val Palette = listOf(0xFFE8590C, 0xFF1E88E5, 0xFF7E57C2, 0xFF43A047, 0xFFD81B60)

@Composable
fun Theme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Color(0xFF5EEAD4), background = Color(0xFF101816), surfaceContainer = Color(0xFF1E2A28))
    else lightColorScheme(primary = Teal, background = Color(0xFFF4F6F4), surfaceContainer = Color.White)
    MaterialTheme(colorScheme = scheme) { Surface(color = MaterialTheme.colorScheme.background, content = content) }
}

private fun ico(p: String, w: Float = 2f) = ImageVector.Builder(
    defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).addPath(PathParser().parsePathString(p).toNodes(), stroke = SolidColor(Color.Black), strokeLineWidth = w,
    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

val IHome = ico("M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z")
val IChart = ico("M5 21V10M12 21V3M19 21v-7")
val IFolder = ico("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
val IDots = ico("M5 12h.01M12 12h.01M19 12h.01", 3.5f)
val IBack = ico("M5 12h14M12 5l7 7-7 7")

@Composable
fun Modifier.ring(): Modifier {
    var f by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme.primary
    return this.onFocusChanged { f = it.isFocused }.drawWithContent {
        drawContent()
        if (f) drawRoundRect(c, cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun Cd(mod: Modifier = Modifier, h: Dp = 44.dp, content: @Composable RowScope.() -> Unit) =
    Row(
        mod.fillMaxWidth().height(h).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content
    )

@Composable
fun Btn(text: String, filled: Boolean = true, mod: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        mod.ring().height(44.dp).clip(shape)
            .then(if (filled) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier.background(Color.Transparent))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (filled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary)
    }
}

@Composable fun Sub(t: String) = Text(t, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
@Composable fun Dot(c: Color) = Box(Modifier.size(10.dp).background(c, CircleShape))
@Composable
fun Bar(frac: Float, c: Color = MaterialTheme.colorScheme.primary) = Box(
    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.copy(alpha = .2f))
) { Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(c)) }

/* ---------- ניווט ---------- */
data class Dest(val k: String, val id: String? = null, val n: Int = 0)

@Composable
fun Root() {
    val s by Store.state.collectAsState()
    val undo by Store.undo.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.runningId) {
        now = System.currentTimeMillis()
        while (s.runningId != null) { delay(1000); now = System.currentTimeMillis() }
    }
    LaunchedEffect(undo) { if (undo != null) { delay(6000); Store.clearUndo() } }
    var tab by remember { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Dest>() }
    var pending by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    BackHandler(stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }

    fun toggle(id: String) {
        if (s.runningId != null && s.runningId != id && s.settings.confirm) pending = id
        else { Store.toggle(id); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
    val go: (Dest) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    val top = stack.lastOrNull()

    Column(Modifier.fillMaxSize()) {
        if (top == null) {
            Row(Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceContainer)) {
                listOf(IHome to "בית", IChart to "סיכום", IFolder to "פרויקטים", IDots to "עוד").forEachIndexed { i, (ic, l) ->
                    val on = tab == i
                    Box(
                        Modifier.weight(1f).fillMaxHeight().ring().clickable { tab = i }.semantics { contentDescription = l },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ic, null, tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (on) Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp).fillMaxWidth().height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(40.dp).ring().clickable(onClick = back).semantics { contentDescription = "חזרה" }, contentAlignment = Alignment.Center) { Icon(IBack, null) }
                Text(title(top, s), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.weight(1f)) {
            when (top?.k) {
                null -> when (tab) {
                    0 -> Home(s, now, ::toggle, go)
                    1 -> Summary(s, now)
                    2 -> Projects(s, now, go)
                    else -> More(go)
                }
                "detail" -> Detail(s, now, top!!.id!!, go, back)
                "add" -> AddProject { stack.removeAt(stack.lastIndex) }
                "manual" -> Manual(top!!.id!!) { stack.removeAt(stack.lastIndex) }
                "settings" -> SettingsScreen(s)
                "guide" -> Guide(go)
                "guidet" -> GuideTopic(top!!.n)
                "backup" -> Backup(go)
            }
        }
        if (undo != null) Row(
            Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF12201E)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("בוצע", color = Color.White, fontSize = 14.sp)
            Box(Modifier.ring().clickable { Store.undoNow() }.padding(8.dp)) { Text("בטל", color = Color(0xFF5EEAD4), fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
    pending?.let { id ->
        val from = s.projects.firstOrNull { it.id == s.runningId }?.name ?: ""
        val to = s.projects.firstOrNull { it.id == id }?.name ?: ""
        AlertDialog(
            onDismissRequest = { pending = null },
            text = { Text("הטיימר ״$from״ רץ (${fmt(now - s.runningStart)}). להפסיק אותו ולהתחיל ״$to״?", fontSize = 14.sp) },
            confirmButton = { TextButton({ Store.toggle(id); pending = null }) { Text("החלף") } },
            dismissButton = { TextButton({ pending = null }) { Text("ביטול") } }
        )
    }
}

fun title(d: Dest, s: AppState) = when (d.k) {
    "detail" -> s.projects.firstOrNull { it.id == d.id }?.name ?: ""
    "add" -> "פרויקט חדש"; "manual" -> "הוספת זמן"; "settings" -> "הגדרות"
    "guide" -> "מדריך"; "guidet" -> GUIDE[d.n].first; "backup" -> "גיבוי ושחזור"; else -> ""
}

/* ---------- בית ---------- */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjRow(i: Int, p: Project, s: AppState, now: Long, toggle: () -> Unit, go: (Dest) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val c = Color(p.color); val run = s.runningId == p.id
    Box {
        Cd(Modifier.ring().combinedClickable(onClick = toggle, onLongClick = { menu = true })) {
            Box(Modifier.size(20.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) { Text("${i + 1}", fontSize = 12.sp) }
            Dot(c)
            Text(p.name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(fmt(s.millis(p, dayStart(), Long.MAX_VALUE, now)), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.size(38.dp).background(c, CircleShape).focusProperties { canFocus = false }.clickable(onClick = toggle)
                    .semantics { contentDescription = if (run) "עצור ${p.name}" else "התחל ${p.name}" },
                contentAlignment = Alignment.Center
            ) {
                if (run) Box(Modifier.size(14.dp).background(Color.White, RoundedCornerShape(3.dp)))
                else Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text("פתח פרויקט") }, { menu = false; go(Dest("detail", p.id)) })
            DropdownMenuItem({ Text(if (p.pinned) "בטל הצמדה" else "הצמד") }, { menu = false; Store.pin(p.id) })
            DropdownMenuItem({ Text("ארכוב") }, { menu = false; Store.archive(p.id, true) })
            DropdownMenuItem({ Text("מחיקה") }, { menu = false; Store.delete(p.id) })
        }
    }
}

@Composable
fun Home(s: AppState, now: Long, onToggle: (String) -> Unit, go: (Dest) -> Unit) {
    val list = s.projects.filter { !it.archived }.sortedByDescending { it.pinned }
    val today = s.total(dayStart(), Long.MAX_VALUE, now)
    val run = s.projects.firstOrNull { it.id == s.runningId }
    var dismissed by remember { mutableStateOf(false) }
    var fix by remember { mutableStateOf(false) }
    val long = run != null && s.settings.longH > 0 && now - s.runningStart > s.settings.longH * 3_600_000L && !dismissed
    LazyColumn(
        Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            val d = e.nativeKeyEvent.keyCode - AKE.KEYCODE_1
            if (e.type == KeyEventType.KeyUp && d in 0..8 && d < list.size) { onToggle(list[d].id); true } else false
        },
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (long) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF1D6)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("הטיימר רץ ${fmt(now - s.runningStart)} שעות", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF12201E))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Btn("השאר", false, Modifier.weight(1f)) { dismissed = true }
                    Btn("תקן", false, Modifier.weight(1f)) { fix = true }
                    Btn("עצור", true, Modifier.weight(1f)) { Store.stop() }
                }
            }
        }
        if (!s.settings.tourDone && list.isNotEmpty()) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFE1F1EE)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ברוכים הבאים! סיור קצר של דקה?", fontSize = 13.sp, color = Color(0xFF12201E))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Btn("אחר כך", false, Modifier.weight(1f)) { Store.settings { it.copy(tourDone = true) } }
                    Btn("התחל", true, Modifier.weight(1f)) { Store.settings { it.copy(tourDone = true) }; go(Dest("guidet", n = 0)) }
                }
            }
        }
        if (run != null) item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF0F3D39)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(run.name, fontSize = 12.sp, color = Color.White.copy(alpha = .85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fmt(now - s.runningStart, true), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(
                        Modifier.size(44.dp).ring().background(Color.White, CircleShape).clickable { Store.stop() }.semantics { contentDescription = "עצור טיימר" },
                        contentAlignment = Alignment.Center
                    ) { Box(Modifier.size(15.dp).background(Color(0xFF0F3D39), RoundedCornerShape(3.dp))) }
                }
                var note by remember(s.runningStart) { mutableStateOf(s.runningNote) }
                OutlinedTextField(
                    note, { note = it; Store.setNote(it) }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color.White),
                    placeholder = { Text("הערה (לא חובה)", fontSize = 13.sp, color = Color.White.copy(alpha = .6f)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (run == null) Sub("אין טיימר פעיל · היום ${fmt(today)} מתוך ${fmt(s.settings.dailyMin * 60_000L)}")
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Sub("היום ${fmt(today)} מתוך ${fmt(s.settings.dailyMin * 60_000L)}"); Sub("${(100 * today / (s.settings.dailyMin * 60_000L)).coerceAtMost(999)}%")
                }
                Bar(today.toFloat() / (s.settings.dailyMin * 60_000L))
            }
        }
        if (list.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("עוד אין פרויקטים", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Sub("הוסיפו פרויקט ראשון והתחילו למדוד זמן")
                Btn("+ פרויקט ראשון", true, Modifier.fillMaxWidth()) { go(Dest("add")) }
                Btn("מדריך קצר", false, Modifier.fillMaxWidth()) { go(Dest("guidet", n = 0)) }
            }
        }
        itemsIndexed(list, key = { _, p -> p.id }) { i, p -> ProjRow(i, p, s, now, { onToggle(p.id) }, go) }
    }
    if (fix) {
        var m by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { fix = false }, title = { Text("כמה דקות עבדתי בפועל?", fontSize = 16.sp) },
            text = { OutlinedTextField(m, { m = it.filter(Char::isDigit) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = { TextButton({ m.toLongOrNull()?.let { Store.stopAt(s.runningStart + it * 60_000L) }; fix = false }) { Text("שמור") } },
            dismissButton = { TextButton({ fix = false }) { Text("ביטול") } }
        )
    }
}

/* ---------- סיכום ---------- */
@Composable
fun Summary(s: AppState, now: Long) {
    var month by remember { mutableStateOf(false) }
    val from = if (month) monthStart() else weekStart()
    val to = if (month) monthStart(1) else weekStart(1)
    val pFrom = if (month) monthStart(-1) else weekStart(-1)
    val n = if (month) 5 else 7
    val bars = (0 until n).map { i -> s.total(addDays(from, 7 * i * (if (month) 1 else 0) + if (month) 0 else i), if (month) minOf(addDays(from, 7 * (i + 1)), to) else addDays(from, i + 1), now) }
    val cur = s.total(from, to, now); val prev = s.total(pFrom, from, now)
    val mx = (bars.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val first = java.util.Calendar.getInstance().firstDayOfWeek
    val top = s.projects.maxByOrNull { s.millis(it, from, to, now) }
    val diff = cur - prev
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(3.dp)) {
            listOf("שבוע" to false, "חודש" to true).forEach { (l, m) ->
                Box(
                    Modifier.weight(1f).ring().clip(RoundedCornerShape(13.dp)).background(if (month == m) MaterialTheme.colorScheme.background else Color.Transparent)
                        .clickable { month = m }.padding(6.dp), contentAlignment = Alignment.Center
                ) { Text(l, fontSize = 13.sp, fontWeight = if (month == m) FontWeight.Bold else FontWeight.Normal) }
            }
        }
        Column {
            Text(fmt(cur), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text((if (diff >= 0) "+" else "-") + fmt(kotlin.math.abs(diff)) + " לעומת ה" + (if (month) "חודש" else "שבוע") + " שעבר", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp, 8.dp, 8.dp, 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom)) {
                    Box(Modifier.fillMaxWidth().height((70.dp * v.toFloat() / mx.toFloat()).coerceAtLeast(2.dp)).clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (v == mx && v > 0) 1f else .45f)))
                    Sub(if (month) "ש${i + 1}" else "אבגדהוש"[(first - 1 + i) % 7].toString())
                }
            }
        }
        if (top != null && s.millis(top, from, to, now) > 0) Cd(h = 40.dp) {
            Dot(Color(top.color)); Text("הכי הרבה: ${top.name}", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(fmt(s.millis(top, from, to, now)), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ---------- פרויקטים ---------- */
@Composable
fun Projects(s: AppState, now: Long, go: (Dest) -> Unit) {
    var showArch by remember { mutableStateOf(false) }
    val live = s.projects.filter { !it.archived }.sortedByDescending { it.pinned }
    val arch = s.projects.filter { it.archived }
    val w0 = weekStart()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(if (showArch) live + arch else live, key = { _, p -> p.id }) { _, p ->
            val wk = s.millis(p, w0, Long.MAX_VALUE, now)
            Column(
                Modifier.fillMaxWidth().ring().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { go(Dest("detail", p.id)) }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dot(Color(p.color))
                    Column(Modifier.weight(1f)) {
                        Text(p.name + if (p.archived) " (מאורכב)" else "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Sub(if (p.goalMin > 0) "יעד ${p.goalMin / 60} שעות" else "ללא יעד")
                    }
                    Text(fmt(wk), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                if (p.goalMin > 0) Bar(wk.toFloat() / (p.goalMin * 60_000L), Color(p.color))
            }
        }
        if (arch.isNotEmpty()) item {
            Box(Modifier.fillMaxWidth().ring().clickable { showArch = !showArch }.padding(8.dp), contentAlignment = Alignment.Center) { Sub(if (showArch) "הסתר מאורכבים" else "מאורכבים (${arch.size})") }
        }
        item { Btn("+ פרויקט חדש", true, Modifier.fillMaxWidth()) { go(Dest("add")) } }
    }
}

@Composable
fun Detail(s: AppState, now: Long, id: String, go: (Dest) -> Unit, back: () -> Unit) {
    val p = s.projects.firstOrNull { it.id == id } ?: run { LaunchedEffect(Unit) { back() }; return }
    val f = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val d0 = dayStart(); val w0 = weekStart()
    val idx = p.sessions.indices.reversed().take(30)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("היום" to s.millis(p, d0, Long.MAX_VALUE, now), "השבוע" to s.millis(p, w0, Long.MAX_VALUE, now), "סה״כ" to s.millis(p, 0, Long.MAX_VALUE, now)).forEach { (l, v) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fmt(v), fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Sub(l)
                    }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Btn("+ זמן", false, Modifier.weight(1f)) { go(Dest("manual", id)) }
            Btn(if (p.archived) "שחזור" else "ארכוב", false, Modifier.weight(1f)) { Store.archive(id, !p.archived) }
        } }
        item { Sub("לחיצה על סשן מוחקת אותו (ניתן לבטל)") }
        items(idx.size) { k ->
            val i = idx[k]; val x = p.sessions[i]
            Cd(Modifier.ring().clickable { Store.deleteSession(id, i) }, h = 44.dp) {
                Column(Modifier.weight(1f)) {
                    Text(f.format(Date(x.start)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (x.note.isNotEmpty()) Sub(x.note)
                }
                Text(fmt(x.end - x.start), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AddProject(done: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var col by remember { mutableStateOf(Palette[0]) }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("שם הפרויקט") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(goal, { goal = it.filter(Char::isDigit).take(3) }, singleLine = true, label = { Text("יעד שבועי בשעות (לא חובה)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Palette.forEach { c ->
                Box(Modifier.size(36.dp).ring().background(Color(c), CircleShape).clickable { col = c }.semantics { contentDescription = "צבע" }, contentAlignment = Alignment.Center) {
                    if (c == col) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Btn("שמור", true, Modifier.fillMaxWidth()) { if (name.isNotBlank()) { Store.addProject(name, col, goal.toIntOrNull() ?: 0); done() } }
    }
}

@Composable
fun Manual(id: String, done: () -> Unit) {
    var h by remember { mutableStateOf("") }
    var m by remember { mutableStateOf("") }
    fun plus(x: Int) { val t = (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0) + x; h = (t / 60).toString(); m = (t % 60).toString() }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(h, { h = it.filter(Char::isDigit).take(3) }, singleLine = true, label = { Text("שעות") }, modifier = Modifier.weight(1f), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(m, { m = it.filter(Char::isDigit).take(2) }, singleLine = true, label = { Text("דקות") }, modifier = Modifier.weight(1f), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 30, 60).forEach { x -> Btn("+$x", false, Modifier.weight(1f)) { plus(x) } }
        }
        Spacer(Modifier.weight(1f))
        Btn("הוסף", true, Modifier.fillMaxWidth()) {
            val t = (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0)
            if (t > 0) { Store.addManual(id, t); done() }
        }
    }
}

/* ---------- עוד / הגדרות / גיבוי ---------- */
@Composable
fun Row2(text: String, end: String = "←", focus: Boolean = false, onClick: () -> Unit) =
    Cd(Modifier.ring().clickable(onClick = onClick), h = 44.dp) { Text(text, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Sub(end) }

@Composable
fun More(go: (Dest) -> Unit) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row2("הגדרות") { go(Dest("settings")) }
        Row2("מדריך") { go(Dest("guide")) }
        Row2("גיבוי ושחזור") { go(Dest("backup")) }
        Row2("אודות", "גרסה 1.0") {}
    }
}

@Composable
fun SettingsScreen(s: AppState) {
    val st = s.settings
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row2("שאלה לפני החלפת טיימר", if (st.confirm) "פועל" else "כבוי") { Store.settings { it.copy(confirm = !it.confirm) } }
        Row2("התראה על טיימר ארוך", if (st.longH == 0) "כבוי" else "${st.longH} שעות") {
            val o = listOf(4, 8, 12, 0); Store.settings { it.copy(longH = o[(o.indexOf(it.longH) + 1) % o.size]) }
        }
        Row2("יעד יומי", fmt(st.dailyMin * 60_000L)) {
            val o = listOf(240, 360, 480); Store.settings { it.copy(dailyMin = o[(o.indexOf(it.dailyMin) + 1) % o.size]) }
        }
        Row2("הצג סיור שוב", "") { Store.settings { it.copy(tourDone = false) } }
        Sub("לחיצה על שורה משנה את ההגדרה")
    }
}

private fun write(ctx: Context, u: Uri, text: String) { ctx.contentResolver.openOutputStream(u)?.use { it.write(text.toByteArray()) } }

@Composable
fun Backup(go: (Dest) -> Unit) {
    val ctx = LocalContext.current
    var msg by remember { mutableStateOf<String?>(null) }
    var bad by remember { mutableStateOf(false) }
    val expJ = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> u?.let { write(ctx, it, Store.exportJson()); msg = "הגיבוי נשמר"; bad = false } }
    val expC = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u -> u?.let { write(ctx, it, Store.exportCsv()); msg = "הקובץ נשמר"; bad = false } }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let {
            try {
                Store.restore(ctx.contentResolver.openInputStream(it)!!.bufferedReader().readText()); msg = "השחזור הושלם (ניתן לבטל)"; bad = false
            } catch (e: Exception) { msg = "הקובץ לא תקין"; bad = true }
        }
    }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Btn("שמירת גיבוי", true, Modifier.fillMaxWidth()) { expJ.launch("worktimer-backup.json") }
        Btn("שחזור מגיבוי", false, Modifier.fillMaxWidth()) { imp.launch(arrayOf("*/*")) }
        Btn("ייצוא ל-CSV", false, Modifier.fillMaxWidth()) { expC.launch("worktimer.csv") }
        msg?.let {
            Text(it, fontSize = 13.sp, color = if (bad) Color(0xFFB3261E) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (bad) Btn("מדריך: גיבוי ושחזור ←", false, Modifier.fillMaxWidth()) { go(Dest("guidet", n = 4)) }
        }
    }
}

/* ---------- מדריך ---------- */
val GUIDE = listOf(
    "התחלה מהירה" to "1. בלשונית ״פרויקטים״ (תיקייה) לחצו ״פרויקט חדש״ והקלידו שם.\n2. בבית לחצו על הפרויקט (או OK) כדי להתחיל טיימר.\n3. לעצירה לחצו על הכפתור הלבן בכרטיס הכהה למעלה.\n4. הזמן נשמר אוטומטית, ומופיע בסיכום.\n5. אפשר לכתוב הערה קצרה בזמן העבודה.",
    "מקשים" to "חצים: מעבר בין פריטים\nOK: התחלה או עצירה\nספרות 1–9: הפעלת הפרויקט עם אותו מספר\nלחיצה ארוכה על שורה: תפריט (פתיחה, הצמדה, ארכוב, מחיקה)\nחזרה: חזרה למסך הקודם",
    "אייקונים" to "בית: טיימר ורשימת הפרויקטים\nגרף: סיכום שבועי וחודשי\nתיקייה: ניהול פרויקטים ויעדים\nשלוש נקודות: הגדרות, מדריך וגיבוי",
    "טעויות ותיקונים" to "אחרי כל עצירה, מחיקה או ארכוב מופיע פס תחתון עם ״בטל״ למשך 6 שניות.\nשכחתי לעצור: אם הטיימר רץ יותר מהזמן שהוגדר, יופיע פס בבית. בחרו ״תקן״ והזינו כמה דקות עבדתם בפועל.\nשכחתי להפעיל: במסך הפרויקט לחצו ״+ זמן״.\nסשן שגוי: במסך הפרויקט לחצו עליו כדי למחוק.",
    "גיבוי ושחזור" to "עוד ← גיבוי ושחזור.\nשמירת גיבוי: יוצר קובץ שמאפשר לשחזר הכול.\nשחזור: בחרו את קובץ הגיבוי. אפשר לבטל מיד אחרי השחזור.\nייצוא CSV: קובץ לפתיחה באקסל.\nמומלץ לגבות לפני כל עדכון של האפליקציה.",
    "עדכון האפליקציה" to "1. ב-GitHub העלו את הקבצים החדשים (Add file ← Upload files).\n2. בלשונית Actions המתינו שהבנייה תסתיים ושייסמן וי ירוק.\n3. הורידו את הקובץ WorkTimer-apk.\n4. התקינו אותו מעל הקיים. אל תסירו את האפליקציה: החתימה קבועה, ולכן הנתונים נשמרים.\nגבו קודם, לביטחון.",
    "פתרון בעיות" to "ההתקנה נכשלה: ודאו שהתקנה מקבצים מותרת, ושלא הותקנה גרסה עם חתימה שונה (אז יש להסיר ולשחזר מגיבוי).\nהשעון לא רץ בהתראה: אשרו התראות לאפליקציה בהגדרות המכשיר.\nמקשי הספרות לא עובדים: לחצו קודם על חץ למטה כדי שאחד הפריטים יקבל פוקוס.\nטקסט קטוע: הקטינו את גודל הגופן במערכת."
)

@Composable
fun Guide(go: (Dest) -> Unit) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GUIDE.forEachIndexed { i, g -> Row2(g.first) { go(Dest("guidet", n = i)) } }
    }
}

@Composable
fun GuideTopic(n: Int) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item { Text(GUIDE[n].second, fontSize = 14.sp, lineHeight = 22.sp) }
    }
}
