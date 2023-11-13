package com.deepid.lgc.data.repository

import android.util.Log
import com.deepid.lgc.data.common.BaseResult
import com.deepid.lgc.data.common.Failure
import com.deepid.lgc.data.model.FileUploadRequest
import com.deepid.lgc.data.model.FileUploadResponse
import com.deepid.lgc.data.network.MainNetwork
import com.deepid.lgc.data.serializeToMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.RequestBody

class MainRepository(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainNetwork: MainNetwork
) : IMainRepository {
    override suspend fun getUploadUrls(
        fileUploadRequest: FileUploadRequest,
        fileRequestBody: RequestBody
    ): Flow<BaseResult<FileUploadResponse, Failure>> =
        flow {
            emit(
                try {
                    val response =
                        mainNetwork.getUploadUrls(fileUploadRequest.serializeToMap() as Map<String, String>)
                    if (response.isSuccessful) {
                        Log.d(TAG, "getUploadUrls: ${response.body()?.first()?.path}")
                        response.body()?.first()?.url?.let {
                            Log.d(TAG, "getUploadUrls:${response.body()?.first()?.path?.substring(22)} ")
                            mainNetwork.uploadFile(
                                uploadUrl = it,
                                headers = mapOf(
                                    "Content-Type" to fileUploadRequest.mimeType,
                                    "Content-Length" to fileUploadRequest.fileLength.toString()
                                ),
//                                contentType = fileUploadRequest.mimeType,
//                                contentLength = fileUploadRequest.fileLength,
                                file = fileRequestBody
                            )
                            it
                        }
                        BaseResult.Success(FileUploadResponse(response.body()))
                    } else {
                        Log.e(TAG, "getUploadUrls: ${response.code()},${response.body()},${response}")
                        BaseResult.Error(Failure(response.code(), response.message()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getUploadUrls: $e")
                    BaseResult.Error(Failure(-1, e.message.toString()))
                }
            )
        }.flowOn(ioDispatcher)

    companion object {
        private val TAG = this.javaClass.name;
    }

}

interface IMainRepository {
    suspend fun getUploadUrls(
        fileUploadRequest: FileUploadRequest,
        fileRequestBody: RequestBody
    ): Flow<BaseResult<FileUploadResponse, Failure>>
}
