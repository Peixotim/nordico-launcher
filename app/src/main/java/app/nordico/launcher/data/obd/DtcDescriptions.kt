package app.nordico.launcher.data.obd

private val DESCRIPTIONS = mapOf(
    "P0011" to "Comando de válvulas (admissão) adiantado",
    "P0101" to "Sensor de fluxo de ar fora da faixa",
    "P0107" to "Sensor de pressão do coletor, sinal baixo",
    "P0113" to "Sensor de temperatura do ar de admissão, sinal alto",
    "P0117" to "Sensor de temperatura do líquido de arrefecimento, sinal baixo",
    "P0125" to "Motor demora a atingir a temperatura de trabalho",
    "P0128" to "Termostato abaixo da temperatura esperada",
    "P0133" to "Sensor de oxigênio, resposta lenta",
    "P0171" to "Mistura pobre demais",
    "P0172" to "Mistura rica demais",
    "P0300" to "Falhas de ignição em vários cilindros",
    "P0301" to "Falha de ignição no cilindro 1",
    "P0302" to "Falha de ignição no cilindro 2",
    "P0303" to "Falha de ignição no cilindro 3",
    "P0304" to "Falha de ignição no cilindro 4",
    "P0335" to "Sensor de rotação do virabrequim, sem sinal",
    "P0420" to "Eficiência do catalisador abaixo do limite",
    "P0441" to "Sistema de emissões evaporativas, fluxo incorreto",
    "P0442" to "Pequeno vazamento no sistema de emissões evaporativas",
    "P0455" to "Grande vazamento no sistema de emissões evaporativas",
    "P0500" to "Sensor de velocidade do veículo com defeito",
)

/** Descrição em português de um código de falha comum; `null` quando não conhecemos. */
fun dtcDescription(code: String): String? = DESCRIPTIONS[code.trim().uppercase()]
