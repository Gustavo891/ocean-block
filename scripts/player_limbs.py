"""Converte um .bbmodel humanoide para player limbs do ModelEngine.

Identifica os bones pelo recorte de UV dos cubos (layout de skin 64x64), cria um
bone filho com o token de limb (`phead`, `pbody`, `prarm`, `plarm`, `prleg`,
`plleg`) no pivo que o render exige e move os cubos para ele. O bone original
continua com o nome, o pivo e as animacoes intactos.

Os cubos de segunda camada (chapeu, jaqueta, calca, manga) sao apagados: a peca
de limb do ModelEngine ja desenha a camada de fora da skin. Use --manter-camadas
para preservar a roupa do pack por cima da skin.

O calculo do pivo esta em limb_pivots.py, que tambem serve de conferencia depois.

Uso:
    python scripts/player_limbs.py <arquivo.bbmodel> [...] [--aplicar] [--manter-camadas]

Sem --aplicar so mostra o que seria feito. Com --aplicar grava um backup
`<arquivo>.bak` antes de escrever.
"""

import json
import shutil
import sys
import uuid

from limb_pivots import required_origin

# Canto superior esquerdo da face norte de cada regiao da skin vanilla 64x64.
REGIONS = {
    (8, 8): "phead",
    (20, 20): "pbody",
    (44, 20): "prarm",
    (36, 52): "plarm",
    (4, 20): "prleg",
    (20, 52): "plleg",
}

LAYERS = {(40, 8), (20, 36), (44, 36), (52, 52), (4, 36), (4, 52)}


def region_of(cube):
    uv = cube.get("faces", {}).get("north", {}).get("uv")
    if not uv:
        return None
    return (round(uv[0]), round(uv[1]))


def new_bone(name, origin, cube_uuids):
    return {
        "name": name,
        "origin": origin,
        "color": 0,
        "uuid": str(uuid.uuid4()),
        "export": True,
        "isOpen": False,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": cube_uuids,
    }


def convert(path, apply, keep_layers):
    data = json.load(open(path, encoding="utf-8"))
    if "groups" in data:
        return [f"{path}: formato 5.0 (bbmodel salvo no Blockbench novo) — converta a mao ou reabra no formato antigo"]

    elements = {e["uuid"]: e for e in data["elements"]}
    report = []
    dropped = []
    claimed = set()

    def walk(nodes):
        for node in nodes:
            if not isinstance(node, dict):
                continue

            # Guardado antes de mexer nos filhos: o bone de limb novo nao deve ser visitado.
            sub_bones = [c for c in node.get("children", []) if isinstance(c, dict)]
            name = node.get("name", "")

            if name in REGIONS.values():
                walk(sub_bones)
                continue

            cube_uuids = [c for c in node.get("children", []) if not isinstance(c, dict) and c in elements]
            base = [c for c in cube_uuids if region_of(elements[c]) in REGIONS]
            layer = [c for c in cube_uuids if region_of(elements[c]) in LAYERS]

            token = REGIONS[region_of(elements[base[0]])] if base else None
            if token and token in claimed:
                report.append(f"  {name}: ignorado, '{token}' ja foi usado (cosmetico reaproveitando a UV?)")
                token = None

            if token:
                claimed.add(token)
                # Pivo sai do cubo base: avental, ferramenta e capa mudariam a caixa.
                origin = required_origin([elements[c] for c in base], token)
                moved = base + ([] if keep_layers else layer)
                report.append(f"  {name}: bone '{token}' criado no pivo {origin}")
                node["children"] = [c for c in node["children"] if c not in moved]
                node["children"].insert(0, new_bone(token, origin, moved))
            # So apaga bone que e SO camada externa: um cosmetico pode ter um cubo
            # solto reaproveitando essa regiao da UV (a espada do blacksmith_1).
            elif layer and not base and len(layer) == len(cube_uuids) and not keep_layers:
                dropped.extend(layer)
                report.append(f"  {name}: camada externa removida ({len(layer)} cubo(s))")
                node["children"] = [c for c in node["children"] if c not in layer]

            walk(sub_bones)

    walk(data["outliner"])

    if dropped:
        data["elements"] = [e for e in data["elements"] if e["uuid"] not in dropped]

    if apply and report:
        shutil.copyfile(path, path + ".bak")
        json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False)

    return report


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    apply = "--aplicar" in sys.argv
    keep_layers = "--manter-camadas" in sys.argv

    if not args:
        print(__doc__)
        raise SystemExit(2)

    for path in args:
        print(path)
        lines = convert(path, apply, keep_layers)
        print("\n".join(lines) if lines else "  nada a fazer")

    if not apply:
        print("\nSimulacao. Rode de novo com --aplicar para gravar.")


if __name__ == "__main__":
    main()
