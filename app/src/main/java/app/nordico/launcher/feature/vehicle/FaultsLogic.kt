package app.nordico.launcher.feature.vehicle

import app.nordico.launcher.data.obd.ClearBlock
import app.nordico.launcher.data.obd.ClearOutcome
import app.nordico.launcher.data.obd.Severity
import app.nordico.launcher.data.prefs.ClearResult
import java.net.URLEncoder

/** Modelo do carro nas buscas de vídeo. Constante única para trocar num lugar só. */
const val VEHICLE_QUERY = "Honda Civic"

fun faultsSummary(count: Int, milOn: Boolean?): String = when {
    count == 0 -> "Nenhuma falha registrada"
    else -> "$count ${if (count == 1) "falha" else "falhas"}" + if (milOn == true) " · luz do motor acesa" else ""
}

fun severityLabel(s: Severity): String = when (s) {
    Severity.Ok -> "Pode seguir"
    Severity.Attention -> "Atenção"
    Severity.Stop -> "Pare e procure ajuda"
}

/** Busca no YouTube pelo código e o modelo do carro; abre no app ou navegador da central. */
fun videoUrl(code: String, vehicle: String): String =
    "https://www.youtube.com/results?search_query=" + URLEncoder.encode("$code $vehicle como resolver", "UTF-8")

/** Vídeo distrai: só com a velocidade lida e igual a zero. */
fun canOpenVideo(speedKmh: Int?): Boolean = speedKmh == 0

/** Texto sob o botão de apagar: explica o que ele faz ou por que está bloqueado. */
fun clearHint(block: ClearBlock?): String = when (block) {
    null -> "Apaga todas de uma vez. O carro não permite escolher uma só."
    ClearBlock.Moving -> "Pare o carro para apagar falhas."
    ClearBlock.NotConnected -> "Conecte o adaptador OBD para apagar falhas."
    ClearBlock.NothingToClear -> "Não há falhas para apagar."
}

data class OutcomeText(val title: String, val message: String, val button: String)

/** Mensagens do resultado de apagar (spec 8.2). "Limpo" só quando a releitura confirmou. */
fun outcomeText(o: ClearOutcome): OutcomeText = when (o) {
    ClearOutcome.Cleared -> OutcomeText(
        "Falhas apagadas",
        "O carro confirmou que não há falhas registradas. Os monitores de prontidão precisam de alguns dias de uso para completar.",
        "Concluir",
    )
    is ClearOutcome.Returned -> OutcomeText(
        "O carro ainda registra falhas",
        "O comando foi aceito, mas ${o.codes.joinToString(", ") { it.code }} continua registrado. A falha persiste ou é permanente. Corrija a causa e apague de novo.",
        "Ver falhas",
    )
    is ClearOutcome.Refused ->
        if (o.nrc == 0x22) {
            OutcomeText("O carro recusou apagar", "Desligue o motor, deixe a chave na posição ligada e tente de novo.", "Entendi")
        } else {
            OutcomeText("O carro recusou apagar", "O carro recusou o comando (código 0x%02X). Tente de novo com o carro parado e a chave ligada.".format(o.nrc), "Entendi")
        }
    ClearOutcome.Unverified -> OutcomeText(
        "Comando aceito, mas não deu para conferir",
        "Não consegui reler as falhas. Abra a lista de novo para ver o estado do carro.",
        "Ver falhas",
    )
    ClearOutcome.NoResponse -> OutcomeText(
        "O carro não respondeu",
        "Confira a lista de falhas antes de tentar de novo. Nada foi reenviado.",
        "Ver falhas",
    )
    is ClearOutcome.Blocked -> when (o.reason) {
        ClearBlock.Moving -> OutcomeText("Carro em movimento", clearHint(o.reason), "Entendi")
        ClearBlock.NotConnected -> OutcomeText("Adaptador desconectado", clearHint(o.reason), "Entendi")
        ClearBlock.NothingToClear -> OutcomeText("Nada para apagar", "O carro não tem falhas registradas.", "Entendi")
    }
}

/** Resultado para o histórico. Bloqueios não enviaram nada, então não entram nele. */
fun ClearOutcome.toResult(): ClearResult? = when (this) {
    ClearOutcome.Cleared -> ClearResult.Cleared
    is ClearOutcome.Returned -> ClearResult.Returned
    is ClearOutcome.Refused -> ClearResult.Refused
    ClearOutcome.Unverified -> ClearResult.Unverified
    ClearOutcome.NoResponse -> ClearResult.NoResponse
    is ClearOutcome.Blocked -> null
}

fun toggled(selection: Set<String>, code: String): Set<String> = if (code in selection) selection - code else selection + code
