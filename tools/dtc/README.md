# Fault code catalog tool

Generates `app/src/main/assets/dtc/catalog.json`, the offline fault code catalog bundled in the app.
The generated file is committed; the downloaded sources are not.

## Layers

| `r` | What it has | Source |
|---|---|---|
| 1 | Portuguese title, severity, causes, symptoms and what to do | hand-written in `curated/*.json` |
| 2 | Portuguese title | OBDIICodes (MIT), generic codes only |
| 3 | English title | OBDex (CC0) and the Honda list from dtc-database (MIT) |

Manufacturer-specific `P1xxx` codes never get a generic title: they only enter through the Honda list
or a curated entry. Anything missing from the catalog gets a generic text by category in the app.

## Run

```sh
pip install pyyaml
git clone --depth 1 https://github.com/foerbsnavi/OBDex.git      /some/empty/dir/OBDex
git clone --depth 1 https://github.com/fabiovila/OBDIICodes.git  /some/empty/dir/OBDIICodes
git clone --depth 1 https://github.com/Wal33D/dtc-database.git   /some/empty/dir/dtc-database

python -I tools/dtc/build_catalog.py build \
  --obdex /some/empty/dir/OBDex/data/generic \
  --ptbr  /some/empty/dir/OBDIICodes/Codigos-ptbr.json \
  --honda /some/empty/dir/dtc-database/data/source-data/honda_codes.txt
```

To read what OBDex says about a code before translating it:

```sh
python -I tools/dtc/build_catalog.py show P0420 P0171 --obdex /some/empty/dir/OBDex/data/generic
```

## Curated entries

One JSON file per batch in `curated/`, named `NN-<family>.json`:

```json
{
  "P0420": {
    "title": "Eficiência do catalisador abaixo do limite (banco 1)",
    "severity": "attention",
    "causes": ["Catalisador envelhecido ou contaminado", "Sensor de oxigênio traseiro lento"],
    "symptoms": ["Luz do motor acesa"],
    "solution": "Confira vazamentos no escape e o sensor traseiro antes de trocar o catalisador."
  }
}
```

Rules: orientation, never a closed diagnosis; when unsure use `attention`; `stop` only for safety issues
(multi-cylinder misfire, overheating, oil pressure loss, anything that can damage the catalyst or engine).
Without a reliable source, an entry does not go in. `build_catalog.py` fails on missing fields, bad codes
and duplicates, and the `DtcCatalogFileTest` unit test checks the generated file.

Licenses of the sources: see `THIRD_PARTY_NOTICES.md` at the repository root.
