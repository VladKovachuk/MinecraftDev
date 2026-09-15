Add-Type -AssemblyName System.Drawing
$source = [System.Drawing.Bitmap]::new('C:\Users\MrUser\.codex\generated_images\01a08738-9f42-73d0-827b-e4f383e79724\exec-2062f5e9-75dc-4ac8-b4bc-240174fff050.png')
$target = [System.Drawing.Bitmap]::new(176, 230)
$graphics = [System.Drawing.Graphics]::FromImage($target)
try {
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, 176, 230), 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)
    $target.Save('C:\Users\MrUser\Desktop\MinecraftDev\smokemod_main\src\main\resources\assets\smokemod\textures\gui\container\jar.png', [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $target.Dispose()
    $source.Dispose()
}
