"""Confere (e corrige) os pivos dos bones de player limb do ModelEngine num .bbmodel.

O bone de limb nao renderiza os cubos do artista: o ModelEngine troca o modelo do
bone por um item PLAYER_HEAD e a posicao final vem do display transform de
`assets/modelengine/models/custom/entities/player/*.json`. O cranio ocupa
y em [-8, 0] em relacao ao centro do item, entao a peca PENDURA abaixo do pivo:

    span_y = pivo + translation_y + scale_y * [-8, 0]

Dai o pivo exigido por tipo de limb (com os numeros do ModelEngine 4.1):

    phead          pivo = base do cubo        (translation 7.5, scale 0.9375)
    pbody          pivo = topo do cubo        (translation 0,   scale 1.40625)
    prleg/plleg    pivo = topo do cubo        (translation 0,   scale 1.40625)
    prarm/plarm    pivo = topo do cubo - 1.875
    x              pivo = centro do cubo -/+ 0.9375 nos bracos, centro nos demais

Uso:
    python scripts/limb_pivots.py <arquivo.bbmodel> [--fix]

Sem --fix so lista as diferencas. Com --fix, bones sem animacao tem o pivo
corrigido no lugar; bones com keyframes ganham um filho novo com o token de limb
(o pai continua animando no pivo original).
"""

import json
import re
import sys
import uuid

TOKEN_SPLIT = re.compile(r"_(?![^\[]*\])")

LIMB_TOKENS = {
    "phead": ("base", 0.0, 0.0),
    "pbody": ("topo", 0.0, 0.0),
    "prarm": ("topo", -1.875, -0.9375),
    "plarm": ("topo", -1.875, 0.9375),
    "prleg": ("topo", 0.0, 0.0),
    "plleg": ("topo", 0.0, 0.0),
}

# Meio pixel: abaixo disso a diferenca nao aparece em tela e so gera ruido.
TOLERANCE = 0.5


def limb_token(name):
    for token in TOKEN_SPLIT.split(name):
        key = token.split("[")[0]
        if key in LIMB_TOKENS:
            return key
    return None


def load(path):
    data = json.load(open(path, encoding="utf-8"))
    elements = {e["uuid"]: e for e in data["elements"]}
    groups = {g["uuid"]: g for g in data.get("groups", [])} if "groups" in data else None
    return data, elements, groups


def bone_of(node, groups):
    if groups is not None:
        return groups.get(node["uuid"], {})
    return node


def animated(data, node_uuid):
    for animation in data.get("animations", []):
        if node_uuid in animation.get("animators", {}):
            return True
    return False


def required_origin(cubes, token):
    anchor, offset_y, offset_x = LIMB_TOKENS[token]
    min_y = min(c["from"][1] for c in cubes)
    max_y = max(c["to"][1] for c in cubes)
    min_x = min(c["from"][0] for c in cubes)
    max_x = max(c["to"][0] for c in cubes)
    min_z = min(c["from"][2] for c in cubes)
    max_z = max(c["to"][2] for c in cubes)

    y = (min_y if anchor == "base" else max_y) + offset_y
    x = (min_x + max_x) / 2 + offset_x
    z = (min_z + max_z) / 2
    return [round(x, 4), round(y, 4), round(z, 4)]


def walk(nodes, data, elements, groups, findings):
    for node in nodes:
        if not isinstance(node, dict):
            continue

        bone = bone_of(node, groups)
        name = bone.get("name")
        cubes = [elements[c] for c in node.get("children", []) if not isinstance(c, dict) and c in elements]
        token = limb_token(name) if name else None

        if token and cubes:
            wanted = required_origin(cubes, token)
            current = bone.get("origin", [0, 0, 0])
            diff = [round(w - c, 4) for w, c in zip(wanted, current)]
            if any(abs(d) > TOLERANCE for d in diff):
                findings.append((node, bone, name, token, current, wanted, diff, cubes))

        walk(node.get("children", []), data, elements, groups, findings)


def fix(node, bone, token, wanted, cubes, data, groups):
    if not animated(data, node["uuid"]):
        bone["origin"] = wanted
        return f"pivo ajustado para {wanted}"

    limb_uuid = str(uuid.uuid4())
    child = dict(bone)
    child.update({
        "uuid": limb_uuid,
        "name": token,
        "origin": wanted,
        "rotation": [0, 0, 0],
        "children": [],
    })
    data.setdefault("groups", []).append(child)

    bone["name"] = TOKEN_SPLIT.split(bone["name"])[0] or bone["name"]

    cube_uuids = [c["uuid"] for c in cubes]
    node["children"] = [c for c in node.get("children", []) if c not in cube_uuids]
    node["children"].insert(0, {"uuid": limb_uuid, "isOpen": False, "children": cube_uuids})

    return f"bone '{bone['name']}' segue animando; filho '{token}' criado no pivo {wanted}"


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(2)

    path = sys.argv[1]
    apply_fix = "--fix" in sys.argv

    data, elements, groups = load(path)
    findings = []
    walk(data["outliner"], data, elements, groups, findings)

    if not findings:
        print("Todos os bones de limb estao com o pivo certo.")
        return

    for node, bone, name, token, current, wanted, diff, cubes in findings:
        print(f"{name} ({token}): pivo {current} -> {wanted}  (delta {diff})")
        if apply_fix:
            print("  " + fix(node, bone, token, wanted, cubes, data, groups))

    if apply_fix:
        json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False)
        print(f"\n{len(findings)} bone(s) corrigido(s) em {path}")
    else:
        print(f"\n{len(findings)} bone(s) fora do lugar. Rode de novo com --fix para corrigir.")


if __name__ == "__main__":
    main()
