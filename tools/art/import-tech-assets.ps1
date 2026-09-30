param(
    [string]$MachineAtlas,
    [string]$ItemAtlas
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$assets = Join-Path $repo 'src/main/resources/assets/changede'
$archive = Join-Path $repo 'docs/art/technology'
New-Item -ItemType Directory -Force $archive | Out-Null
Copy-Item -LiteralPath $MachineAtlas -Destination (Join-Path $archive 'machine-atlas.png')
Copy-Item -LiteralPath $ItemAtlas -Destination (Join-Path $archive 'item-atlas.png')
function Export-Tiles($source, $names, $folder) {
    $atlas = [Drawing.Bitmap]::new($source)
    New-Item -ItemType Directory -Force (Join-Path $assets $folder) | Out-Null
    for ($i=0; $i -lt $names.Count; $i++) {
        # Historical plate assets are imported separately from the original game textures.
        if ($names[$i] -like 'plate*' -or $names[$i] -in @('peach','enchanted_golden_orange')) { continue }
        $tile = [Drawing.Bitmap]::new(32,32,[Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g=[Drawing.Graphics]::FromImage($tile)
        $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
        $rect=[Drawing.RectangleF]::new(($i%4)*$atlas.Width/4,[Math]::Floor($i/4)*$atlas.Height/4,$atlas.Width/4,$atlas.Height/4)
        $g.DrawImage($atlas,[Drawing.RectangleF]::new(0,0,32,32),$rect,[Drawing.GraphicsUnit]::Pixel)
        $tile.Save((Join-Path $assets ($folder+'/'+$names[$i]+'.png')),[Drawing.Imaging.ImageFormat]::Png)
        $g.Dispose(); $tile.Dispose()
    }
    $atlas.Dispose()
}
Export-Tiles $MachineAtlas @('machine_side','machine_top','basic_crystal_generator_front','basic_latex_fluid_generator_front','basic_latex_purifier_front','basic_alloy_furnace_front','white_latex_power_converter_front','dark_latex_power_converter_front','orange_producer_front','energy_pipe','item_pipe','fluid_pipe','machine_rear','purifier_top','panel_light','panel_dark') 'textures/block'
Export-Tiles $ItemAtlas @('pipe_wrench','iridium_ingot','painite_ingot','chain_ingot','plate','plate_helmet','plate_chestplate','plate_leggings','plate_boots','riding_stick','ridden_stick','remote_riding_stick','peach','enchanted_golden_orange','tank_icon','energy_icon') 'textures/item'
foreach($shade in @('light','dark')) {
    Copy-Item -LiteralPath (Join-Path $assets ('textures/block/panel_'+$shade+'.png')) -Destination (Join-Path $assets ('textures/gui/machine_panel_'+$shade+'.png'))
}
function Write-Json($path,$obj) {
    $obj | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath (Join-Path $assets $path) -Encoding utf8
}
foreach($id in @('basic_crystal_generator','basic_latex_fluid_generator','basic_latex_purifier','basic_alloy_furnace','white_latex_power_converter','dark_latex_power_converter','orange_producer')) {
    $top=if($id -eq 'basic_latex_purifier'){'purifier_top'}else{'machine_top'}
    Write-Json ('models/block/'+$id+'.json') ([ordered]@{parent='minecraft:block/cube';textures=[ordered]@{particle='changede:block/machine_side';down='changede:block/machine_top';up=('changede:block/'+$top);north=('changede:block/'+$id+'_front');south='changede:block/machine_rear';west='changede:block/machine_side';east='changede:block/machine_side'}})
    Write-Json ('models/item/'+$id+'.json') @{parent=('changede:block/'+$id)}
    $statePath=Join-Path $assets ('blockstates/'+$id+'.json')
    $state=(Get-Content -LiteralPath $statePath -Raw).Replace('changede:block/basic_generator','changede:block/'+$id)
    $state | Set-Content -LiteralPath $statePath -Encoding utf8
}
foreach($id in @('pipe_wrench','iridium_ingot','painite_ingot','chain_ingot','plate','plate_helmet','plate_chestplate','plate_leggings','plate_boots','riding_stick','ridden_stick','remote_riding_stick')) {
    Write-Json ('models/item/'+$id+'.json') @{parent=$(if($id -like '*stick' -or $id -eq 'pipe_wrench'){'minecraft:item/handheld'}else{'minecraft:item/generated'});textures=@{layer0=('changede:item/'+$id)}}
}
foreach($kind in @('energy','item','fluid')) {
    foreach($part in @('center','arm')) {
        $model=Get-Content -LiteralPath (Join-Path $assets ('models/block/pipe_'+$part+'.json')) -Raw | ConvertFrom-Json
        $model.textures.all='changede:block/'+$kind+'_pipe'; $model.textures.particle=$model.textures.all
        # Explicit UVs map the small pipe cross-section to the whole stripe texture.
        foreach($element in $model.elements){foreach($face in $element.faces.psobject.Properties){$face.Value | Add-Member -NotePropertyName uv -NotePropertyValue @(0,0,16,16) -Force}}
        Write-Json ('models/block/'+$kind+'_pipe_'+$part+'.json') $model
    }
    $id=if($kind -eq 'energy'){'basic_wire'}else{'basic_'+$kind+'_pipe'}
    $path=Join-Path $assets ('blockstates/'+$id+'.json')
    (Get-Content -LiteralPath $path -Raw).Replace('changede:block/pipe_center','changede:block/'+$kind+'_pipe_center').Replace('changede:block/pipe_arm','changede:block/'+$kind+'_pipe_arm') | Set-Content -LiteralPath $path -Encoding utf8
    Write-Json ('models/item/'+$id+'.json') @{parent=('changede:block/'+$kind+'_pipe_center')}
}
& (Join-Path $PSScriptRoot 'import-legacy-plate.ps1')
Write-Output 'Imported technology texture atlases, models, pipe variants, item icons and armor UV textures.'
