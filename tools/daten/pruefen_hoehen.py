#!/usr/bin/env python3
"""Prüft die Weltenhöhe jedes Universumstyps (Meilenstein 11, Entscheidung M11-08).

Jeder Typ in UniverseType.java trägt minY und height (Standard -64 / 384). Der passende dimension_type muss
genau dieselben Werte haben, logical_height darf nicht größer sein, und mehr als 1024 Blöcke Höhe sind verboten.
Aufruf: python3 tools/daten/pruefen_hoehen.py (aus dem Repo-Wurzelordner)
"""
import json
import pathlib
import re
import sys

WURZEL = pathlib.Path(__file__).resolve().parents[2]
TYPEN = WURZEL / "src/main/java/dev/riftverse/universe/UniverseType.java"
DIM_TYPE = WURZEL / "src/main/resources/data/riftverse/dimension_type"
DIM = WURZEL / "src/main/resources/data/riftverse/dimension"
MAX_HOEHE = 1024

# NAME("id", stufe, schwerkraft, gewicht[, einzigartig, entwicklung[, minY, height]])
MUSTER = re.compile(r'^\s{4}([A-Z_]+)\("([a-z_]+)",\s*([^)]*)\)[,;]', re.M)


def typen():
    text = TYPEN.read_text(encoding="utf-8")
    for m in MUSTER.finditer(text):
        args = [a.strip() for a in m.group(3).split(",")]
        min_y, hoehe = (-64, 384)
        if len(args) >= 7:
            min_y, hoehe = int(args[5]), int(args[6])
        yield m.group(1), m.group(2), min_y, hoehe


def main():
    fehler = []
    gefunden = list(typen())
    if not gefunden:
        fehler.append("Keine Universumstypen in UniverseType.java gefunden")
    for name, ident, min_y, hoehe in gefunden:
        if hoehe > MAX_HOEHE:
            fehler.append(f"{name}: Höhe {hoehe} über {MAX_HOEHE}")
        if hoehe % 16 or min_y % 16:
            fehler.append(f"{name}: minY {min_y} und Höhe {hoehe} müssen Vielfache von 16 sein")
        datei = DIM_TYPE / f"{ident}.json"
        if not datei.exists():
            fehler.append(f"{name}: dimension_type/{ident}.json fehlt")
            continue
        if not (DIM / f"{ident}.json").exists():
            fehler.append(f"{name}: dimension/{ident}.json fehlt")
        d = json.loads(datei.read_text(encoding="utf-8"))
        if d.get("min_y") != min_y or d.get("height") != hoehe:
            fehler.append(f"{name}: dimension_type hat min_y {d.get('min_y')} / height {d.get('height')}, "
                          f"UniverseType sagt {min_y} / {hoehe}")
        if d.get("logical_height", 0) > d.get("height", 0):
            fehler.append(f"{name}: logical_height {d.get('logical_height')} größer als height {d.get('height')}")
    if fehler:
        for f in fehler:
            print("FEHLER:", f)
        sys.exit(1)
    print(f"Weltenhöhen ok ({len(gefunden)} Universumstypen)")


if __name__ == "__main__":
    main()
