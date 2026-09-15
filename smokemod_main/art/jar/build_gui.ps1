Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$gray=[Drawing.Bitmap]::new((Join-Path $root 'art/cookpot/gui-gray-base.png'))
$out=[Drawing.Bitmap]::new(256,256)
$g=[Drawing.Graphics]::FromImage($out)
function Box($x,$y,$w,$h,$hex) {
    $b=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($hex))
    $g.FillRectangle($b,$x,$y,$w,$h);$b.Dispose()
}
# Opaque, muted glass interior with restrained pixel shading behind the item.
# Fill follows the jar shoulders and base; the surrounding UI stays transparent.
Box 69 44 39 5 '#293a32'
Box 67 49 42 3 '#304239'
Box 66 52 44 35 '#354a40'
Box 68 87 40 3 '#304239'
Box 70 90 36 2 '#293a32'
Box 69 53 3 32 '#3e5549'
Box 72 54 32 31 '#384d42'
Box 104 53 4 33 '#2e4137'
Box 73 85 30 2 '#32453b'
# Preserve the original painted lid and cool glass highlights above the new interior.
$src=[Drawing.Bitmap]::new((Join-Path $PSScriptRoot 'jar-original.png'))
$g.Dispose()
for($y=35;$y -lt 94;$y++) {
    if($y -lt 44) { $left=69;$right=107 }
    elseif($y -lt 48) { $left=68;$right=108 }
    elseif($y -lt 51) { $left=65;$right=110 }
    elseif($y -lt 88) { $left=64;$right=111 }
    elseif($y -lt 91) { $left=66;$right=109 }
    else { $left=69;$right=106 }
    for($x=$left;$x -le $right;$x++) {
        if($x -ge 75 -and $x -le 101 -and $y -ge 53 -and $y -le 85) { continue }
        $pixel=$src.GetPixel($x,$y)
        # Below the lid, warm brown/olive pixels belong to the old wooden wall.
        # Retain the cool, pale glass highlights without filling the transparent gaps.
        if($y -ge 44 -and ($pixel.G -lt $pixel.R*0.98 -or $pixel.B -lt $pixel.R*0.9)) { continue }
        $out.SetPixel($x,$y,$pixel)
    }
}
$src.Dispose()
$g=[Drawing.Graphics]::FromImage($out)
# Copy panel pixels directly; slot interiors are rebuilt at handler coordinates.
$g.Dispose()
for($y=118;$y -lt 214;$y++) { for($x=0;$x -lt 176;$x++) {
    $out.SetPixel($x,$y+16,$gray.GetPixel($x,$y))
} }
$g=[Drawing.Graphics]::FromImage($out)
Box 3 134 170 1 '#ffffff'
Box 5 145 166 79 '#c6c6c6'
function Slot($x,$y) {
    Box ($x-1) ($y-1) 18 18 '#ffffff'
    Box ($x-1) ($y-1) 17 1 '#373737';Box ($x-1) $y 1 16 '#373737'
    Box $x $y 16 16 '#8b8b8b'
}
for($r=0;$r -lt 3;$r++) { for($c=0;$c -lt 9;$c++) { Slot (8+18*$c) (146+18*$r) } }
for($c=0;$c -lt 9;$c++) { Slot (8+18*$c) 204 }
# Quiet grey progress track plus its separate green fill sprite.
Box 15 122 146 11 '#373737';Box 16 123 144 9 '#c6c6c6';Box 17 124 142 7 '#252525'
Box 0 240 142 7 '#6e913c';Box 0 240 142 2 '#b6cc70';Box 0 246 142 1 '#425c26'
$g.Dispose();$gray.Dispose()
$out.Save((Join-Path $root 'src/main/resources/assets/smokemod/textures/gui/container/jar.png'),[Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()
