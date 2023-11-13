package com.deepid.lgc.ui.defaultscanner

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.deepid.lgc.databinding.ActivityDefaultScannerBinding
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.configuration.FaceCaptureConfiguration
import com.regula.facesdk.enums.ImageType
import com.regula.facesdk.enums.LivenessStatus
import com.regula.facesdk.model.MatchFacesImage
import com.regula.facesdk.model.results.FaceCaptureResponse
import com.regula.facesdk.model.results.LivenessResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesResponse
import com.regula.facesdk.model.results.matchfaces.MatchFacesSimilarityThresholdSplit
import com.regula.facesdk.request.MatchFacesRequest

class DefaultScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDefaultScannerBinding
    private var currentScenario = Scenario.SCENARIO_FULL_AUTH
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDefaultScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun showScanner() {
        Log.d("MainActivity", "DEBUGX showScanner: currentscenario $currentScenario")
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@DefaultScannerActivity, scannerConfig, completion)
    }

    private fun displayResults(results: DocumentReaderResults) {
        val documentImage: Bitmap? =
            results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
        if (documentImage != null) {
            Log.d("DefaultScanner", "DEBUGX documentImage is not null")
            binding.documentIv.setImageBitmap(documentImage)
            return
        }
        results.graphicResult?.fields?.first()?.let {
            val name = it.getFieldName(this) + " [${it.pageIndex}]"
            binding.titleTv.text = name
            val image = it.bitmap
            binding.documentIv.setImageBitmap(image)
            Log.d("DefaultScanner", "DEBUGX initResults: name = ${name} ")
        }
    }

    private fun displayResults(documentImage: Bitmap, liveImage: Bitmap) {
        with(binding) {
            documentIv.setImageBitmap(documentImage)
            liveIv.setImageBitmap(liveImage)
        }
    }

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
//            hideDialog()
//            cancelAnimation()
            if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                Log.d("MainActivity", "DEBUGX MULTIPAGEMODE: ")
                if (results?.morePagesAvailable != 0) {
                    DocumentReader.Instance().startNewPage()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showScanner()
                    }, 100)
                    return@IDocumentReaderCompletion
                } else {
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()
                }
            }
            if (results?.chipPage != 0) {
                Log.d("MainActivity", "DEBUGX RFID IS PERFORMED: ")
                DocumentReader.Instance().startRFIDReader(this, object : IRfidReaderCompletion() {
                    override fun onChipDetected() {
                        Log.d("Rfid", "Chip detected")
                    }

                    override fun onProgress(notification: DocumentReaderNotification) {
//                        rfidProgress(notification.code, notification.value)
                    }

                    override fun onRetryReadChip(exception: DocReaderRfidException) {
                        Log.d("Rfid", "Retry with error: " + exception.errorCode)
                    }

                    override fun onCompleted(
                        rfidAction: Int,
                        results_RFIDReader: DocumentReaderResults?,
                        error: DocumentReaderException?
                    ) {
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL)
                            captureFace(results_RFIDReader!!)
                    }
                })
            } else {
                Log.d("MainActivity", "DEBUGX NO RFID PERFORMED ")
                /**
                * perform [livenessFace] or [captureface] then check similarity
                */
              //  livenessFace(results)
                captureFace(results)
            }
        } else
            if (action == DocReaderAction.CANCEL) {
                if (DocumentReader.Instance().functionality().isManualMultipageMode)
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
//                hideDialog()
//                cancelAnimation()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
//                hideDialog()
//                cancelAnimation()
            }
    }

    private fun livenessFace(results: DocumentReaderResults) {
        FaceSDK.Instance().startLiveness(this) { livenessResponse: LivenessResponse? ->
            livenessResponse?.liveness?.let {
                if (it == LivenessStatus.PASSED) {
                    Toast.makeText(
                        this,
                        "Liveness check is Passed",
                        Toast.LENGTH_LONG
                    ).show()
                    val documentImage: Bitmap? =
                        results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
                    val liveImage: Bitmap? = livenessResponse.bitmap
                    if (documentImage != null && liveImage != null) {
                        matchFaces(documentImage, liveImage)
                        displayResults(documentImage, liveImage)
                    } else {
                        displayResults(results)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Liveness check is Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun captureFace(results: DocumentReaderResults) {
        val faceCaptureConfiguration: FaceCaptureConfiguration =
            FaceCaptureConfiguration.Builder()
                .setCloseButtonEnabled(true)
                .setTorchButtonEnabled(true)
                .build()
        FaceSDK.Instance()
            .presentFaceCaptureActivity(
                this@DefaultScannerActivity,
                faceCaptureConfiguration
            ) { response: FaceCaptureResponse ->
                // ... check response.image for capture result
                response.image?.bitmap?.let { bitmap ->
                    val documentImage: Bitmap? =
                        results.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
                            ?: results.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
                    val liveImage: Bitmap = bitmap
                    documentImage?.let {
                        matchFaces(documentImage, liveImage)
                        displayResults(documentImage, liveImage)
                    }
                } ?: run {
                    response.exception?.message?.let {
                        Toast.makeText(
                            this@DefaultScannerActivity,
                            "Error: $it",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    displayResults(results)
                }
            }
    }

    private fun matchFaces(first: Bitmap, second: Bitmap) {
        val firstImage = MatchFacesImage(first, ImageType.DOCUMENT_WITH_LIVE, true)
        val secondImage = MatchFacesImage(second, ImageType.LIVE, true)
        val matchFacesRequest = MatchFacesRequest(arrayListOf(firstImage, secondImage));
        FaceSDK.Instance().matchFaces(matchFacesRequest) { matchFacesResponse: MatchFacesResponse ->
            val split = MatchFacesSimilarityThresholdSplit(matchFacesResponse.results, 0.75)
            with(binding) {
                if (split.matchedFaces.size > 0) {
                    val similarity = split.matchedFaces[0].similarity
                    similarityTv.text =
                        "Similarity: " + String.format("%.2f", similarity * 100) + "%"
                    if (similarity > 0.8) {
                        statusTv.text = "(Valid)"
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                this@DefaultScannerActivity,
                                com.regula.common.R.color.dark_green
                            )
                        )
                        btnUpload.isEnabled = true
                    } else {
                        statusTv.text = "(Not Valid)"
                        statusTv.setTextColor(
                            ContextCompat.getColor(
                                this@DefaultScannerActivity,
                                com.regula.common.R.color.red
                            )
                        )
                    }
                } else {
                    similarityTv.text = "Similarity: 0%"
                    statusTv.text = "(Not Valid)"
                    statusTv.setTextColor(
                        ContextCompat.getColor(
                            this@DefaultScannerActivity,
                            com.regula.common.R.color.red
                        )
                    )
                }
//                btnScan.isEnabled = true
            }
        }
    }

    private fun resetViews() {
        with(binding) {
            similarityTv.text = "Similarity: -"
            statusTv.text = ""
            documentIv.setImageBitmap(null)
            liveIv.setImageBitmap(null)
        }
    }

    private fun initViews() {
        with(binding) {
            btnScan.setOnClickListener {
                resetViews()
                btnUpload.isEnabled = false
                showScanner()
            }
            btnUpload.setOnClickListener {
                // TODO: PERFORM UPLOAD IMAGE/FILE HERE
                Toast.makeText(
                    this@DefaultScannerActivity,
                    "This feature will be available soon",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
