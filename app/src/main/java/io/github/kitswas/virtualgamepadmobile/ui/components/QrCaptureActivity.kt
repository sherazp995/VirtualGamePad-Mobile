package io.github.kitswas.virtualgamepadmobile.ui.components

import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import io.github.kitswas.virtualgamepadmobile.R

/**
 * The QR scanner screen. Same as the library's, but it only reads a code inside a
 * centred square (see res/layout/qr_capture.xml) and dims the rest of the view.
 */
class QrCaptureActivity : CaptureActivity() {
    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.qr_capture)
        return findViewById(R.id.zxing_barcode_scanner)
    }
}
