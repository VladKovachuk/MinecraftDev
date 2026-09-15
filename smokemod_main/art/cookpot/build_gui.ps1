Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$source = [Drawing.Bitmap]::new((Join-Path $PSScriptRoot 'gui-copper-source.png'))
$old = [Drawing.Bitmap]::new((Join-Path $PSScriptRoot 'gui-gray-base.png'))
$out = [Drawing.Bitmap]::new(256,256)
$g = [Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
function Box($x,$y,$w,$h,$color) {
    $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($color))
    $g.FillRectangle($brush,$x,$y,$w,$h)
    $brush.Dispose()
}
# Import generated copper artwork at the logical Minecraft pixel resolution.
$g.DrawImage($source,[Drawing.Rectangle]::new(0,0,176,117),0,54,1042,683,[Drawing.GraphicsUnit]::Pixel)
# Reconstruct the functional face from a clean copper patch of the artwork.
# This removes approximate generated slots before placing pixel-exact recesses.
$patch = $source.Clone([Drawing.Rectangle]::new(450,390,64,64),[Drawing.Imaging.PixelFormat]::Format32bppArgb)
for($yy=34;$yy -lt 91;$yy+=8) {
    for($xx=21;$xx -lt 159;$xx+=8) {
        $g.DrawImage($patch,[Drawing.Rectangle]::new($xx,$yy,[Math]::Min(8,159-$xx),[Math]::Min(8,91-$yy)),0,0,64,64,[Drawing.GraphicsUnit]::Pixel)
    }
}
function Slot($x,$y,$w=16,$h=16) {
    Box ($x-1) ($y-1) ($w+2) ($h+2) '#db9860'
    Box ($x-1) ($y-1) ($w+1) 1 '#35241e'
    Box ($x-1) $y 1 $h '#35241e'
    Box $x $y $w $h '#544038'
}
Slot 18 35; Slot 18 71; Slot 66 35; Slot 90 35; Slot 114 35; Slot 148 71
Slot 44 35 8 52
Box 44 52 8 1 '#a97650'; Box 44 69 8 1 '#a97650'
Box 25 55 2 8 '#eed4a8'; Box 23 61 6 2 '#eed4a8'; Box 24 63 4 1 '#eed4a8'; Box 25 64 2 1 '#eed4a8'
function Arrow($x,$y,$color) {
    Box $x ($y+4) 66 6 $color
    for($i=0;$i -lt 7;$i++) { Box ($x+66+$i) ($y+$i) 1 (14-2*$i) $color }
}
Arrow 66 71 '#68402a'
# Preserve original vanilla inventory without scaling or regenerating its slots.
$g.DrawImage($old,[Drawing.Rectangle]::new(0,118,176,96),0,118,176,96,[Drawing.GraphicsUnit]::Pixel)
Box 3 118 170 1 '#ffffff'; Box 4 119 168 1 '#c6c6c6'
Box 176 0 6 15 '#3477b4'; Box 176 0 6 2 '#91d8ee'; Box 176 13 6 2 '#285b91'
Arrow 176 20 '#f4dfb2'
$g.Dispose(); $patch.Dispose(); $source.Dispose(); $old.Dispose()
$out.Save((Join-Path $root 'src/main/resources/assets/smokemod/textures/gui/container/cookpot.png'),[Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()
