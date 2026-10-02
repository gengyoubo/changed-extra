"""Export real nodes/selectors for the dependency-free Java layout regression test."""
import json
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[2]
trees = {p.stem: json.loads(p.read_text(encoding='utf-8'))
         for p in (root/'src/main/resources/data/changede/latex_skill_trees').glob('*.json')}
forms = json.loads((root/'FORM_AFFILIATIONS.json').read_text(encoding='utf-8'))['forms']
lines = []
for branch, tree in trees.items():
    for node in tree['nodes']:
        lines.append('\t'.join(['node', node['id'], 'changede:'+branch, str(node['x']),
                               str(node['y']), str(tree['scope']=='global').lower()]))
for form in forms:
    visible = []
    for branch, tree in trees.items():
        if not tree['nodes'] or tree['scope']=='global':
            continue
        if tree.get('latex_type', 'any') not in {'any', form['latex']}:
            continue
        if tree.get('forms') and form['form'] not in tree['forms']:
            continue
        visible.append('changede:'+branch)
    lines.append('\t'.join(['form', form['form'], ','.join(sorted(visible))]))
output = Path(sys.argv[1])
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text('\n'.join(lines)+'\n', encoding='utf-8', newline='\n')
