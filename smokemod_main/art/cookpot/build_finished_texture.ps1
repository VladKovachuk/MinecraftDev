Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$dir=Join-Path $root 'src/main/resources/assets/smokemod/textures/block'
$src=[Drawing.Bitmap]::new((Join-Path $dir 'cookpot_water.png'))
$out=[Drawing.Bitmap]::new($src.Width,$src.Height)
# Retain the water's pixel pattern, mapped to an ivory/cream palette.
for($y=0;$y -lt $src.Height;$y++) { for($x=0;$x -lt $src.Width;$x++) {
    $p=$src.GetPixel($x,$y)
    $shade=[Math]::Clamp([int](($p.R+$p.G+$p.B)/3)-70,0,45)
    $out.SetPixel($x,$y,[Drawing.Color]::FromArgb($p.A,[Math]::Min(255,221+$shade),[Math]::Min(255,210+$shade),[Math]::Min(255,183+$shade)))
} }
$out.Save((Join-Path $dir 'cookpot_finished.png'),[Drawing.Imaging.ImageFormat]::Png)
$out.Dispose();$src.Dispose()
