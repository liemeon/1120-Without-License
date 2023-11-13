package com.deepid.lgc.util

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.deepid.lgc.R
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

object Utils {

    fun getLicense(context: Context?): ByteArray? {
        if (context == null) return null
        val licInput = context.resources.openRawResource(R.raw.regula)
        val available: Int = try {
            licInput.available()
        } catch (e: IOException) {
            return null
        }
        val license = ByteArray(available)
        try {
            licInput.read(license)
        } catch (e: IOException) {
            return null
        }
        return license
    }

}
fun getImageFile(context: Context?, bitmap: Bitmap): File {

    val wrapper = ContextWrapper(context)
    var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
    file = File(file,"${UUID.randomUUID()}.jpg")
    val stream: OutputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG,25,stream)
    stream.flush()
    stream.close()
    return file

}
fun File.mimeType(): String? =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)

fun ContentResolver.readAsRequestBody(uri: Uri): RequestBody =
    object: RequestBody() {
        override fun contentType(): MediaType? =
            this@readAsRequestBody.getType(uri)?.toMediaTypeOrNull()

        override fun writeTo(sink: BufferedSink) {
            this@readAsRequestBody.openInputStream(uri)?.source()?.use(sink::writeAll)
        }

        override fun contentLength(): Long =
            this@readAsRequestBody.query(uri, null, null, null, null)?.use { cursor ->
                val sizeColumnIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeColumnIndex)
            } ?: super.contentLength()
    }
