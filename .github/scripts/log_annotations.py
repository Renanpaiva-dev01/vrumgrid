"""Extrai trechos relevantes de logs (Gradle e logcat) e os publica como anotações do GitHub Actions.

Útil quando os logs completos não podem ser baixados: as anotações ficam na API de check-runs.
"""
import os
import re
import sys

PATTERNS = re.compile(
    r"FATAL EXCEPTION|AndroidRuntime|Process crashed|Exception|Caused by|at com\.miaugrid|FAILED|ANR in|"
    r"Instrumentation run failed|TestRunner: failed|TestRunner: finished|TestRunner: started"
)


def esc(v):
    return v.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


budget = 9
for path in sys.argv[1:]:
    if not os.path.exists(path):
        print(f"::warning title=Log ausente::{path} não foi gerado")
        continue
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.readlines()
    hits = []
    for i, line in enumerate(lines):
        if PATTERNS.search(line) and "at androidx." not in line and "at java." not in line:
            hits.append(line.rstrip()[:300])
    # Agrupa em blocos de ~3500 caracteres por anotação.
    block = ""
    for h in hits:
        if len(block) + len(h) > 3500:
            if budget > 0:
                print(f"::error title={esc(os.path.basename(path))}::{esc(block)}")
                budget -= 1
            block = ""
        block += h + "\n"
    if block and budget > 0:
        print(f"::error title={esc(os.path.basename(path))}::{esc(block)}")
        budget -= 1
