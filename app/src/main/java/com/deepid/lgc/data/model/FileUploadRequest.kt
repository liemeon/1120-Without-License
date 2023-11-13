package com.deepid.lgc.data.model

import com.google.gson.annotations.SerializedName

data class FileUploadRequest(
    @SerializedName("type")
    val type: String = "image",

    @SerializedName("mimeType")
    val mimeType: String = "image/jpeg",

    @SerializedName("imageUploadTarget")
    val imageUploadTarget: String = "profile",

    @SerializedName("num")
    val num: String = "1",

    val fileLength: Long

//    val file: RequestBody
)

//fun FileUploadRequest.toRequestBody(contentResolver: ContentResolver): RequestBody {
//    return FileRequestBody(contentResolver, Uri.fromFile(file)) { bytesWritten, contentLength ->
//        val progress = 100 * bytesWritten / contentLength
//    }
//}

//class FileRequestBody(
//    private val contentResolver: ContentResolver,
//    private val fileUri: Uri,
//    private val onProgressUpdate: (writtenBytes: Long, contentLength: Long) -> Unit
//) : RequestBody() {
//
//    override fun contentType(): MediaType? =
//        contentResolver.getType(fileUri)?.toMediaTypeOrNull()
//
//    override fun writeTo(sink: BufferedSink) {
//        val countingSink = CountingSink(sink, this, onProgressUpdate)
//        val bufferedSink = countingSink.buffer()
//        contentResolver.openInputStream(fileUri)?.source()?.use(bufferedSink::writeAll)
//        bufferedSink.flush()
//    }
//
//    override fun contentLength(): Long =
//        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
//            val sizeColumnIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
//            cursor.moveToFirst()
//            cursor.getLong(sizeColumnIndex)
//        } ?: super.contentLength()
//}
//
//class CountingSink(
//    sink: Sink,
//    private val requestBody: RequestBody,
//    private val onProgressUpdate: (bytesWritten: Long, contentLength: Long) -> Unit
//) : ForwardingSink(sink) {
//    private var bytesWritten = 0L
//
//    override fun write(source: Buffer, byteCount: Long) {
//        super.write(source, byteCount)
//
//        bytesWritten += byteCount
//        onProgressUpdate(bytesWritten, requestBody.contentLength())
//    }
//}
