package com.deepid.lgc.ui.scanner

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deepid.lgc.R
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderResults

class SuccessfulInitActivity : AppCompatActivity() {
    private var uvImage: ImageView? = null
    private var rfidImage: ImageView? = null
    private var showScannerBtn: Button? = null
//    private lateinit var binding: SucessfullInitActivityBinding

    // TODO: add view model and upload image when scan is successfull
//    private val scannerViewModel: ScannerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = SucessfullInitActivityBinding.inflate(layoutInflater)
//        setContentView(binding.root)
        setContentView(R.layout.sucessfull_init_activity)
        initViews()
        setUpFunctionality()
        //observe()

        if (!DocumentReader.Instance().isReady)
            showScannerBtn!!.isEnabled = false
        showScannerBtn!!.setOnClickListener {
            val scannerConfig = ScannerConfig.Builder(Scenario.SCENARIO_OCR).build()
            setUpFunctionality()
            DocumentReader.Instance().showScanner(
                this, scannerConfig
            ) { action, results, error ->
                if (action == DocReaderAction.COMPLETE) {
                    showUvImage(results)
                    //Checking, if nfc chip reading should be performed
                    if (results!!.chipPage != 0) {
                        //starting chip reading
                        DocumentReader.Instance().startRFIDReader(
                            this@SuccessfulInitActivity,
                            object : IRfidReaderCompletion() {
                                override fun onCompleted(
                                    rfidAction: Int,
                                    results: DocumentReaderResults?,
                                    error: DocumentReaderException?
                                ) {
                                    if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                        showGraphicFieldImage(results)
                                    }
                                }

                            })
                    }
                    Log.d(
                        this@SuccessfulInitActivity.localClassName,
                        "[DEBUGX] completion raw result: " + results.rawResult
                    )
                } else {
                    //something happened before all results were ready
                    if (action == DocReaderAction.CANCEL) {
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Scanning was cancelled",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else if (action == DocReaderAction.ERROR) {
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Error:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    } else{
                        Toast.makeText(
                            this@SuccessfulInitActivity,
                            "Something wrong:$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun setUpFunctionality() {
        DocumentReader.Instance().processParams().timeout = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        DocumentReader.Instance().functionality().edit()
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setDisplayMetadata(true)
            .apply()
    }

//    private fun observe() {
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                scannerViewModel.state.collect { uiState ->
//                    handleStateChange(uiState)
//                }
//            }
//        }
//    }
//
//    private fun handleStateChange(uiState: ScannerUiState) {
//        when (uiState) {
//            is ScannerUiState.Init -> Unit
//            is ScannerUiState.Loading -> Unit
//            is ScannerUiState.Error -> Unit
//            is ScannerUiState.Success -> Unit
//        }
//    }

    override fun onPause() {
        super.onPause()
        resetViews()
    }

    private fun resetViews() {
        uvImage?.invalidate()
        rfidImage?.invalidate()
    }

    private fun showUvImage(documentReaderResults: DocumentReaderResults?) {
        val uvDocumentReaderGraphicField = documentReaderResults?.getGraphicFieldByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
        )
        if (uvDocumentReaderGraphicField != null && uvDocumentReaderGraphicField.bitmap != null)
            uvImage?.setImageBitmap(resizeBitmap(uvDocumentReaderGraphicField.bitmap))
    }

    private fun showGraphicFieldImage(results: DocumentReaderResults?) {
        val documentImage: Bitmap? =
            if (results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT) == null) {
                results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
            } else {
                results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            }
        if (documentImage != null)
            rfidImage?.setImageBitmap(resizeBitmap(documentImage))
    }

    private fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    private fun initViews() {
        showScannerBtn = findViewById(R.id.showScannerBtn)
        uvImage = findViewById(R.id.uvImageView)
        rfidImage = findViewById(R.id.documentImageIv)
//        binding.uploadBtn.setOnClickListener {
//            uvImage?.drawable?.toBitmap()?.let { bitmap ->
//                val file: File = getImageFile(this, bitmap)
//                val fileUploadRequest =
//                    FileUploadRequest(
//                        mimeType = file.mimeType().toString(),
//                        fileLength = file.length()
//                    )
//                val fileRequestBody = contentResolver.readAsRequestBody(Uri.fromFile(file))
//                scannerViewModel.uploadFile(fileUploadRequest, fileRequestBody)
//            }
//        }
    }

    companion object {
        const val TAG = "SuccessfullInitActivity"
    }
}
