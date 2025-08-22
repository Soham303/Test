package com.example.trialpaymentapp // Make sure this matches your app's package name

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

object QrUtils {
    fun generateQrCodeBitmap(text: String, width: Int = 200, height: Int = 200): Bitmap? { // Changed default size to 200 to match UI
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                null // You can pass encoding hints here if needed
            )
            val bmp = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: WriterException) {
            // Log the error or handle it more gracefully
            // For now, just printing the stack trace and returning null
            e.printStackTrace()
            null
        }
    }
}
