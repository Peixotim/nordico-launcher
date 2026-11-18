#!/usr/bin/env python3
"""Gera curated/10-familias.json: entradas completas (camada 1) para famílias de códigos que seguem um
padrão fixo do SAE J2012 (sensores de oxigênio, bobinas, bicos, sensores MAP/MAF/IAT/ECT/TPS e falhas de
ignição por cilindro). Os textos-modelo abaixo são escritos e revisados à mão; o script só os combina com
o nome do sensor, o banco e o cilindro. Códigos que já têm entrada própria em outro arquivo de curated/ são
pulados, para a entrada escrita à mão prevalecer.

Uso: python -I tools/dtc/gen_families.py
"""
import glob
import io
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
CURATED = os.path.join(HERE, "curated")
OUT = os.path.join(CURATED, "10-familias.json")


def existing_codes():
    codes = set()
    for f in glob.glob(os.path.join(CURATED, "*.json")):
        if os.path.abspath(f) == os.path.abspath(OUT):
            continue
        with io.open(f, encoding="utf-8") as fh:
            codes |= set(json.load(fh))
    return codes


def entry(title, severity, causes, symptoms, solution):
    return {"title": title, "severity": severity, "causes": causes, "symptoms": symptoms, "solution": solution}


# ---------------------------------------------------------------- sensores de oxigênio
O2_SYMPTOMS = [
    "Luz do motor acesa (em geral depois de dois ciclos de condução)",
    "Consumo um pouco maior",
    "Emissões mais altas; pode reprovar na inspeção",
]
O2_TYPES = {
    0: ("circuito com defeito", [
        "Fiação ou conector do sensor com defeito (aberto, em curto ou corroído)",
        "Sensor de oxigênio com defeito",
        "Vazamento de escapamento perto do sensor",
    ], "Inspecione a fiação e o conector do sensor antes de trocá-lo. Procure vazamentos no escapamento."),
    1: ("tensão baixa", [
        "Sensor contaminado ou envelhecido",
        "Fio de sinal em curto com o terra",
        "Mistura realmente pobre (entrada de ar, pressão de combustível baixa)",
        "Vazamento de escapamento antes do sensor",
    ], "Procure entradas de ar e vazamentos no escape. Se a mistura estiver certa, o sensor é o suspeito."),
    2: ("tensão alta", [
        "Fio de sinal em curto com alimentação ou bateria",
        "Sensor com defeito internamente",
        "Mistura realmente rica (bicos, válvula de purga, pressão de combustível alta)",
        "Sensor contaminado por óleo ou líquido de arrefecimento",
    ], "Confira o chicote e verifique se o motor não está rico. Se a fiação estiver boa, o sensor costuma ser trocado."),
    3: ("resposta lenta", [
        "Sensor envelhecido, com fuligem ou contaminado",
        "Vazamento de escapamento antes do sensor",
        "Aquecedor do sensor fraco",
        "Falha de mistura (entrada de ar, injetores, pressão de combustível)",
    ], "Procure vazamentos e confira a fiação. Sensor com muitos anos costuma ser trocado; use peça de especificação correta."),
    4: ("sem atividade detectada", [
        "Sensor desconectado ou fio aberto",
        "Sensor morto ou muito envelhecido",
        "Aquecedor do sensor sem funcionar",
        "Vazamento de escapamento perto do sensor",
    ], "Confira o conector e o fusível do aquecedor. Sem sinal nenhum, o sensor normalmente precisa ser trocado."),
}
O2_HEATER = (
    "aquecedor, circuito com defeito",
    [
        "Resistência de aquecimento do sensor queimada (troca do sensor)",
        "Fusível, relé ou fio de comando do aquecedor aberto",
        "Curto no aquecedor (corrente alta demais)",
        "Conector corroído, resistência de contato",
    ],
    "Verifique o fusível e o conector do sensor. Aquecedor queimado exige trocar o sensor. Não costuma afetar a direção, mas impede a inspeção de emissões.",
)


def o2():
    out = {}
    # (primeiro código do bloco, banco, sensor): 5 códigos de sinal e depois 1 de aquecedor
    blocks = [(0x130, 1, 1), (0x136, 1, 2), (0x142, 1, 3), (0x150, 2, 1), (0x156, 2, 2), (0x162, 2, 3)]
    for start, bank, sensor in blocks:
        where = "banco %d, sensor %d" % (bank, sensor)
        for t, (label, causes, fix) in O2_TYPES.items():
            code = "P%04X" % (start + t)
            out[code] = entry("Sensor de oxigênio, %s (%s)" % (label, where), "attention", causes, O2_SYMPTOMS, fix)
        label, causes, fix = O2_HEATER
        out["P%04X" % (start + 5)] = entry("Sensor de oxigênio, %s (%s)" % (label, where), "attention", causes,
                                           O2_SYMPTOMS[:2] + ["Monitor do catalisador não completa a prontidão"], fix)
    return out


# ---------------------------------------------------------------- falha de ignição por cilindro
def misfire():
    out = {}
    for n in range(5, 9):
        out["P030%d" % n] = entry(
            "Falha de ignição no cilindro %d" % n, "stop",
            ["Bobina do cilindro %d com defeito (bobina, conector, terra)" % n,
             "Vela do cilindro %d desgastada, suja, trincada ou com folga errada" % n,
             "Bico do cilindro %d entupido ou vazando, ou seu comando" % n,
             "Compressão baixa no cilindro %d (válvula, anéis, junta do cabeçote) ou entrada de ar local" % n],
            ["Luz do motor acesa; piscando quando a falha pode danificar o catalisador",
             "Marcha lenta irregular, tranco, vibração",
             "Perda de força e consumo maior"],
            "Troque a bobina do cilindro %d de lugar com a de outro cilindro: se o código seguir a bobina, ela é a culpada. Depois confira a vela. Com a luz piscando, evite rodar: o catalisador pode ser danificado." % n)
    return out


# ---------------------------------------------------------------- bobinas de ignição
def coils():
    out = {}
    for i, letter in enumerate("ABCDEFGH"):
        out["P035%d" % (1 + i)] = entry(
            "Bobina de ignição %s, circuito primário/secundário" % letter, "attention",
            ["Bobina %s com defeito" % letter,
             "Conector ou chicote da bobina corroído, solto ou aberto",
             "Alimentação ou terra da bobina ruim",
             "Módulo de ignição ou do motor com defeito (menos comum)"],
            ["Luz do motor acesa", "Marcha lenta irregular e tranco", "Falha de ignição no cilindro ligado à bobina"],
            "Verifique o conector e o chicote da bobina %s e troque-a de lugar com outra para confirmar. Bobina ruim causa falha de ignição e pode estragar o catalisador." % letter)
    return out


# ---------------------------------------------------------------- bicos injetores
def injectors():
    out = {}
    for n in range(1, 9):
        out["P020%d" % n] = entry(
            "Circuito do bico injetor, cilindro %d" % n, "attention",
            ["Bico injetor do cilindro %d com defeito (bobina interna fora da especificação)" % n,
             "Conector ou chicote do bico corroído, solto ou aberto",
             "Módulo do motor com defeito (menos comum)"],
            ["Luz do motor acesa", "Falha de ignição ou marcha lenta irregular", "Perda de força e consumo maior"],
            "Confira o conector e o chicote do bico do cilindro %d e meça a resistência da bobina do bico. Bico entupido ou com circuito aberto costuma ser trocado ou limpo em oficina." % n)
    return out


# ---------------------------------------------------------------- sensores MAP, IAT, ECT, TPS, MAF
SENSORS = {
    "MAP": {
        "name": "Sensor de pressão do coletor (MAP)",
        "first": 0x105,
        "causes": ["Mangueira de vácuo do sensor solta ou rachada", "Conector ou chicote corroído, solto ou aberto", "Sensor com defeito", "Entrada de ar falsa no coletor"],
        "symptoms": ["Luz do motor acesa", "Marcha lenta irregular", "Falta de força e consumo maior"],
        "fix": "Confira a mangueira de vácuo e o conector do sensor. Se estiverem bons, o sensor costuma ser trocado.",
    },
    "IAT": {
        "name": "Sensor de temperatura do ar de admissão",
        "first": 0x110,
        "causes": ["Conector solto ou desconectado, ou fio rompido", "Sensor com defeito", "Fio de sinal em curto com o terra ou com tensão"],
        "symptoms": ["Luz do motor acesa", "Em geral quase não se nota", "Consumo um pouco maior"],
        "fix": "Verifique o conector do sensor, perto da caixa do filtro de ar ou no coletor. Costuma ser mau contato; sensor novo é barato.",
    },
    "ECT": {
        "name": "Sensor de temperatura do líquido de arrefecimento",
        "first": 0x115,
        "causes": ["Conector ou chicote com mau contato, em curto ou aberto", "Sensor com defeito ou com líquido dentro", "Nível baixo do líquido de arrefecimento ou ar no sistema"],
        "symptoms": ["Luz do motor acesa", "Ventoinha ligada o tempo todo ou que não liga", "Partida a frio difícil", "Ponteiro de temperatura estranho"],
        "fix": "Inspecione o conector e o chicote e confira o nível do líquido. Como esse sensor pode esconder um superaquecimento real, vigie o ponteiro de temperatura.",
    },
    "TPS": {
        "name": "Sensor de posição da borboleta (TPS)",
        "first": 0x120,
        "causes": ["Corpo de borboleta sujo ou com defeito", "Conector ou chicote corroído, solto ou aberto", "Sensor de posição com defeito", "Entrada de ar falsa"],
        "symptoms": ["Luz do motor acesa", "Aceleração falhando ou irregular", "Marcha lenta instável", "O carro pode entrar em modo de emergência"],
        "fix": "Limpe o corpo de borboleta com limpa-corpo próprio e confira o conector. Falha no sensor da borboleta pode limitar a potência: evite rodar longe.",
    },
    "MAF": {
        "name": "Sensor de fluxo de ar (MAF)",
        "first": 0x100,
        "causes": ["Sensor sujo ou com defeito", "Conector ou chicote corroído, solto ou aberto", "Vazamento ou duto de ar solto entre o sensor e a borboleta", "Filtro de ar muito sujo"],
        "symptoms": ["Luz do motor acesa", "Falta de força ou hesitação", "Marcha lenta irregular ou motor morrendo", "Consumo maior"],
        "fix": "Confira o conector e a mangueira de ar entre o sensor e a borboleta. Limpar o sensor com limpa-contato próprio ajuda em alguns casos.",
    },
}
# Os cinco tipos de falha de cada sensor, na ordem dos códigos: circuito, faixa, baixo, alto, intermitente.
SENSOR_TYPES = ["circuito com defeito", "sinal fora da faixa", "sinal baixo", "sinal alto", "sinal intermitente"]


def sensors():
    out = {}
    for s in SENSORS.values():
        for i, label in enumerate(SENSOR_TYPES):
            code = "P%04X" % (s["first"] + i)
            out[code] = entry("%s, %s" % (s["name"], label), "attention", s["causes"], s["symptoms"], s["fix"])
    return out


def main():
    skip = existing_codes()
    merged = {}
    for fam in (o2(), misfire(), coils(), injectors(), sensors()):
        merged.update(fam)
    result = {k: merged[k] for k in sorted(merged) if k not in skip}
    with io.open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    print("geradas %d entradas (%d puladas por já existirem à mão)" % (len(result), len(merged) - len(result)))


if __name__ == "__main__":
    main()
