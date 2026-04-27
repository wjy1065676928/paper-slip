package io.github.wjy.meditate.ui.home.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import io.github.wjy.meditate.data.JournalEntry
import io.github.wjy.meditate.data.JournalRepository
import io.github.wjy.meditate.ui.home.ShareDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

@Composable
fun FastTransferOverlay(
    visible: Boolean,
    qrBitmap: Bitmap?,
    canSwitchMode: Boolean = true,
    onClose: () -> Unit,
    onImport: (List<JournalEntry>) -> Unit
) {
    // 1. 状态锁定机制：在动画期间保持最后一份有效内容
    var cachedQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    
    // 扫码结果预览状态
    var scannedEntries by remember { mutableStateOf<List<JournalEntry>?>(null) }
    
    LaunchedEffect(visible, qrBitmap) {
        if (visible) {
            // 只有在弹窗显示期间才更新缓存
            if (qrBitmap != null) {
                cachedQrBitmap = qrBitmap
                isScanning = false
            } else {
                isScanning = true
            }
            scannedEntries = null // 每次打开时重置预览
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { JournalRepository.getInstance(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "需要相机权限来扫描", Toast.LENGTH_SHORT).show()
            if (cachedQrBitmap == null) onClose() else isScanning = false
        }
    }

    LaunchedEffect(visible, isScanning, hasCameraPermission) {
        if (visible && isScanning && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // 增加半透明背景，提升模糊感
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
                .zIndex(200f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val title = when {
                        scannedEntries != null -> "导入预览"
                        isScanning -> "扫码预览"
                        else -> "面对面快传"
                    }
                    
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (scannedEntries != null) {
                        val entries = scannedEntries!!
                        // 扫描结果预览列表
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("识别到 ${entries.size} 条记录：", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(entries) { entry ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = entry.content.take(50),
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    scannedEntries?.let { onImport(it) }
                                    onClose()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("留下")
                            }
                        }
                    } else {
                        // 扫码或展示码界面
                        Text(
                            if (isScanning) "对准另一台设备的二维码" else "让对方扫码接收选中的日记",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isScanning && hasCameraPermission) Color.Black else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isScanning && hasCameraPermission) {
                                QrScannerView(onResult = { result ->
                                    scope.launch {
                                        try {
                                            // 🚀 性能优化：在后台线程解析 JSON，避免界面卡顿
                                            val dtoList = withContext(Dispatchers.Default) {
                                                repository.json.decodeFromString<List<ShareDto>>(result)
                                            }
                                            scannedEntries = dtoList.map { dto ->
                                                JournalEntry(
                                                    content = dto.c,
                                                    moodTag = dto.t,
                                                    timestamp = dto.d,
                                                    selfAdvice = dto.a
                                                )
                                            }
                                            // 扫描成功，不再扫描，转为显示预览
                                            isScanning = false 
                                        } catch (e: Exception) {
                                            Log.e("FastTransfer", "Parse failed", e)
                                        }
                                    }
                                })
                            } else if (isScanning) {
                                 Text("正在请求相机权限...", color = Color.Gray)
                            } else {
                                // 使用缓存的图片，防止动画期间内容闪失
                                cachedQrBitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "二维码",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: Text("未生成二维码", color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (canSwitchMode && cachedQrBitmap != null) {
                                TextButton(
                                    onClick = { isScanning = !isScanning },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(if (isScanning) "显示我的码" else "扫码接收")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrScannerView(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val reader = remember {
        BarcodeReader(BarcodeReader.Options().apply {
            formats = setOf(BarcodeReader.Format.QR_CODE)
            tryRotate = true
            tryHarder = true
        })
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                // 核心修复：使用 COMPATIBLE (TextureView) 模式。
                // 只有 TextureView 才能跟随 Compose 的图形变换（透明度、缩放），解决动画分层问题。
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier
            .fillMaxSize()
            // 明确应用图形层以确保 alpha 能够传递到底层 native View
            .graphicsLayer { clip = true },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it ->
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val results = imageProxy.use { reader.read(it) }
                            if (results.isNotEmpty()) {
                                val result = results.first().text
                                if (!result.isNullOrBlank()) {
                                    onResult(result)
                                }
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("ScannerView", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        },
        onRelease = {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("ScannerView", "Release failed", e)
            }
        }
    )
}
