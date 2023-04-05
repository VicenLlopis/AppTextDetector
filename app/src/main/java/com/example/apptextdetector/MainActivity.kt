package com.example.apptextdetector

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.*
import android.graphics.Canvas as CanvasG

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.hide()
        setContent {
            ContentView()
        }
    }
}

/**
 * Main cotent where is the canvas and the buttons
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ContentView() {

    var view: View

    val lastTouchx = remember {
        mutableStateOf(0f)
    }
    var isButtonClicked by remember {
        mutableStateOf(false)
    }

    val lastTouchY = remember {
        mutableStateOf(0f)
    }

    val path = remember {
        mutableStateOf<Path?>(Path())
    }
    var shouldLoadBitmap by remember { mutableStateOf(false) }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            view = LocalView.current



            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {

                            MotionEvent.ACTION_DOWN -> {
                                path.value?.moveTo(motionEvent.x, motionEvent.y)
                                lastTouchx.value = motionEvent.x
                                lastTouchY.value = motionEvent.y
                            }
                            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {

                                val historySize = motionEvent.historySize

                                for (i in 0 until historySize) {

                                    val historicalX = motionEvent.getHistoricalX(i)
                                    val historicalY = motionEvent.getHistoricalY(i)
                                    path.value?.lineTo(historicalX, historicalY)

                                }
                                path.value?.lineTo(motionEvent.x, motionEvent.y)

                                lastTouchx.value = motionEvent.x
                                lastTouchY.value = motionEvent.y
                            }
                        }
                        lastTouchx.value = motionEvent.x
                        lastTouchY.value = motionEvent.y

                        val temPath = path.value
                        path.value = null
                        path.value = temPath

                        true
                    },

                onDraw = {
                    path.value?.let {
                        drawPath(
                            path = it,
                            color = Color.Black,
                            style = Stroke(
                                width = 4.dp.toPx()
                            )
                        )
                    }
                }
            )
            if (shouldLoadBitmap) {
                CreateBitMap(view = view)
            }
            Button(
                onClick = {
                    shouldLoadBitmap = true
                },
                shape = CutCornerShape(10),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text(text = "Leer")
            }
            Button(
                onClick = {
                    path.value?.reset()
                },
                shape = CutCornerShape(10),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(text = "Limpiar")
            }
        }
    }
}

/**
 * Read the bitmap and pass it to text
 */

/*@Composable
fun BitmapTextGoogle(bitmap: Bitmap){
    val context = LocalContext.current
    val textRecognizer = TextRecognizer.Builder(context).build()
    val frame = Frame.Builder().setBitmap(bitmap).build()

    val textBlocks = textRecognizer.detect(frame)
    val stringBuilder = StringBuilder()
    for (i in 0 until textBlocks.size()) {
        val textBlock = textBlocks.valueAt(i)
        stringBuilder.append(textBlock.value)
    }
    val extractedText = stringBuilder.toString()

    Log.d("Prueba1",extractedText)

    Box(modifier = Modifier.fillMaxSize()){
        Text(text = extractedText, fontSize = 45.sp)
    }
}*/

/**
 * Read the bitmap and pass it to text
 */

@Composable
fun CreateTextTess(bitmap: Bitmap) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        val textState = remember { mutableStateOf("") }
        val loadingState = remember { mutableStateOf(true) }

        LaunchedEffect(bitmap) {
            val text = withContext(Dispatchers.IO) {
                val tess = TessBaseAPI()
                try {
                val dataPath = context.getExternalFilesDir(null).toString()

                val folderTessDataName: String = "tessdata"

                val folder = File(dataPath, folderTessDataName)

                if (!folder.exists()) {
                    folder.mkdir()
                    Log.d("Prueba File folder", "hola")
                }

                val file = File("$dataPath/$folderTessDataName/spa.traineddata")
                if (!file.exists()) {
                    val inputStream: InputStream = context.resources.openRawResource(R.raw.spa)
                    file.appendBytes(inputStream.readBytes())
                    file.createNewFile()
                    Log.d("Prueba File exist", "hola")
                }

                tess.init(dataPath, "spa")
                tess.setImage(bitmap)
                val text = tess.utF8Text
                text
                }finally {
                    tess.recycle()

                }
            }
            textState.value = text
            loadingState.value = false
        }

        if (loadingState.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Text(text = textState.value, fontSize = 45.sp)
        }
    }
}

/**
 * Create a bitmap from the paint canvas
 **/
@Composable
fun CreateBitMap(view: View): Bitmap {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(view) {
        bitmap = loadBitmapAsync(view).await()
    }

    bitmap?.let {
        CreateTextTess(it)
       // BitmapTextGoogle(bitmap = it)
        return it
    }

    return Bitmap.createBitmap(
        view.width,
        view.height,
        Bitmap.Config.ARGB_8888
    )
}

private fun loadBitmapAsync(view: View): Deferred<Bitmap> = CoroutineScope(Dispatchers.Default).async {
    val width = view.width
    val height = view.height

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = CanvasG(bitmap)
    view.draw(canvas)

    return@async bitmap
}