package com.deepid.lgc.ui.scanner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepid.lgc.data.common.BaseResult

import com.deepid.lgc.data.model.FileUploadRequest
import com.deepid.lgc.data.model.FileUploadResponse
import com.deepid.lgc.data.repository.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import okhttp3.RequestBody

class ScannerViewModel(private val mainRepository: MainRepository) : ViewModel() {
    private val _state = MutableStateFlow<ScannerUiState>(ScannerUiState.Init)
    val state: StateFlow<ScannerUiState> get() = _state.asStateFlow()

    private fun showLoading() {
        _state.value = ScannerUiState.Loading(true)
    }

    private fun hideLoading() {
        _state.value = ScannerUiState.Loading(false)
    }

    private fun setError(message: String) {
        _state.value = ScannerUiState.Error(message)
    }

    private fun setSuccess(data: FileUploadResponse) {
        Log.d(TAG, "setSuccess: \n" +
                "url: ${data.fileUploadResponse?.first()?.url}\n" +
                "path: ${data.fileUploadResponse?.first()?.path}")
        _state.value = ScannerUiState.Success(data)
    }


    fun uploadFile(fileUploadRequest: FileUploadRequest, fileRequestBody: RequestBody) {
        viewModelScope.launch {
            mainRepository.getUploadUrls(fileUploadRequest, fileRequestBody).onStart {
                showLoading()
            }.catch {
                hideLoading()
            }.collect { result ->
                hideLoading()
                when (result) {
                    is BaseResult.Error -> setError(result.err.message)
                    is BaseResult.Success -> setSuccess(result.data)
                }
            }
        }
    }
    companion object{
        val TAG: String = this.javaClass.name
    }
}

sealed interface ScannerUiState {
    object Init : ScannerUiState
    data class Loading(val isLoading: Boolean) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
    data class Success(val data: FileUploadResponse) : ScannerUiState
}
