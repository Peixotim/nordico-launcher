#!/usr/bin/env python3
"""Gera app/src/main/assets/dtc/catalog.json, o catálogo de falhas embutido no Nórdico.

Camadas (campo `r` no JSON):
  1  entrada completa em português, escrita à mão em tools/dtc/curated/*.json
  2  título em português (OBDIICodes, MIT) para códigos genéricos
  3  título em inglês (OBDex, CC0, e a lista Honda do dtc-database, MIT)

Fontes (baixadas por quem roda, nunca versionadas):
  --obdex   pasta data/generic do OBDex   (arquivos *_enriched.yaml)
  --ptbr    Codigos-ptbr.json do OBDIICodes
  --honda   data/source-data/honda_codes.txt do dtc-database
  --curated pasta com as traduções feitas à mão (padrão: tools/dtc/curated)

Uso:
  build_catalog.py build --obdex D --ptbr F --honda F [--out F]
  build_catalog.py show CODE...  --obdex D        (mostra o que o OBDex diz, para traduzir)

Dependência da ferramenta (não do app): pyyaml.
"""
import argparse
import glob
import io
import json
import os
import re
import sys

import yaml

CODE = re.compile(r"^[PCBU][0-3][0-9A-F]{3}$")
SEVERITIES = {"ok", "attention", "stop"}
HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OUT = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "dtc", "catalog.json")


def die(msg):
    sys.exit("erro: " + msg)


def load_obdex(folder):
    """code -> entrada completa do OBDex (título em inglês, causas, sintomas...)."""
    loader = getattr(yaml, "CSafeLoader", yaml.SafeLoader)
    out = {}
    files = sorted(glob.glob(os.path.join(folder, "*_enriched.yaml")))
    if not files:
        die("nenhum *_enriched.yaml em " + folder)
    for f in files:
        with io.open(f, encoding="utf-8") as fh:
            for entry in yaml.load(fh, Loader=loader):
                code = str(entry["code"]).upper()
                if code in out:
                    die("código duplicado no OBDex: " + code)
                out[code] = entry
    return out


def load_ptbr(path):
    """code -> título em português. P1xxx (e P3xxx de fabricante) ficam de fora: dependem da marca."""
    with io.open(path, encoding="utf-8") as fh:
        rows = json.load(fh)
    out = {}
    for r in rows:
        code = r["Code"].strip().upper()
        if code.startswith("P1") or not CODE.match(code):
            continue
        title = " ".join(r["Description"].split())
        if title:
            out[code] = title
    return out


def load_honda(path):
    out = {}
    with io.open(path, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            m = re.match(r"^\s*([PCBU][0-9A-F]{4})\s*-\s*(.+?)\s*$", line)
            if m and CODE.match(m.group(1).upper()):
                out[m.group(1).upper()] = " ".join(m.group(2).split())
    return out


def load_curated(folder):
    out = {}
    for f in sorted(glob.glob(os.path.join(folder, "*.json"))):
        with io.open(f, encoding="utf-8") as fh:
            data = json.load(fh)
        for code, e in data.items():
            code = code.upper()
            if code in out:
                die("código curado em mais de um arquivo: " + code)
            if not CODE.match(code):
                die("código inválido em %s: %s" % (f, code))
            if e.get("severity") not in SEVERITIES:
                die("%s: severity inválida (%s)" % (code, e.get("severity")))
            if not e.get("title", "").strip() or not e.get("solution", "").strip() or not e.get("causes"):
                die("%s: title, solution e causes são obrigatórios" % code)
            out[code] = e
    return out


def build(obdex, ptbr, honda, curated):
    codes = {}
    # Camada 3: título em inglês.
    for code, e in obdex.items():
        codes[code] = {"t": " ".join(e["title"]["en"].split()), "l": "en", "r": 3}
    for code, title in honda.items():
        codes.setdefault(code, {"t": title, "l": "en", "r": 3})
    # Camada 2: título em português.
    for code, title in ptbr.items():
        codes[code] = {"t": title, "r": 2}
    # Camada 1: entradas completas, escritas à mão.
    for code, e in curated.items():
        item = {"t": " ".join(e["title"].split()), "r": 1, "c": e["causes"], "x": e["solution"].strip()}
        if e["severity"] != "attention":
            item["s"] = e["severity"]
        if e.get("symptoms"):
            item["y"] = e["symptoms"]
        codes[code] = item
    for code, item in codes.items():
        if not CODE.match(code):
            die("código inválido: " + code)
        if not item["t"]:
            die("sem título: " + code)
    return codes


def cmd_build(a):
    obdex = load_obdex(a.obdex)
    ptbr = load_ptbr(a.ptbr)
    honda = load_honda(a.honda)
    curated = load_curated(a.curated)
    codes = build(obdex, ptbr, honda, curated)
    doc = {"version": 1, "codes": {k: codes[k] for k in sorted(codes)}}
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    with io.open(a.out, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(doc, fh, ensure_ascii=False, separators=(",", ":"))
        fh.write("\n")
    by_tier = {1: 0, 2: 0, 3: 0}
    by_family = {}
    for code, item in codes.items():
        by_tier[item["r"]] += 1
        by_family[code[:2]] = by_family.get(code[:2], 0) + 1
    print("total=%d  tier1=%d  tier2=%d  tier3=%d  bytes=%d" % (len(codes), by_tier[1], by_tier[2], by_tier[3], os.path.getsize(a.out)))
    print("famílias:", ", ".join("%s=%d" % kv for kv in sorted(by_family.items())))


def cmd_show(a):
    obdex = load_obdex(a.obdex)
    for code in [c.upper() for c in a.codes]:
        e = obdex.get(code)
        if not e:
            print("== %s: não está no OBDex" % code)
            continue
        print("== %s: %s" % (code, e["title"]["en"]))
        print("   descrição:", " ".join(e.get("description", {}).get("en", "").split()))
        for c in e.get("common_causes", []):
            print("   causa [%s]: %s" % (c.get("likelihood"), c["label"]["en"]))
        for s in e.get("symptoms", []):
            print("   sintoma:", s["en"])
        flags = e.get("flags", {})
        print("   flags:", {k: v for k, v in flags.items() if v}, " reparo:", e.get("repair", {}).get("difficulty"))
        print("   relacionados:", e.get("related_codes"))


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build")
    b.add_argument("--obdex", required=True)
    b.add_argument("--ptbr", required=True)
    b.add_argument("--honda", required=True)
    b.add_argument("--curated", default=os.path.join(HERE, "curated"))
    b.add_argument("--out", default=DEFAULT_OUT)
    b.set_defaults(fn=cmd_build)
    s = sub.add_parser("show")
    s.add_argument("codes", nargs="+")
    s.add_argument("--obdex", required=True)
    s.set_defaults(fn=cmd_show)
    a = p.parse_args()
    a.fn(a)


if __name__ == "__main__":
    main()
