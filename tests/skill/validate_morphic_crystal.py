"""Validate the agreed ore, worldgen, alloy/core recipes, models and research material chain."""
import argparse,json,os,struct
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[2]
res=root/'src/main/resources'
def read(path):return json.loads((res/path).read_text(encoding='utf-8'))
ores=['dark_latex_morphic_crystal_ore','white_latex_morphic_crystal_ore']
parser=argparse.ArgumentParser()
parser.add_argument('--vanilla-jar',type=Path,default=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle'))/'caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar')
args=parser.parse_args()
with ZipFile(args.vanilla_jar) as vanilla:
    for ore in ores:
        copper=json.loads(vanilla.read('data/minecraft/loot_tables/blocks/copper_ore.json'))
        for entry in copper['pools'][0]['entries'][0]['children']:
            entry['name']='changede:'+ore if entry['name']=='minecraft:copper_ore' else 'changede:morphic_crystal'
        copper['random_sequence']='changede:blocks/'+ore
        assert read(f'data/changede/loot_tables/blocks/{ore}.json')==copper
        for tag in ['minecraft/tags/blocks/mineable/pickaxe','minecraft/tags/blocks/needs_iron_tool',
                    'forge/tags/blocks/ores','forge/tags/blocks/ores/morphic_crystal',
                    'forge/tags/items/ores','forge/tags/items/ores/morphic_crystal']:
            assert 'changede:'+ore in read('data/'+tag+'.json')['values']
        assert read(f'assets/changede/blockstates/{ore}.json')['variants']['']['model']=='changede:block/'+ore
        assert read(f'assets/changede/models/item/{ore}.json')['parent']=='changede:block/'+ore
    for suffix in ['small','buried','large']:
        expected=json.loads(vanilla.read(f'data/minecraft/worldgen/configured_feature/ore_diamond_{suffix}.json'))['config']
        actual=read(f'data/changede/worldgen/configured_feature/ore_morphic_crystal_{suffix}.json')
        assert actual['type']=='changede:morphic_crystal_ore'
        assert actual['config']['size']==expected['size']
        assert actual['config']['discard_chance_on_air_exposure']==expected['discard_chance_on_air_exposure']
        assert actual['config']['targets']==[
            {'target':{'predicate_type':'minecraft:block_match','block':'changede:'+color+'_latex_stone'},
             'state':{'Name':'changede:'+ore}} for color,ore in zip(['dark','white'],ores)]
    for suffix in ['','_buried','_large']:
        expected=json.loads(vanilla.read(f'data/minecraft/worldgen/placed_feature/ore_diamond{suffix}.json'))
        expected['feature']='changede:ore_morphic_crystal'+(suffix or '_small')
        assert read(f'data/changede/worldgen/placed_feature/ore_morphic_crystal{suffix}.json')==expected
    for name in [*['block/'+ore for ore in ores],*['item/'+n for n in ['morphic_crystal','morphic_crystal_alloy','morphic_crystal_core']]]:
        model=read('assets/changede/models/'+name+'.json')
        is_block=name.startswith('block/')
        assert model=={'parent':'minecraft:block/cube_all' if is_block else 'minecraft:item/generated',
                       'textures':{'all' if is_block else 'layer0':'changede:'+name}}
        for texture in model['textures'].values():
            namespace,path=texture.split(':',1)
            png=(res/f'assets/{namespace}/textures/{path}.png').read_bytes()
            assert png[:8]==b'\x89PNG\r\n\x1a\n' and png[12:16]==b'IHDR',texture
            assert struct.unpack('>II',png[16:24])==(16,16),texture
            assert png[24]==8 and png[25] in ([2,6] if is_block else [6]),texture

biomes=read('data/changede/dimension/latex_space.json')['generator']['biome_source']['biomes']
assert set(read('data/changede/tags/worldgen/biome/latex_space.json')['values'])=={b['biome'] for b in biomes}
modifier=read('data/changede/forge/biome_modifier/morphic_crystal_ores.json')
assert modifier=={'type':'forge:add_features','biomes':'#changede:latex_space',
                 'features':['changede:ore_morphic_crystal','changede:ore_morphic_crystal_buried','changede:ore_morphic_crystal_large'],'step':'underground_ores'}
alloy=read('data/changede/recipes/alloy_furnace/morphic_crystal_alloy.json')
assert alloy=={'type':'changede:alloy_furnace','X':{'item':'changede:morphic_crystal'},'Y':{'item':'changede:latex_ingot'},
               'time':10,'lp_per_second':200,'result':{'item':'changede:morphic_crystal_alloy','count':1}}
core=read('data/changede/recipes/morphic_crystal_core.json')
assert core['pattern']==[' X ','XYX',' X ']
assert core['key']=={'X':{'item':'changede:morphic_crystal_alloy'},'Y':{'item':'changede:painite_ingot'}}
assert core['result']=={'item':'changede:morphic_crystal_core','count':1}
assert 'morphic' not in json.dumps(read('data/changede/recipes/latex_skill_research_table.json'))
assert 'morphic' not in json.dumps(read('data/changede/recipes/latex_painting_portal.json'))
counts={'canine':2,'arthropod':2,'sea':3,'mer':3,'reptile':3,'insect':3,'arachnid':3,'bird':3,'ray':3,'marine_mammal':3,
        'air':4,'shark':4,'cephalopod':4,'feline':8,'dragon':16,'dark':0,'white':0}
total=0
for p in (res/'data/changede/latex_skill_trees').glob('*.json'):
    tree=json.loads(p.read_text(encoding='utf-8'))
    for node in tree['nodes']:
        if node.get('research','none')=='none':continue
        project=read(f'data/changede/skill_research/{node["id"].split(":")[1]}.json')
        count=32 if node['research']=='core' else counts.get(p.stem,1)
        assert project['startup_materials']==([] if count==0 else [{'item':'changede:morphic_crystal_core','count':count}])
        total+=1
assert total==27
for language in ['zh_cn','en_us']:
    lang=read('assets/changede/lang/'+language+'.json')
    for ore in ores:assert 'block.changede.'+ore in lang
    for item in ['morphic_crystal','morphic_crystal_alloy','morphic_crystal_core']:assert 'item.changede.'+item in lang
    for kind in ['ore','crystal','alloy','core']:assert 'tooltip.changede.morphic_crystal.'+kind in lang
print('PASS: copper-style loot, iron tags, shared diamond generation, all seven biomes, native models/textures, LP recipe, core recipe and 27 research costs.')
