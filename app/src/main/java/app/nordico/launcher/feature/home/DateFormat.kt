package app.nordico.launcher.feature.home

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// forLanguageTag: Locale.of() não existe no Android 14 (API 34).
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", PT_BR)

/** "domingo, 15 de novembro" */
fun formatDate(d: LocalDate): String = d.format(DATE)

/** "09:05" */
fun formatClock(t: LocalTime): String = t.format(CLOCK)
