package app.nordico.launcher.data.obd

/** Ordem de prioridade quando mais de um PID está vencido por igual. */
private val PRIORITY = listOf(
    ObdPid.Speed, ObdPid.Rpm, ObdPid.Load, ObdPid.Throttle,
    ObdPid.Coolant, ObdPid.Intake, ObdPid.Fuel, ObdPid.Voltage,
)

/** Intervalo mínimo entre leituras de cada PID, em ms (spec 10.1). */
val POLL_INTERVALS: Map<ObdPid, Long> = mapOf(
    ObdPid.Speed to 250L, ObdPid.Rpm to 250L,
    ObdPid.Load to 1000L, ObdPid.Throttle to 1000L,
    ObdPid.Coolant to 2000L, ObdPid.Intake to 2000L, ObdPid.Fuel to 2000L, ObdPid.Voltage to 2000L,
)

/** O PID mais atrasado entre os [active]; `null` se nenhum venceu ainda. */
fun nextPid(nowMs: Long, last: Map<ObdPid, Long>, active: Set<ObdPid>): ObdPid? =
    PRIORITY.filter { it in active }
        .mapNotNull { pid ->
            val lastRead = last[pid] ?: return@mapNotNull pid to Long.MAX_VALUE
            val overdue = nowMs - lastRead - POLL_INTERVALS.getValue(pid)
            if (overdue >= 0) pid to overdue else null
        }
        .maxByOrNull { it.second } // empate: o primeiro da prioridade (maxByOrNull fica com o primeiro)
        ?.first

/** Conta `NO DATA` seguidos por PID; ao chegar ao limite o PID deve deixar de ser pedido. */
class StrikeCounter(private val limit: Int = 3) {
    private val strikes = mutableMapOf<ObdPid, Int>()

    /** `true` quando o PID acabou de atingir o limite. */
    fun noData(p: ObdPid): Boolean {
        val n = (strikes[p] ?: 0) + 1
        strikes[p] = n
        return n >= limit
    }

    fun ok(p: ObdPid) {
        strikes.remove(p)
    }
}
