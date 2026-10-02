"""Validate research content, shaped recipes, tags, models and bilingual UI coverage."""
import json,re
from collections import Counter
from pathlib import Path
root=Path(__file__).resolve().parents[2]
res=root/'src/main/resources'
def read(path):return json.loads((res/path).read_text(encoding='utf-8'))
core=read('data/changede/recipes/painite_workbench_core.json')
table=read('data/changede/recipes/latex_skill_research_table.json')
assert core['pattern']==[' X ','XYX',' X ']
assert core['key']=={'X':{'item':'changede:painite_ingot'},'Y':{'item':'minecraft:crafting_table'}}
assert core['result']=={'item':'changede:painite_workbench_core','count':1}
assert table['pattern']==['AZA','YXY','YYY']
assert table['key']=={'A':{'tag':'changede:latex_planks'},'Z':{'item':'minecraft:book'},'Y':{'item':'changede:latex_ingot'},'X':{'item':'changede:painite_workbench_core'}}
assert table['result']=={'item':'changede:latex_skill_research_table','count':1}
for recipe in [core,table]:
    assert recipe['type']=='minecraft:crafting_shaped'
    chars=Counter(''.join(recipe['pattern']).replace(' ',''))
    assert set(chars)==set(recipe['key'])
assert Counter(''.join(core['pattern']))['X']==4
assert Counter(''.join(table['pattern']))['Y']==5
for kind in ['items','blocks']:
    assert set(read(f'data/changede/tags/{kind}/latex_planks.json')['values'])=={'changede:dark_latex_planks','changede:white_latex_planks'}
count=0
projects=set()
for p in (res/'data/changede/latex_skill_trees').glob('*.json'):
    tree=json.loads(p.read_text(encoding='utf-8'))
    for n in tree['nodes']:
        research=n.get('research','none')
        assert research in {'none','race','core'}
        if n['key'] and tree['scope']!='global':
            assert research==('core' if n['id']=='changede:feline_nine_lives' else 'race')
            assert n['cost']==0 if n['id'] in {'changede:dark_shell','changede:white_vitality'} else n['cost']>0
            count+=1
            projects.add(n['id'].split(':')[1])
        else:assert research=='none'
assert count==27
definitions={p.stem:json.loads(p.read_text(encoding='utf-8')) for p in (res/'data/changede/skill_research').glob('*.json')}
assert set(definitions)==projects
for project,d in definitions.items():
    assert 0<=d['duration_ticks']<=20*60*60*24 and d['duration_ticks']%20==0
    assert 0<=d['wlp_per_second']<=100000
    assert 'total_wlp' not in d # Per-second cost is the primary configuration.
    if project in {'dark_shell','white_vitality'}:
        assert d=={'duration_ticks':0,'wlp_per_second':0,'tier':'free','startup_materials':[]}
    else:
        assert d['duration_ticks']>0 and d['wlp_per_second']>0 and d['startup_materials']
    assert len({m['item'] for m in d['startup_materials']})==len(d['startup_materials'])
    assert all(m['count']>0 for m in d['startup_materials'])
cat=definitions['feline_core'];dragon=definitions['dragon_core'];nine=definitions['feline_nine_lives']
assert cat['duration_ticks']<dragon['duration_ticks']<=nine['duration_ticks']
assert cat['wlp_per_second']<dragon['wlp_per_second']<nine['wlp_per_second']
state=read('assets/changede/blockstates/latex_skill_research_table.json')
assert set(state['variants'])=={'facing='+d for d in ['north','south','east','west']}
loot=read('data/changede/loot_tables/blocks/latex_skill_research_table.json')
assert loot['pools'][0]['entries']==[{'type':'minecraft:item','name':'changede:latex_skill_research_table'}]
assert loot['pools'][0]['conditions']==[{'condition':'minecraft:survives_explosion'}]
for tool in ['axe','pickaxe']:
    assert 'changede:latex_skill_research_table' in read(f'data/minecraft/tags/blocks/mineable/{tool}.json')['values']
for path in ['assets/changede/models/block/latex_skill_research_table.json','assets/changede/models/item/painite_workbench_core.json']:
    model=read(path)
    for texture in model['textures'].values():
        if texture.startswith('changede:'):assert (res/('assets/changede/textures/'+texture.split(':',1)[1]+'.png')).is_file(),texture
    for element in model['elements']:
        assert all(0<=a<b<=16 for a,b in zip(element['from'],element['to']))
        assert all(face['texture'][1:] in model['textures'] for face in element['faces'].values())
keys=set()
for path in ['src/main/java/github/com/gengyoubo/CE/client/LatexSkillResearchScreen.java','src/main/java/github/com/gengyoubo/CE/Block/LatexSkillResearchTableBlock.java']:
    keys.update(re.findall(r'Component.translatable\("([a-z0-9_.]+)"\)',(root/path).read_text(encoding='utf-8')))
for lang in ['zh_cn','en_us']:
    data=read(f'assets/changede/lang/{lang}.json')
    assert keys<=data.keys(),keys-data.keys()
    assert 'screen.changede.skills.reason.research_table' in data
    assert 'item.changede.painite_workbench_core' in data
    assert {'screen.changede.research.status.'+s for s in ['unstarted','paused','running','completed','no_wlp','station_missing','unavailable']}<=data.keys()
    assert {'screen.changede.research.tier.'+s for s in ['free','basic','intermediate','advanced','rare','core']}<=data.keys()
print('PASS: exact recipes, 27 complete research definitions, per-second rates, free latex keys, balance tiers, block models, drops and bilingual GUI.')
