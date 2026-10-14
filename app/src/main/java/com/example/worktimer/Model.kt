package com.example.worktimer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Session(val start: Long, val end: Long, val note: String = "")
data class Project(
    val id: String, val name: String, val color: Long, val sessions: List<Session> = emptyList(),
    val goalMin: Int = 0, val archived: Boolean = false, val pinned: Boolean = false
)
data class Settings(val confirm: Boolean = true, val longH: Int = 8, val dailyMin: Int = 360, val tourDone: Boolean = false)
data class AppState(
    val projects: List<Project> = emptyList(), val runningId: String? = null,
    val runningStart: Long = 0, val runningNote: String = "", val settings: Settings = Settings()
)

private fun overlap(a: Long, b: Long, from: Long, to: Long) = maxOf(0L, minOf(b, to) - maxOf(a, from))

fun AppState.millis(p: Project, from: Long, to: Long, now: Long): Long =
    p.sessions.sumOf { overlap(it.start, it.end, from, to) } +
        if (runningId == p.id) overlap(runningStart, now, from, to) else 0L

fun AppState.total(from: Long, to: Long, now: Long) = projects.sumOf { millis(it, from, to, now) }

fun dayStart(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun addDays(t: Long, d: Int): Long = Calendar.getInstance().apply { timeInMillis = t; add(Calendar.DAY_OF_YEAR, d) }.timeInMillis

fun weekStart(offset: Int = 0): Long {
    val c = Calendar.getInstance().apply { timeInMillis = dayStart() }
    while (c.get(Calendar.DAY_OF_WEEK) != c.firstDayOfWeek) c.add(Calendar.DAY_OF_YEAR, -1)
    c.add(Calendar.DAY_OF_YEAR, 7 * offset)
    return c.timeInMillis
}

fun monthStart(offset: Int = 0): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart(); set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset)
}.timeInMillis

fun fmt(ms: Long, seconds: Boolean = false): String {
    val t = ms / 1000
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (seconds) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(h, m)
}

fun AppState.toJson(): JSONObject = JSONObject().apply {
    put("v", 1); put("run", runningId ?: ""); put("rs", runningStart); put("rn", runningNote)
    put("set", JSONObject().put("c", settings.confirm).put("l", settings.longH).put("d", settings.dailyMin).put("t", settings.tourDone))
    put("p", JSONArray().also { arr ->
        projects.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("n", p.name).put("c", p.color).put("g", p.goalMin)
                .put("a", p.archived).put("pi", p.pinned)
                .put("s", JSONArray().also { sa -> p.sessions.forEach { sa.put(JSONArray().put(it.start).put(it.end).put(it.note)) } }))
        }
    })
}

fun parseState(str: String): AppState {
    val o = JSONObject(str)
    val ps = o.optJSONArray("p") ?: JSONArray()
    val st = o.optJSONObject("set")
    val d = Settings()
    return AppState(
        (0 until ps.length()).map { i ->
            val p = ps.getJSONObject(i); val ss = p.getJSONArray("s")
            Project(p.getString("id"), p.getString("n"), p.getLong("c"),
                (0 until ss.length()).map { j -> val a = ss.getJSONArray(j); Session(a.getLong(0), a.getLong(1), a.optString(2, "")) },
                p.optInt("g", 0), p.optBoolean("a", false), p.optBoolean("pi", false))
        },
        o.optString("run").ifEmpty { null }, o.optLong("rs"), o.optString("rn", ""),
        if (st == null) d else Settings(st.optBoolean("c", d.confirm), st.optInt("l", d.longH), st.optInt("d", d.dailyMin), st.optBoolean("t", d.tourDone))
    )
}

class App : Application() {
    override fun onCreate() { super.onCreate(); Store.init(this) }
}

object Store {
    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val _s = MutableStateFlow(AppState())
    private val _undo = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState> = _s.asStateFlow()
    val undo: StateFlow<AppState?> = _undo.asStateFlow()

    fun init(c: Context) {
        app = c.applicationContext
        prefs = app.getSharedPreferences("wt", Context.MODE_PRIVATE)
        _s.value = try { parseState(prefs.getString("data", "")!!) } catch (e: Exception) { AppState() }
        Notifier.sync(app, _s.value)
    }

    private fun persist(n: AppState) {
        _s.value = n
        prefs.edit().putString("data", n.toJson().toString()).apply()
        Notifier.sync(app, n)
    }
    private fun upd(undoable: Boolean = false, f: (AppState) -> AppState) {
        val old = _s.value
        val n = f(old)
        if (undoable) _undo.value = old
        persist(n)
    }
    private fun stopped(s: AppState, end: Long): AppState =
        if (s.runningId == null) s else s.copy(
            projects = s.projects.map {
                if (it.id == s.runningId && end > s.runningStart) it.copy(sessions = it.sessions + Session(s.runningStart, end, s.runningNote.trim())) else it
            }, runningId = null, runningNote = "")

    fun toggle(id: String) = upd(true) { s ->
        val now = System.currentTimeMillis(); val cur = stopped(s, now)
        if (s.runningId == id) cur else cur.copy(runningId = id, runningStart = now, runningNote = "")
    }
    fun stop() = upd(true) { stopped(it, System.currentTimeMillis()) }
    fun stopAt(end: Long) = upd(true) { stopped(it, end) }
    fun setNote(t: String) = upd { it.copy(runningNote = t) }
    fun addProject(name: String, color: Long, goalH: Int) = upd {
        it.copy(projects = it.projects + Project(UUID.randomUUID().toString(), name.trim(), color, goalMin = goalH * 60))
    }
    fun archive(id: String, v: Boolean) = upd(true) { s ->
        val x = if (s.runningId == id) stopped(s, System.currentTimeMillis()) else s
        x.copy(projects = x.projects.map { if (it.id == id) it.copy(archived = v) else it })
    }
    fun delete(id: String) = upd(true) { s ->
        val x = if (s.runningId == id) stopped(s, System.currentTimeMillis()) else s
        x.copy(projects = x.projects.filter { it.id != id })
    }
    fun pin(id: String) = upd { s -> s.copy(projects = s.projects.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }) }
    fun addManual(id: String, min: Int) = upd { s ->
        val now = System.currentTimeMillis()
        s.copy(projects = s.projects.map { if (it.id == id) it.copy(sessions = it.sessions + Session(now - min * 60_000L, now, "הוספה ידנית")) else it })
    }
    fun deleteSession(pid: String, idx: Int) = upd(true) { s ->
        s.copy(projects = s.projects.map { if (it.id == pid) it.copy(sessions = it.sessions.filterIndexed { i, _ -> i != idx }) else it })
    }
    fun settings(f: (Settings) -> Settings) = upd { it.copy(settings = f(it.settings)) }
    fun restore(str: String) { val n = parseState(str); upd(true) { n } }
    fun exportJson(): String = _s.value.toJson().toString(2)
    fun exportCsv(): String {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder("project,start,end,minutes,note\n")
        _s.value.projects.forEach { p ->
            p.sessions.forEach {
                sb.append("\"${p.name.replace("\"", "\"\"")}\",${f.format(Date(it.start))},${f.format(Date(it.end))},${(it.end - it.start) / 60000},\"${it.note.replace("\"", "\"\"")}\"\n")
            }
        }
        return sb.toString()
    }
    fun resumeLast() {
        val s = _s.value
        if (s.runningId != null) return
        val live = s.projects.filter { !it.archived }
        val p = live.maxByOrNull { it.sessions.maxOfOrNull { x -> x.end } ?: 0L } ?: return
        toggle(p.id)
    }
    fun undoNow() { _undo.value?.let { persist(it); _undo.value = null } }
    fun clearUndo() { _undo.value = null }
}
