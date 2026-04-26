package io.github.wjy.meditate.network

import android.graphics.Bitmap
import qrcode.QRCode

object QrCodeUtils {
    /**
     * 高性能且轻量的二维码生成：使用 qrcode-kotlin
     */
    fun generateQrCode(content: String, size: Int): Bitmap? {
        return try {
            // qrcode-kotlin: withSize 设置的是每个模块的像素大小
            // 我们估算模块数为 33，计算对应的像素大小
            val cellSize = (size / 33).coerceAtLeast(1)
            
            QRCode.ofRoundedSquares()
                .withSize(cellSize)
                .build(content)
                .render()
                .nativeImage() as Bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
