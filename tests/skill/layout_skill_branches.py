"""Arrange existing skill data symmetrically without changing gameplay definitions."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'src/main/resources'
TREES = RES / 'data/changede/latex_skill_trees'
# From the trunk outwards: each pair occupies matching left/right lanes.
PAIRS = [
    ('dark', 'white'), ('biped', 'feral'), ('taur', 'snake'),
    ('mer', 'sea'), ('land', 'air'), ('feline', 'canine'),
    ('dragon', 'reptile'), ('arthropod', 'bird'), ('shark', 'ray'),
    ('fish', 'marine_mammal'), ('cephalopod', 'plant'),
    ('other_mammal', 'humanoid'),
]

def save(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n',
                    encoding='utf-8', newline='\n')

def arrange():
    trees = {p.stem: json.loads(p.read_text(encoding='utf-8'))
             for p in TREES.glob('*.json')}
    definitions = {name: [{k: v for k, v in n.items() if k != 'x'}
                          for n in tree['nodes']] for name, tree in trees.items()}
    centers = {}
    cursor = 0
    for left, right in PAIRS:
        nested = left == 'arthropod' or right == 'arthropod'
        center = cursor + (7 if nested else 3)
        centers[left], centers[right] = -center, center
        cursor += 12 if nested else 4
    centers['insect'] = centers['arthropod'] + 3
    centers['arachnid'] = centers['arthropod'] - 3
    for name, tree in trees.items():
        if name == 'trunk':
            for node in tree['nodes']:
                node['x'] = 0
        elif tree['nodes']:
            center = centers[name]
            original = tree['nodes'][0]['x']
            offsets = [node['x'] - original for node in tree['nodes']]
            # Mirror left-hand lanes on first run; preserve them on repeated runs.
            mirror = -1 if center < 0 and sum(offsets) > 0 else 1
            for node, offset in zip(tree['nodes'], offsets):
                node['x'] = center + mirror * offset
        assert definitions[name] == [{k: v for k, v in n.items() if k != 'x'}
                                     for n in tree['nodes']], name
        save(TREES / (name + '.json'), tree)
    visual_path = RES / 'assets/changede/latex_skill_visuals/default.json'
    visual = json.loads(visual_path.read_text(encoding='utf-8'))
    for region in visual['regions']:
        name = region['id'].removeprefix('changede:').removesuffix('_branch')
        if name not in centers:
            continue
        if 'branch' in region:
            continue  # Runtime regions follow the branch bounds, not design coordinates.
        xs = [node['x'] for node in trees[name]['nodes']]
        region['x'] = min(xs) - .75
        region['width'] = max(xs) - min(xs) + 1.5
    save(visual_path, visual)
    print('Arranged 12 top-level branches per side; common trunk centered at x=0.')

if __name__ == '__main__':
    arrange()
