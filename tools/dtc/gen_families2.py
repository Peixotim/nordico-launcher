#!/usr/bin/env python3
"""Gera curated/11-familias-2.json: mais famílias de códigos com padrão fixo do SAE J2012 (câmbio automático,
bomba de combustível, EGR, pressão de óleo e comunicação entre módulos). Mesmas regras do gen_families.py:
textos-modelo escritos e revisados à mão, e códigos que já têm entrada própria em outro arquivo de curated/
são pulados.

Uso: python -I tools/dtc/gen_families2.py
"""
import glob
import io
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
CURATED = os.path.join(HERE, "curated")
OUT = os.path.join(CURATED, "11-familias-2.json")


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


# ---------------------------------------------------------------- câmbio automático
TRANS_FLUID = "Confira o nível e o estado do fluido do câmbio (cheiro de queimado ou cor escura é sinal ruim). "
TRANS_SYMPTOMS = ["Luz do motor ou do câmbio acesa", "Trocas de marcha bruscas, atrasadas ou erradas", "Câmbio em modo de emergência (preso em uma marcha)"]
SOLENOID_TYPES = ["mau funcionamento", "desempenho ou preso desligado", "preso ligado", "falha elétrica", "intermitente"]


def transmission():
    out = {}
    families = [
        ("Solenoide de troca %s", ["A", "B", "C", "D", "E"], 0x750),
        ("Solenoide do conversor de torque (TCC)", [None], 0x740),
        ("Solenoide de controle de pressão A", [None], 0x745),
    ]
    for pattern, letters, start in families:
        for li, letter in enumerate(letters):
            for t, label in enumerate(SOLENOID_TYPES):
                code = "P%04X" % (start + li * 5 + t)
                name = pattern % letter if letter else pattern
                out[code] = entry(
                    "%s, %s" % (name, label), "attention",
                    ["Nível ou estado do fluido do câmbio ruim (baixo, velho ou contaminado)",
                     "Solenoide com defeito, emperrado ou com a tela entupida",
                     "Conector ou chicote do solenoide com mau contato, aberto ou em curto",
                     "Corpo de válvulas ou módulo do câmbio com defeito (menos comum)"],
                    TRANS_SYMPTOMS,
                    TRANS_FLUID + "Se o fluido estiver bom, procure uma oficina de câmbio: solenoide e corpo de válvulas exigem desmontagem.")
    ratio = {
        0x730: "Relação de marcha incorreta", 0x731: "Relação incorreta na 1ª marcha", 0x732: "Relação incorreta na 2ª marcha",
        0x733: "Relação incorreta na 3ª marcha", 0x734: "Relação incorreta na 4ª marcha", 0x735: "Relação incorreta na 5ª marcha",
        0x736: "Relação incorreta na marcha à ré",
    }
    for c, t in ratio.items():
        out["P%04X" % c] = entry(
            "Câmbio: " + t, "attention",
            ["Câmbio patinando por fluido baixo, velho ou contaminado", "Embreagens ou fitas internas gastas",
             "Solenoide ou corpo de válvulas com defeito", "Sensor de velocidade do câmbio com defeito"],
            ["Rotação do motor sobe sem o carro acompanhar", "Trocas de marcha demoradas ou bruscas", "Luz do motor ou do câmbio acesa"],
            TRANS_FLUID + "Câmbio patinando se destrói rápido: evite acelerar forte e procure uma oficina logo.")
    speed_sensors = [
        ("Sensor de rotação de entrada do câmbio (turbina)", 0x715),
        ("Sensor de rotação de saída do câmbio", 0x720),
    ]
    spd = ["circuito com defeito", "sinal fora da faixa", "sem sinal", "sinal intermitente"]
    for name, start in speed_sensors:
        for i, label in enumerate(spd):
            out["P%04X" % (start + i)] = entry(
                "%s, %s" % (name, label), "attention",
                ["Sensor de velocidade com defeito ou sujo de limalha", "Fio ou conector aberto, em curto ou corroído", "Roda fônica ou anel danificado"],
                ["Luz do motor ou do câmbio acesa", "Velocímetro errático ou trocas de marcha estranhas"],
                "Confira o conector e o fluido do câmbio. Sensor com limalha costuma ser limpo ou trocado em oficina.")
    five = ["circuito com defeito", "sinal fora da faixa", "sinal baixo", "sinal alto", "sinal intermitente"]
    for i, label in enumerate(five):
        out["P%04X" % (0x710 + i)] = entry(
            "Sensor de temperatura do fluido do câmbio, %s" % label, "attention",
            ["Sensor de temperatura do fluido com defeito", "Fio ou conector aberto, em curto ou corroído",
             "Fluido do câmbio superaquecido por uso pesado ou por problema interno"],
            ["Luz do motor ou do câmbio acesa", "Trocas de marcha diferentes do normal"],
            "Confira o conector do sensor e o estado do fluido. Câmbio superaquecido pede revisão em oficina.")
        out["P%04X" % (0x705 + i)] = entry(
            "Sensor de posição da alavanca do câmbio, %s" % label, "attention",
            ["Sensor ou interruptor da alavanca desregulado ou com defeito", "Fio ou conector aberto, em curto ou corroído",
             "Cabo ou regulagem da alavanca fora de ajuste"],
            ["Luz do motor ou do câmbio acesa", "Carro que não dá partida em N ou P, ou que dá partida em outra posição", "Indicador de marcha errado no painel"],
            "Confira a regulagem da alavanca e o conector do sensor. É um serviço de oficina de câmbio.")
    out["P0700"] = entry(
        "Sistema de controle do câmbio (pedido de luz do motor)", "attention",
        ["O módulo do câmbio registrou uma falha própria (leia os códigos do câmbio)", "Fluido ou solenoides com problema",
         "Comunicação entre o câmbio e o módulo do motor"],
        ["Luz do motor ou do câmbio acesa", "Câmbio em modo de emergência"],
        "Esse código só avisa que o câmbio tem falhas próprias: peça a leitura dos códigos do módulo do câmbio em uma oficina.")
    shift = {0x780: "Falha de troca de marcha", 0x781: "Falha na troca 1-2", 0x782: "Falha na troca 2-3",
             0x783: "Falha na troca 3-4", 0x784: "Falha na troca 4-5"}
    for c, t in shift.items():
        out["P%04X" % c] = entry(
            "Câmbio: " + t, "attention",
            ["Fluido do câmbio baixo, velho ou contaminado", "Solenoide de troca ou corpo de válvulas com defeito", "Embreagem ou fita interna gasta"],
            TRANS_SYMPTOMS, TRANS_FLUID + "Se o fluido estiver bom, procure uma oficina de câmbio.")
    return out


# ---------------------------------------------------------------- combustível, EGR, óleo
def fuel_egr_oil():
    out = {}
    pump = ["circuito com defeito", "sinal baixo", "sinal alto", "sinal intermitente"]
    for i, label in enumerate(pump):
        out["P%04X" % (0x230 + i)] = entry(
            "Bomba de combustível, circuito primário (relé de controle), %s" % label, "attention",
            ["Fusível ou relé da bomba de combustível com defeito", "Conector ou fio da bomba ou do relé aberto, em curto ou corroído",
             "Bomba de combustível com defeito", "Interruptor de corte de combustível (inércia) acionado"],
            ["Carro que não pega ou morre rodando", "Luz do motor acesa", "Falta de força"],
            "Teste o fusível e o relé da bomba e ouça o zumbido da bomba ao ligar a chave. Se o carro não pega, evite insistir na partida.")
    egr = {
        0x400: ("EGR: fluxo com defeito", "Válvula EGR ou passagens entupidas por carvão"),
        0x401: ("EGR: fluxo insuficiente", "Válvula EGR ou passagens entupidas por carvão"),
        0x402: ("EGR: fluxo excessivo", "Válvula EGR presa aberta"),
        0x403: ("EGR: circuito da válvula", "Fio ou conector da válvula EGR com defeito"),
        0x404: ("EGR: circuito fora da faixa", "Válvula EGR emperrada ou sensor de posição com defeito"),
        0x405: ("EGR: sensor de posição A, sinal baixo", "Sensor de posição da EGR com defeito"),
        0x406: ("EGR: sensor de posição A, sinal alto", "Sensor de posição da EGR com defeito"),
    }
    for c, (t, main) in egr.items():
        out["P%04X" % c] = entry(
            t, "attention",
            [main, "Mangueiras de vácuo da EGR soltas ou rachadas", "Conector ou chicote da válvula corroído ou solto"],
            ["Luz do motor acesa", "Marcha lenta irregular ou falhas em baixa rotação", "Detonação (batida de pino) em aceleração"],
            "Limpar a válvula EGR e as passagens costuma resolver. Confira também as mangueiras de vácuo.")
    oil = {
        0x520: "Sensor ou interruptor de pressão do óleo, circuito",
        0x521: "Sensor ou interruptor de pressão do óleo, fora da faixa",
        0x522: "Sensor ou interruptor de pressão do óleo, sinal baixo",
        0x523: "Sensor ou interruptor de pressão do óleo, sinal alto",
    }
    for c, t in oil.items():
        out["P%04X" % c] = entry(
            t, "attention",
            ["Sensor ou interruptor de pressão do óleo com defeito", "Fio ou conector aberto, em curto ou corroído",
             "Pressão de óleo realmente baixa (nível, bomba, desgaste)"],
            ["Luz de pressão do óleo acesa ou apagada de forma estranha", "Luz do motor acesa"],
            "Confira o nível do óleo agora. Se a luz de pressão do óleo estiver acesa no painel, pare o carro: rodar sem pressão destrói o motor.")
    out["P0524"] = entry(
        "Pressão do óleo do motor baixa demais", "stop",
        ["Nível de óleo baixo ou óleo velho e fino", "Bomba de óleo ou peneira entupida", "Folgas internas do motor gastas",
         "Sensor com defeito (menos provável)"],
        ["Luz de pressão do óleo acesa", "Barulho de batidas ou do comando no motor", "Luz do motor acesa"],
        "Pare o carro em local seguro, desligue o motor e confira o nível do óleo. Não rode com a luz de pressão do óleo acesa: o motor pode ser destruído. Peça guincho se o nível estiver certo.")
    return out


# ---------------------------------------------------------------- comunicação entre módulos
def comm():
    out = {}
    modules = {
        0x100: "módulo do motor (ECM/PCM)", 0x101: "módulo do câmbio (TCM)", 0x102: "módulo da caixa de transferência",
        0x121: "módulo do ABS", 0x122: "módulo de controle de estabilidade",
        0x140: "módulo da carroceria (BCM)", 0x151: "módulo do airbag", 0x155: "painel de instrumentos",
        0x164: "módulo do ar-condicionado", 0x167: "módulo do imobilizador",
    }
    for c, name in modules.items():
        out["U%04X" % c] = entry(
            "Comunicação perdida com o %s" % name, "attention",
            ["Bateria fraca ou queda de tensão (a causa mais comum)", "Fiação da rede CAN danificada, em curto ou com conector corroído",
             "Módulo sem alimentação (fusível, relé, terra) ou com defeito"],
            ["Várias luzes de aviso acesas ao mesmo tempo", "Funções do módulo sem resposta", "Códigos em vários módulos"],
            "Comece pela bateria e pelos terminais: tensão baixa derruba vários módulos de uma vez. Se a bateria está boa, procure uma oficina para testar a rede CAN.")
    out["U0001"] = entry(
        "Rede de comunicação CAN de alta velocidade", "attention",
        ["Fiação da rede CAN em curto, aberta ou com conector corroído", "Resistores de terminação ou um módulo derrubando a rede", "Bateria fraca ou terra ruim"],
        ["Várias luzes de aviso acesas", "Carro que não pega ou funções sem resposta"],
        "Confira bateria e terras e procure uma oficina para testar a rede CAN: um módulo ou um fio em curto pode derrubar tudo.")
    out["U0073"] = entry(
        "Rede de comunicação desligada por erro (bus off)", "attention",
        ["Fiação da rede CAN em curto ou aberta", "Módulo com defeito derrubando a rede", "Bateria fraca ou quedas de tensão"],
        ["Várias luzes de aviso acesas", "Funções sem resposta"],
        "Comece pela bateria e pelos terras. Se a bateria está boa, a rede CAN precisa de teste profissional.")
    return out


def main():
    skip = existing_codes()
    merged = {}
    for fam in (transmission(), fuel_egr_oil(), comm()):
        merged.update(fam)
    result = {k: merged[k] for k in sorted(merged) if k not in skip}
    with io.open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2)
        fh.write("\n")
    print("geradas %d entradas (%d puladas por já existirem)" % (len(result), len(merged) - len(result)))


if __name__ == "__main__":
    main()
