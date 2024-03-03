package com.example.mlkitsimpleexemple

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mlkitsimpleexemple.databinding.ActivityMainBinding
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var bindig: ActivityMainBinding

    private lateinit var interpreter: Interpreter

    // Dimensões da entrada do modelo
    private val inputImageWidth = 150
    private val inputImageHeight = 150
    private val inputImageChannels = 3
    private val numClasses = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindig = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindig.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicialize o Interpreter do TensorFlow Lite
        val modelFileDescriptor = assets.openFd("fruits_model.tflite")
        val inputStream = FileInputStream(modelFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelFileDescriptor.startOffset
        val declaredLength = modelFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)

        // Chame a função para carregar uma imagem do diretório "assets"
        pickImageFromAssets("kiwitwo.jpg")
    }

    private fun pickImageFromAssets(imageFileName: String) {
        try {
            // Carregue a imagem do diretório "assets"
            val inputStream = assets.open(imageFileName)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Redimensione a imagem para o formato de entrada do modelo
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)

            // Mostre a imagem redimensionada no ImageView
            //bindig.imageView.setImageBitmap(resizedBitmap)

            // Faça a inferência com o modelo TensorFlow Lite
            val result = performInference(resizedBitmap)
            // Desenhe as caixas delimitadoras na imagem
            val outputBitmap = drawBoundingBoxes(resizedBitmap, result)

            // Mostre a imagem com caixas delimitadoras no ImageView
            bindig.imageView.setImageBitmap(outputBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun performInference(bitmap: Bitmap): FloatArray {
        // Pré-processamento da imagem
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * inputImageChannels)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageHeight)

        for (pixelValue in pixels) {
            inputBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }

        // Faça a inferência
        val outputBuffer = ByteBuffer.allocateDirect(4 * numClasses)
        outputBuffer.order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        // Processamento do resultado
        val result = FloatArray(numClasses)
        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(result)

        // Exemplo: imprimir coordenadas da caixa delimitadora
        val xmin = result[0]
        val ymin = result[1]
        val xmax = result[2]
        val ymax = result[3]

        Log.d("Object Detection", "Coordenadas da Caixa Delimitadora: ($xmin, $ymin, $xmax, $ymax)")

        // Encontrar a classe com a maior probabilidade
        val maxProbIndex = result.indices.maxByOrNull { result[it] } ?: -1

        // Carregar os rótulos
        val labels = loadLabels("labels.txt") // Substitua "labels.txt" pelo seu arquivo de rótulos

        // Identificar o objeto detectado
        val detectedObject = labels.getOrNull(maxProbIndex) ?: "Objeto Desconhecido"

        Log.d("Object Detection", "Objeto Detectado: $detectedObject")


        return result
    }

    private fun loadLabels(fileName: String): List<String> {
        try {
            val labelsInputStream = assets.open(fileName)
            val labels = mutableListOf<String>()
            labelsInputStream.bufferedReader().useLines { lines ->
                lines.forEach { labels.add(it) }
            }
            labelsInputStream.close()
            return labels
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return emptyList()
    }

    // funcao ainda nao finalizada
    private fun drawBoundingBoxes(bitmap: Bitmap, result: FloatArray): Bitmap {
        val outputBitmap = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(outputBitmap)
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 2.0f

        // Ajuste para as coordenadas da caixa delimitadora
        val xmin = (result[0] * inputImageWidth).coerceAtLeast(0.0f).coerceAtMost(inputImageWidth.toFloat())
        val ymin = (result[1] * inputImageHeight).coerceAtLeast(0.0f).coerceAtMost(inputImageHeight.toFloat())
        val xmax = (result[2] * inputImageWidth).coerceAtLeast(0.0f).coerceAtMost(inputImageWidth.toFloat())
        val ymax = (result[3] * inputImageHeight).coerceAtLeast(0.0f).coerceAtMost(inputImageHeight.toFloat())


        // Desenhe a caixa delimitadora
        canvas.drawRect(RectF(xmin, ymin, xmax, ymax), paint)

        return outputBitmap
    }
}