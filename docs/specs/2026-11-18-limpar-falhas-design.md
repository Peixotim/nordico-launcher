# Nórdico: limpar falhas do carro (OBD-II) (design)

Data: 2026-11-18 · Status: rascunho para aprovação · Altera a [fase 2](2026-11-17-fase2-veiculo-radio-midia-design.md) (seção 5, "Não-objetivos")

## 1. Intenção e porquê

Hoje a folha de falhas só lê. O dono do carro quer ver o que cada código significa, saber o que fazer e, depois de resolver, **apagar as falhas de verdade** e deixar o carro limpo, sem comprar um scanner nem ir à oficina só para isso.

Isso **revoga uma regra da fase 2**: "o Nórdico só lê, nunca escreve no carro". A decisão é consciente e vale **só para um comando**, o apagar padrão do OBD-II (modo 04). Nenhuma outra escrita entra (seção 5 e 8.2).

## 2. Fatos do protocolo que moldam o design

| Fato | Consequência |
|---|---|
| O modo 04 (SAE J1979 / ISO 15031-5) apaga **todos** os códigos de uma vez. Não existe apagar um código só no padrão. | Não haverá "apagar esta falha". Apagar é sempre "todas". |
| Apagar também zera o freeze frame, os dados de teste dos sensores de oxigênio e os **monitores de prontidão** (readiness). | A confirmação avisa. Readiness leva vários ciclos de condução para voltar, o que importa em inspeção veicular. |
| Falha que persiste faz a luz voltar sozinha. Códigos permanentes (modo 0A) não saem pelo modo 04. | O app **relê depois de apagar** e só diz "limpo" se a releitura confirmar. |
| Muitas ECUs só aceitam apagar com chave ligada e **motor desligado**; recusam com `7F 04 22` (condições incorretas). | Tratar a recusa como resultado normal, com mensagem clara. O comportamento exato do Civic só se confirma no carro (seção 11). |
| Resposta positiva: `44` (uma linha por ECU que respondeu). Negativa: `7F 04 xx`. Pode vir `NO DATA`, `BUS BUSY`, `CAN ERROR`. | Parser puro e tipado (seção 8.1). |
| Apagar duas vezes não causa dano. | Mesmo assim o app **nunca reenvia sozinho**: o usuário decide. |

## 3. Objetivos

- Lista de falhas com, por código: título, **gravidade**, causas prováveis, solução sugerida e botão de vídeo. Conteúdo em português, **embutido no app (offline)**.
- **"Apagar todas as falhas"**: comando real, com confirmação que explica os efeitos e com resultado verificado por releitura.
- **"Marcar como corrigida"** por falha, com **seleção múltipla**: anotação **só no app**, para acompanhar reparos. Não toca no carro, e a interface diz isso.
- Histórico local das limpezas (data, códigos que existiam, resultado).
- Proteções: só com o carro parado e conectado, uma limpeza por vez, sem reenvio automático.
- Todo o caminho roda no emulador (simulador de cenários) e é testado na JVM.

## 4. Não-objetivos

- Apagar **um** código ou um subconjunto (impossível no padrão; UDS 0x14 por DTC é específico de fabricante e fica fora).
- Qualquer outra escrita no carro: coding, adaptações, reset de ECU, modos 05, 08, 09 de escrita, UDS.
- Ler e salvar o freeze frame (modo 02) antes de apagar. Fica como evolução; a confirmação avisa que ele será perdido.
- Códigos pendentes (modo 07) e permanentes (modo 0A) na lista. O resultado apenas informa quando códigos "voltaram".
- Catálogo completo de milhares de códigos. A primeira versão leva os códigos mais comuns (seção 8.5); o resto cai em descrição genérica por categoria.
- API externa. Não há API gratuita confiável com solução em português, e a central pode estar sem internet.
- Vídeo dentro do app. Só um link de busca.
- Apagar por qualquer atalho (HUD, gaveta, notificação). Só dentro da folha de falhas.

## 5. Anti-objetivos (falha mesmo que "funcione")

- Apagar sem o usuário ter visto o que será apagado e confirmado.
- Dizer "falhas apagadas" sem a releitura confirmar.
- Apagar com o carro em movimento.
- Um botão por falha que **parece** apagar só aquele código.
- Enviar ao adaptador qualquer comando fora da lista permitida.
- Mensagem de solução que soe como diagnóstico fechado ou minimize um problema de segurança.

## 6. Restrições

- Continua em `:app`, com `sim`/`device`; o protocolo é código puro na JVM.
- Comandos permitidos ao adaptador, por expressão regular fixa: `AT...`, `01xx`, `03` e `04`. Qualquer outro é erro de programação e falha nos testes.
- Textos em pt-BR. Design system da fase 1 (Nórdico, mola, um único destaque, vermelho de sistema só para alerta).
- Commits em inglês, Conventional Commits, um por arquivo, sem trailers de ferramenta.

## 7. Abordagens consideradas

**Semântica do "resolver":**

| | O que faz | Veredito |
|---|---|---|
| A | "Ver solução" por falha e um único "Apagar todas". | Honesta, mas sem como acompanhar reparos. |
| **B** | A mais "Marcar como corrigida" local por falha, com seleção múltipla. | **Escolhida pelo usuário**, com a ressalva de que apagar real é só "todas". |
| C | Apagar tudo e "restaurar" os códigos não selecionados. | Impossível e enganosa. Descartada. |

**Fonte do conteúdo:** API externa (descartada: sem opção gratuita em português, depende de internet), banco embutido (**escolhido**), híbrido com atualização online (futuro, só se houver fonte com licença clara).

**Vídeo:** link de busca no YouTube com código e modelo, abrindo no app ou navegador da central; incorporar player (descartado: peso, internet e distração).

## 8. Design

### 8.1 Protocolo

- Novo comando `04` em `ObdSession.clearFaults()`, com tempo limite próprio (`CLEAR_TIMEOUT_MS = 8000`: a ECU pode demorar mais que uma leitura).
- Função pura `parseClear(raw)`:
  - ao menos uma linha `44` e nenhuma `7F04` → **Aceito**;
  - `44` junto com `7F04` (várias ECUs) → **Aceito**, e a releitura decide;
  - só `7F04xx` → **Recusado** com o código `xx` (`22` condições incorretas, `11` serviço não suportado, `21` ocupada, outros);
  - `NO DATA` ou sem resposta → **Sem resposta**;
  - `BUS BUSY`, `CAN ERROR`, `UNABLE TO CONNECT` → erro tipado, como nas leituras.
- Lista de comandos permitidos aplicada no único ponto que envia (`ObdSession.send`), para que um comando novo por engano quebre o teste.

### 8.2 Fluxo e resultados

1. Guarda (função pura `clearGate`): conectado, **velocidade lida e igual a 0** e há algo para apagar (falhas na lista ou luz acesa). Senão, bloqueado com o motivo. O repositório **repete a checagem** antes de enviar.
2. Estado `Clearing`; o laço de leitura contínua fica parado pelo mesmo `Mutex` da sessão.
3. Envia `04`. Se aceito, espera `CLEAR_SETTLE_MS = 1500` e **relê** as falhas e o PID de status.
4. Resultado (`ClearOutcome`), mostrado ao usuário:

| Resultado | Quando | Mensagem (resumo) |
|---|---|---|
| `Cleared` | aceito e a releitura veio vazia | "Falhas apagadas. Os monitores de prontidão precisam de alguns dias de uso para completar." |
| `Returned(codes)` | aceito e a releitura ainda tem códigos | "O carro ainda registra: P0420. A falha persiste ou é permanente." |
| `Refused(reason)` | `7F04xx` | `22`: "Desligue o motor, deixe a chave na posição ligada e tente de novo." Outros: "O carro recusou o comando." |
| `Unverified` | aceito, mas a releitura falhou | "Comando aceito, mas não consegui conferir. Abra a lista de novo." |
| `NoResponse` | sem resposta ou conexão perdida | "O carro não respondeu. Confira a lista antes de tentar de novo." |
| `Blocked(reason)` | guarda | "Pare o carro para apagar falhas." / "Conecte o adaptador." |

5. Depois de qualquer resultado com comando enviado, a lista e as marcas são atualizadas; o resultado fica visível até o usuário dispensar. Nada reenvia sozinho; um segundo toque durante `Clearing` é ignorado.

### 8.3 Marcas e histórico (locais)

- **Marca "corrigida"**: conjunto `código → data`, em DataStore. Ao ler a lista, marcas de códigos que **não existem mais** no carro são descartadas (a falha foi apagada ou sumiu). Marca de código ainda presente aparece como "Corrigida, aguardando apagar".
- **Histórico**: até 20 registros `(data, códigos, resultado)`, gravados **antes** de enviar o `04` (o registro nasce como "tentativa" e o resultado é completado depois), para que uma queda no meio não perca o que existia.
- Codificação em texto (como os presets do rádio), classe pura testada.

### 8.4 Interface (a validar em protótipo 05 antes do código)

Folha de falhas ampliada (rolável):

```
Falhas registradas                                 [Selecionar]
2 falhas · luz do motor acesa
[ Apagar todas as falhas ]            (botão principal, cor de destaque)
-----------------------------------------------------------------
 P0420  Eficiência do catalisador abaixo do limite   ● Atenção   ›
 P0133  Sensor de oxigênio, resposta lenta           ● Atenção   ›
```

Linha expandida: causas prováveis, o que fazer, `[Ver vídeo]` (só com o carro parado) e `[Marcar como corrigida]`. Modo "Selecionar": caixas à esquerda e barra inferior `Marcar N como corrigidas`. Texto fixo sob o botão de marcar: "Só no app. Não apaga nada no carro."

Confirmação (folha própria): lista o que será apagado (os códigos), os efeitos (freeze frame, readiness, a luz volta se a falha persistir) e as condições ("com o carro parado; em muitos carros, motor desligado e chave ligada"), com `[Cancelar]` e `[Apagar]`. Durante `Clearing`, indicador e botões desativados. O resultado aparece em cartão na própria folha.

Gravidade: **Pode seguir** (verde de sistema), **Atenção** (âmbar), **Pare e procure ajuda** (vermelho de sistema). Vermelho só nesse caso.

### 8.5 Conteúdo (catálogo embutido)

- `assets/dtc/catalog.json`: por código, `title`, `severity`, `causes[]`, `solution`.
- Primeira versão: os códigos mais comuns de motor e de emissões (P0xxx genéricos) e os da linha Honda que tivermos como confirmar (P1xxx), todos em português, **escritos e revisados à mão**, com nota de que é orientação e não diagnóstico.
- Fallback sempre definido: código padrão sem entrada ("descrição não disponível no Nórdico", pelo prefixo: motor/câmbio, carroceria, chassi, rede) e código de fabricante.
- A função atual `dtcDescription` é substituída pelo catálogo.

### 8.6 Vídeo

`https://www.youtube.com/results?search_query=<código>+<modelo>+como+resolver`, aberto por `ACTION_VIEW`, **só com velocidade 0**. Sem app ou navegador na central, a linha mostra "Nenhum app para abrir o vídeo", sem travar. O modelo vem de uma constante única.

## 9. Arquitetura

```
data/obd/     ObdClear.kt  (parseClear, ClearOutcome, ClearGate, clearGate)
              ObdSession.kt (altera: clearFaults, lista de comandos permitidos)
              ObdRepository.kt (altera: clearFaults(), clearState, dismissClearResult())
              ElmObdRepository.kt (altera)
              DtcCatalog.kt  (DtcInfo, Severity, lookup com fallback)
data/prefs/   FaultLog.kt (marcas e histórico, codec puro)  PrefsRepository.kt (altera)
feature/vehicle/  FaultsSheet.kt (reescreve)  ClearConfirmSheet.kt  FaultRow.kt  FaultsLogic.kt
app/src/main/assets/dtc/catalog.json
app/src/sim/  SimObdRepository.kt (altera: clear e cenários)
docs/prototipos/05-limpar-falhas.html
```

Dependência nova só de teste: `org.json:json` (o `org.json` do Android não existe na JVM). Em produção usa o do próprio Android.

## 10. Testes e verificação

- Unitários (JVM): `parseClear` com todas as respostas da seção 8.1; `ObdSession.clearFaults` contra transporte roteirizado (aceito, aceito com duas ECUs, recusado `22`, sem resposta, tempo esgotado); **lista de comandos permitidos** (nenhum fluxo envia algo fora dela); `clearGate`; fluxo do repositório (bloqueio em movimento, segundo toque ignorado, releitura, `Returned`, `Unverified`, perda de conexão no meio, leitura contínua retoma); codec do `FaultLog` e poda de marcas; catálogo (todo código com título, causas e solução; gravidade válida; fallback por prefixo).
- Simulador: cenários `Faults` (apaga com sucesso), `FaultsPersistent` (aceita, mas os códigos voltam) e `ClearRefused` (recusa `22`), acionáveis pelo painel.
- Integração: contra o ELM327-emulator via TCP, se o cenário `car` dele responder ao `04`; senão fica só o roteiro.
- Visual no emulador: lista, linha expandida, seleção, confirmação, cada resultado, Dia e Noite, 240 dpi.

## 11. Riscos

- **O Civic recusa com o motor ligado** (provável) ou se comporta de outro jeito. Mitigação: a recusa `22` é um resultado de primeira classe; o texto orienta. Confirmar no carro (checklist abaixo).
- **Clone de ELM327 com firmware ruim** responde mal ao `04`. Mitigação: nada é dado como feito sem releitura; `Unverified` e `NoResponse` existem.
- **Usuário apaga para esconder defeito** (venda, inspeção). Mitigação: histórico local e aviso de readiness. O app não impede; a decisão e a responsabilidade são do dono.
- **Conteúdo errado ou excessivamente confiante.** Mitigação: revisão manual, texto de orientação e gravidade conservadora (na dúvida, "Atenção").
- **Luz volta logo.** É esperado se a falha persiste; o resultado `Returned` explica.

**Checklist no carro (fase 3):** (1) com chave ligada e motor desligado, `04` é aceito? (2) com motor ligado, recusa com `22`? (3) quanto tempo a ECU leva até a releitura vir limpa? (4) a luz do motor apaga e o PID de status vai a zero? (5) quais monitores de prontidão zeram?

## 12. Decisões tomadas por mim (revisar)

- Exigir velocidade **lida** e igual a zero; sem leitura de velocidade, bloqueia. (Todo carro OBD-II suporta o PID `0D`.)
- **Não** bloquear por rotação: a ECU decide e a recusa `22` é explicada. Bloquear por rotação poderia impedir um carro que aceita.
- Histórico gravado **antes** do envio.
- Uma confirmação em folha própria (sem "segurar para confirmar"), por simplicidade; reavaliar se houver toque acidental.
- Catálogo inicial pequeno, em vez de importar uma base aberta sem licença verificada.

## 13. Deixado para quem implementar

- Texto final de cada entrada do catálogo (com fonte conferida) e a lista exata dos códigos da v1.
- Valores de espera e de tempo limite, ajustáveis na constante.
- Detalhes visuais da folha, a partir do protótipo 05.
- Se o emulador ELM327 suporta `04`; senão, só o roteiro.
