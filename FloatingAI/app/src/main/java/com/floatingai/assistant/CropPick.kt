package com.floatingai.assistant
import android.graphics.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.*
import kotlin.math.*

/** Menampilkan hasil capture layar penuh; pengguna menyeret untuk memilih area yang akan dipindai. */
class CropActivity : ComponentActivity() {
    private fun finish(b: Bitmap?) { if (b != null) Bus.result.tryEmit(b) else Bus.cancel.tryEmit(Unit); Bus.pending = null; finish() }
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val bmp = Bus.pending ?: run { finish(null); return }
        setContent { MaterialTheme(colorScheme = darkColorScheme()) {
            var a by remember { mutableStateOf<Offset?>(null) }; var b by remember { mutableStateOf<Offset?>(null) }; var sz by remember { mutableStateOf(IntSize.Zero) }
            Box(Modifier.fillMaxSize().onSizeChanged { sz = it }) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectDragGestures(onDragStart = { a = it; b = it }) { c, _ -> b = c.position } }) {
                    val p = a; val q = b
                    if (p != null && q != null) {
                        val tl = Offset(min(p.x, q.x), min(p.y, q.y)); val z = Size(abs(p.x - q.x), abs(p.y - q.y))
                        drawRect(Color(0x334DE2C5), tl, z); drawRect(Color(0xFF4DE2C5), tl, z, style = Stroke(4f))
                    }
                }
                Text("Seret untuk memilih area soal", Modifier.align(Alignment.TopCenter).padding(top = 48.dp), color = Color.White)
                Row(Modifier.align(Alignment.BottomCenter).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ finish(null) }) { Text("Batal") }
                    OutlinedButton({ finish(bmp) }) { Text("Seluruh layar") }
                    Button({
                        val p = a; val q = b
                        if (p == null || q == null || sz.width == 0) return@Button
                        val sx = bmp.width.toFloat() / sz.width; val sy = bmp.height.toFloat() / sz.height
                        val x = (min(p.x, q.x) * sx).toInt().coerceIn(0, bmp.width - 1); val y = (min(p.y, q.y) * sy).toInt().coerceIn(0, bmp.height - 1)
                        val w = (abs(p.x - q.x) * sx).toInt().coerceAtMost(bmp.width - x); val h = (abs(p.y - q.y) * sy).toInt().coerceAtMost(bmp.height - y)
                        if (w > 20 && h > 20) finish(Bitmap.createBitmap(bmp, x, y, w, h))
                    }) { Text("Pindai area") }
                }
            }
        } }
    }
}

/** Memilih gambar (foto soal, tangkapan layar, dokumen yang sudah berupa gambar) dari galeri/file. */
class PickActivity : ComponentActivity() {
    private val l = registerForActivityResult(ActivityResultContracts.GetContent()) { u ->
        var out: Bitmap? = null
        if (u != null) {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
            o.inJustDecodeBounds = false; o.inSampleSize = maxOf(1, maxOf(o.outWidth, o.outHeight) / 1600)
            out = contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
        }
        if (out != null) Bus.result.tryEmit(out) else Bus.cancel.tryEmit(Unit)
        finish()
    }
    override fun onCreate(s: Bundle?) { super.onCreate(s); l.launch("image/*") }
}
