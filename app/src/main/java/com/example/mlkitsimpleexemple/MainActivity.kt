package com.example.mlkitsimpleexemple

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitsimpleexemple.databinding.ActivityMainBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var labels: List<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val localModel = LocalModel.Builder()
            .setAssetFilePath("fruits_model.tflite")
            // or .setAbsoluteFilePath(absolute file path to model file)
            // or .setUri(URI to model file)
            .build()

        // Live detection and tracking
        val customObjectDetectorOptions =
            CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()


        val objectDetector =
            ObjectDetection.getClient(customObjectDetectorOptions)

        // Carregar as labels do arquivo de texto
        try {
            labels = loadLabelsFromAsset("labels.txt")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val imageBitmap = BitmapFactory.decodeStream(assets.open("apple.jpg"))
        val image = InputImage.fromBitmap(imageBitmap, 0)

        objectDetector.process(image)
            .addOnSuccessListener { results ->
                Log.d("IMAGEM", "onSuccess")
                drawBoundingBox(imageBitmap, results)
                for (detectedObjects in results) {
                    val boundingBox = detectedObjects.boundingBox
                    Log.d("IMAGEM", "boundingBox: $boundingBox")
                }
            }
            .addOnFailureListener { e ->
                Log.e("IMAGEM", "Error processing image", e)
            }
            .addOnCompleteListener {
                Log.d("IMAGEM", "onComplete")
                objectDetector.close()
            }


    }

    private fun drawBoundingBox(imageBitmap: Bitmap, detectedObjects: MutableList<DetectedObject>) {
        val paint = Paint()
        paint.color = Color.BLUE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f

        val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        for (detectedObject in detectedObjects) {
            val boundingBox = detectedObject.boundingBox
            val left = boundingBox.left
            val top = boundingBox.top
            val right = boundingBox.right
            val bottom = boundingBox.bottom

            val labelIndex = detectedObject.labels[0].index

            // Adiciona a label ao retângulo desenhado
            val labelText = detectedObject.labels[0].text
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            canvas.drawText(labelText, left.toFloat(), top.toFloat() - 10, paint)
        }

        runOnUiThread {
            binding.imageView.setImageBitmap(mutableBitmap)
        }
    }


    @Throws(IOException::class)
    private fun loadLabelsFromAsset(fileName: String): List<String> {
        val labels = mutableListOf<String>()
        val inputStream = assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        try {
            while (reader.readLine().also { line = it } != null) {
                labels.add(line.orEmpty())
            }
        } finally {
            reader.close()
        }

        return labels
    }

}