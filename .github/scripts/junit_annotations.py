"""Transforma falhas de relatórios JUnit (XML) em anotações do GitHub Actions e no resumo da execução.

As anotações aparecem na página da execução e na API de check-runs, sem precisar baixar logs.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

root = sys.argv[1] if len(sys.argv) > 1 else "."
failures = []
for path in glob.glob(os.path.join(root, "**", "*.xml"), recursive=True):
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        continue
    for case in tree.iter("testcase"):
        for tag in ("failure", "error"):
            node = case.find(tag)
            if node is None:
                continue
            text = (node.get("message") or "") + "\n" + (node.text or "")
            lines = [l for l in text.splitlines() if l.strip()]
            head = lines[:6]
            ours = [l.strip() for l in lines if "com.miaugrid" in l][:6]
            body = "\n".join(head + ["..."] + ours)[:3000]
            failures.append((case.get("classname", "?").split(".")[-1], case.get("name", "?"), body))

def esc(v):
    return v.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")

if not failures:
    print(f"::warning title=Sem relatórios JUnit::Nenhuma falha encontrada em {root} (a build pode ter falhado antes dos testes)")
for cls, name, body in failures[:10]:
    print(f"::error title={esc(cls)}.{esc(name)}::{esc(body)}")

summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary, "a", encoding="utf-8") as f:
        f.write(f"## Falhas de teste ({len(failures)})\n\n")
        for cls, name, body in failures:
            f.write(f"### {cls}.{name}\n\n```\n{body}\n```\n\n")
