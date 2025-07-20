package com.attendanceapp.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
//import java.text.SimpleDateFormat
//import java.util.Date

class QRScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var db: FirebaseFirestore

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        db = FirebaseFirestore.getInstance()
        barcodeView = findViewById(R.id.barcode_scanner)
        barcodeView.statusView.text = "Scan QR"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            startScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume() // RESUME CAMERA
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause() // PAUSE CAMERA
    }

    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    barcodeView.pause()
                    handleScanResult(it.text)
                }
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleScanResult(content: String) {
        Toast.makeText(this, "Scanned: $content", Toast.LENGTH_LONG).show()

        val parts = content.split("|")
        if (parts.size == 3) {
            val name = parts[0].trim()
            val roll = parts[1].trim()
            val id = parts[2].trim()

            val student = hashMapOf(
                "name" to name,
                "roll" to roll,
                "id" to id,
                "timestamp" to java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                    .format(java.util.Date())
            )

            val date = java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date())

            db.collection("Attendance")                 // Top-level collection
                .document(date)                         // Document = today's date (e.g. "24-05-25")
                .collection("Students")                 // Sub collection under the date
                .document(roll)                         // Document = Roll number (e.g. "CS101")
                .set(student)                           // Save student data
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Attendance marked successfully", Toast.LENGTH_SHORT)
                        .show()
                    barcodeView.resume()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Error saving to Fire-store", Toast.LENGTH_SHORT).show()
                    barcodeView.resume()
                }

        } else {
            Toast.makeText(this, "Note: QR is not in attendance format", Toast.LENGTH_SHORT).show()
            barcodeView.resume()
        }
    }
}