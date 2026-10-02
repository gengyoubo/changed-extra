"""Validate the shipped multi-affiliation DAG against audited Forms and localization."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
RES=ROOT/'src/main/resources'
def unique(pairs):
    result={}
    for k,v in pairs:
        assert k not in result, f'Duplicate key: {k}'
        result[k]=v
    return result
def read(p):return json.loads(p.read_text(encoding='utf-8'),object_pairs_hook=unique)
catalog=read(ROOT/'FORM_AFFILIATIONS.json')['forms']
trees={p.stem:read(p) for p in (RES/'data/changede/latex_skill_trees').glob('*.json')}
langs=[read(RES/f'assets/changede/lang/{lang}.json') for lang in ['zh_cn','en_us']]
nodes={};positions=set()
effects=set(re.findall(r'"([a-z_]+)"',(ROOT/'src/main/java/github/com/gengyoubo/CE/skill/SkillMechanics.java').read_text(encoding='utf-8').split('private static final Set<String> EFFECTS = Set.of(')[1].split(');')[0]))
for branch,tree in trees.items():
    assert len(tree['nodes'])<=30,branch
    if branch not in {'trunk','dark','white','yufeng'}:
        dimension='movement' if branch in {'sea','land','air'} else 'body' if branch in {'biped','feral','taur','snake','mer'} else 'families'
        expected={r['form'] for r in catalog if (r[dimension]==branch if dimension=='body' else branch in r[dimension])}
        assert set(tree['forms'])==expected and expected,branch
    for n in tree['nodes']:
        assert n['id'] not in nodes
        assert (n['x'],n['y']) not in positions,('Overlapping nodes',n['id'])
        positions.add((n['x'],n['y']));nodes[n['id']]=n
        for lang in langs:assert n['title'] in lang and n['description'] in lang,n['id']
        for reward in n['rewards']:
            assert reward['type'] in {'changede:none','changede:attribute','changede:mechanic'}
            if reward['type']=='changede:mechanic':
                assert reward['effect'] in effects
                assert reward['value']>0
                for lang in langs:assert 'skill.changede.mechanic.'+reward['effect'] in lang
completed=set()
while len(completed)<len(nodes):
    ready={id for id,n in nodes.items() if id not in completed and set(n['parents'])<=completed}
    assert ready,'Missing prerequisites or cycle'
    completed|=ready
assert nodes['changede:arthropod_core']['parents']==['changede:latex_mastery']
for child in ['insect','arachnid']:assert nodes['changede:'+child+'_core']['parents']==['changede:arthropod_core']
assert all(r.get('effect')!='arthropod' for n in trees['insect']['nodes'] for r in n['rewards'])
assert not any(r.get('effect') in {'insect_core','insect_recovery'} for n in trees['arachnid']['nodes'] for r in n['rewards'])
assert trees['yufeng']['nodes']==[]
assert nodes['changede:flight_technique']['parents']==['changede:air_core']
for branch in ['dark','white']:assert trees[branch]['nodes'][0]['parents']==['changede:latex_mastery']
v=read(RES/'assets/changede/latex_skill_visuals/default.json');themes={t['id'] for t in v['themes']}
assert len({r['id'] for r in v['regions']})==len(v['regions'])
assert all(r['theme'] in themes and r['width']>0 and r['height']>0 for r in v['regions'])
for branch,tree in trees.items():
    if branch in {'trunk','yufeng'}:continue
    region=next(r for r in v['regions'] if r['id']=='changede:'+branch+'_branch')
    for n in tree['nodes']:assert region['x']<n['x']<region['x']+region['width'] and region['y']<n['y']<region['y']+region['height'],n['id']
# Run visibility/activation simulations for each audited Form, including mixed species.
def eligible(form):
    result=set()
    for branch,tree in trees.items():
        if tree.get('latex_type') in {'dark','white'} and tree['latex_type']!=form['latex']:continue
        if tree.get('forms') and form['form'] not in tree['forms']:continue
        result|={n['id'] for n in tree['nodes']}
    return result
for f in catalog:
    ids=eligible(f)
    assert 'changede:latex_mastery' in ids
    assert all(set(nodes[id]['parents'])<=ids for id in ids),(f['form'],'Unreachable parent')
    if not f['families']:assert not any('family' in id for id in ids)
stiger=next(f for f in catalog if f['form']=='changed:form_latex_stiger')
assert {'changede:feline_core','changede:arthropod_core','changede:arachnid_core'}<=eligible(stiger)
assert 'changede:insect_core' not in eligible(stiger)
hybrid=next(f for f in catalog if f['form']=='changed_addon:form_latex_dragon_snow_leopard_shark')
assert {'changede:sea_core','changede:air_core','changede:feline_core','changede:dragon_core','changede:shark_core'}<=eligible(hybrid)
print(f'PASS: {len(nodes)} nodes, {sum(bool(t["nodes"]) for t in trees.values())} branches, 150 Form combinations, <=30 nodes/branch, unique coordinates, DAG, selectors, localization and regions.')
