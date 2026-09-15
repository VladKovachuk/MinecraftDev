Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$source = [Drawing.Bitmap]::new((Join-Path $PSScriptRoot 'gui-user-sketch.png'))
$gray = [Drawing.Bitmap]::new((Join-Path $PSScriptRoot 'gui-gray-base.png'))
$out = [Drawing.Bitmap]::new(256,256)
$g = [Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($source,[Drawing.Rectangle]::new(0,0,176,94),0,0,$source.Width,629,[Drawing.GraphicsUnit]::Pixel)
$g.Dispose()
# Remove the sketch's black backdrop. Keep the pot, pipes and steam intact.
for($y=0;$y -lt 94;$y++) { for($x=0;$x -lt 176;$x++) {
    $p=$out.GetPixel($x,$y)
    if($p.R -lt 20 -and $p.G -lt 20 -and $p.B -lt 20) { $out.SetPixel($x,$y,[Drawing.Color]::Transparent) }
} }
$g=[Drawing.Graphics]::FromImage($out)
function Box($x,$y,$w,$h,$color) {
    $b=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    $g.FillRectangle($b,$x,$y,$w,$h); $b.Dispose()
}
function ClearRegion($x,$y,$w,$h) {
    $g.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy
    $g.FillRectangle([Drawing.Brushes]::Transparent,$x,$y,$w,$h)
    $g.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceOver
}
function Slot($x,$y) {
    Box ($x-1) ($y-1) 18 18 '#ffffff'
    Box ($x-1) ($y-1) 17 1 '#373737'; Box ($x-1) $y 1 16 '#373737'
    Box $x $y 16 16 '#8b8b8b'
}
# Match clickable slots to the sketch; left bottom recess becomes the water gauge.
foreach($x in @(18,139)) { foreach($y in @(10,33,56)) { ClearRegion ($x-1) ($y-1) 22 23 } }
Slot 20 12; Slot 20 36
Slot 141 12; Slot 141 36; Slot 141 60
# Three horizontal water cells in place of the third left slot.
Box 19 59 18 20 '#373737'; Box 20 60 16 18 '#8b8b8b'
for($i=0;$i -lt 3;$i++) { Box 21 (61+6*$i) 14 4 '#263c47' }
# The output rests on the water in the centre, marked by understated corner brackets.
foreach($xx in @(79,95)) { foreach($yy in @(35,51)) {
    Box $xx $yy 2 2 '#b4dce3'
} }
# Extract the three lit flames into the sprite atlas, then replace with cold silhouettes.
$g.DrawImage($out,[Drawing.Rectangle]::new(176,20,24,9),76,84,24,9,[Drawing.GraphicsUnit]::Pixel)
$g.Dispose()
for($yy=84;$yy -lt 93;$yy++) { for($xx=76;$xx -lt 100;$xx++) {
    $p=$out.GetPixel($xx,$yy)
    if($p.A -gt 0) { $v=[int](($p.R+$p.G+$p.B)/12)+35; $out.SetPixel($xx,$yy,[Drawing.Color]::FromArgb($p.A,$v,$v,$v)) }
} }
$g=[Drawing.Graphics]::FromImage($out)
# Pixel-exact progress track and fill sprite.
Box 65 78 46 6 '#373737'; Box 66 79 44 4 '#a4a4a4'; Box 67 80 42 2 '#252525'
Box 176 0 42 2 '#eac07b'
Box 176 6 14 4 '#357fa9'; Box 176 6 14 1 '#86c9de'
$g.DrawImage($gray,[Drawing.Rectangle]::new(0,104,176,96),0,118,176,96,[Drawing.GraphicsUnit]::Pixel)
Box 3 104 170 1 '#ffffff'
$g.Dispose();$source.Dispose();$gray.Dispose()
$out.Save((Join-Path $root 'src/main/resources/assets/smokemod/textures/gui/container/cookpot.png'),[Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()
