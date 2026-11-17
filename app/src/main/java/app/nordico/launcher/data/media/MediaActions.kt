package app.nordico.launcher.data.media

/** Ação extra do player (aleatório, repetir, curtir). `id` é o original, para devolver ao app ao tocar. */
data class MediaAction(val id: String, val label: String, val glyph: String, val active: Boolean)

/**
 * Reconhece as ações personalizadas da MediaSession pelo id ou pelo nome. Ações desconhecidas são
 * ignoradas: o botão só aparece quando o app realmente oferece a função.
 */
// Trade-off: heurística por palavra-chave; o Spotify costuma oferecer `..._ON`/`..._OFF`, onde `_OFF`
// significa que a função está ligada agora. Confirmar os ids reais na central e ajustar.
fun classifyAction(id: String, name: CharSequence?): MediaAction? {
    if (id.isBlank()) return null
    val key = "$id ${name ?: ""}".lowercase()
    val active = id.uppercase().endsWith("_OFF")
    return when {
        "shuffle" in key -> MediaAction(id, "Aleatório", "shuffle", active)
        "repeat" in key -> MediaAction(id, "Repetir", "repeat", active)
        listOf("like", "heart", "favorite", "favourite", "thumb").any { it in key } -> MediaAction(id, "Curtir", "favorite", active)
        else -> null
    }
}
