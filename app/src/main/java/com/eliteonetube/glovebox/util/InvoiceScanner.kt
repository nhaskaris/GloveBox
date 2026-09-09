package com.eliteonetube.glovebox.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ScannedInvoice(
    val date: Long? = null,
    val totalCost: Double? = null,
    val mileage: Int? = null,
    val detectedServiceType: String? = null,
    val shopName: String? = null,
    val rawText: String? = null
)

object InvoiceScanner {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scanUri(context: Context, uri: Uri): List<ScannedInvoice> = withContext(Dispatchers.IO) {
        val allResults = mutableListOf<ScannedInvoice>()
        try {
            val isPdf = context.contentResolver.getType(uri)?.contains("pdf") == true
            
            if (isPdf) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext emptyList()
                val renderer = PdfRenderer(pfd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = recognizer.process(image).await()
                    allResults.addAll(extractMultipleServices(result.text))
                    
                    page.close()
                }
                renderer.close()
                pfd.close()
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val result = recognizer.process(image).await()
                        allResults.addAll(extractMultipleServices(result.text))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        allResults
    }

    private fun extractMultipleServices(text: String): List<ScannedInvoice> {
        val invoices = mutableListOf<ScannedInvoice>()
        
        // Pattern to identify the start of a service block
        val headerPattern = Pattern.compile("(?i)(Minor Service|Major Service|Service Record|Timeline)", Pattern.CASE_INSENSITIVE)
        val matcher = headerPattern.matcher(text)
        
        val startIndices = mutableListOf<Int>()
        while (matcher.find()) {
            startIndices.add(matcher.start())
        }

        if (startIndices.isEmpty()) {
            // If no clear headers, try to process the whole text as one
            val single = extractDataFromText(text)
            if (single.date != null || single.totalCost != null) {
                invoices.add(single)
            }
        } else {
            for (i in startIndices.indices) {
                val start = startIndices[i]
                val end = if (i + 1 < startIndices.size) startIndices[i + 1] else text.length
                val sectionText = text.substring(start, end)
                if (sectionText.length > 30) {
                    invoices.add(extractDataFromText(sectionText))
                }
            }
        }
        
        return invoices.filter { it.date != null || it.totalCost != null || it.mileage != null }
    }

    private fun extractDataFromText(text: String): ScannedInvoice {
        var date: Long? = null
        var cost: Double? = null
        var mileage: Int? = null
        var serviceType: String? = null

        // 1. Extract Date
        val dateFormats = listOf(
            "dd MMM yyyy", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", 
            "dd MMMM yyyy", "MMM dd, yyyy", "dd-MMM-yyyy"
        )
        // Clean up text a bit to handle OCR artifacts in dates
        val cleanText = text.replace(Regex("[|]"), " ")
        
        val datePattern = Pattern.compile("(\\d{1,2}[\\s/-](?:\\d{1,2}|[a-zA-Z\\u0370-\\u03ff]{3,10})[\\s/-]\\d{2,4})|(\\d{4}-\\d{2}-\\d{2})")
        val dateMatcher = datePattern.matcher(cleanText)
        
        val dateCandidates = mutableListOf<Long>()
        while (dateMatcher.find()) {
            val dateStr = dateMatcher.group()
            for (format in dateFormats) {
                try {
                    val d = SimpleDateFormat(format, Locale.US).apply { isLenient = false }.parse(dateStr)
                    if (d != null) { dateCandidates.add(d.time); break }
                } catch (e: Exception) {
                    try {
                        val d = SimpleDateFormat(format, Locale.getDefault()).apply { isLenient = false }.parse(dateStr)
                        if (d != null) { dateCandidates.add(d.time); break }
                    } catch (e2: Exception) {}
                }
            }
        }
        date = dateCandidates.firstOrNull()

        // 2. Extract Cost (More flexible: look for values after labels or just currency symbols)
        val costLabelPattern = Pattern.compile("(?:Total|Amount|Balance|Sum|Price|Cost|Σύνολο|Κόστος)[\\s:]*", Pattern.CASE_INSENSITIVE)
        val amountPattern = Pattern.compile("[\\$€£]?\\s*(\\d[\\d,]*\\.\\d{2})")
        
        val costMatcher = amountPattern.matcher(cleanText)
        val costCandidates = mutableListOf<Double>()
        while (costMatcher.find()) {
            costMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()?.let { costCandidates.add(it) }
        }
        
        // If "Cost" label exists, try to find the number nearest to it
        val labelMatcher = costLabelPattern.matcher(cleanText)
        if (labelMatcher.find()) {
            val labelEnd = labelMatcher.end()
            // Find candidate closest to label
            cost = costCandidates.minByOrNull { Math.abs(cleanText.indexOf(it.toString()) - labelEnd) }
        }
        
        if (cost == null) cost = costCandidates.maxOrNull() // Final fallback

        // 3. Extract Mileage
        val mileagePattern = Pattern.compile("(\\d[\\d,]{1,6})\\s*(?:Km|Miles|Mi|χλμ|Odometer|Mileage|Οδόμετρο)", Pattern.CASE_INSENSITIVE)
        val mileageMatcher = mileagePattern.matcher(cleanText)
        if (mileageMatcher.find()) {
            mileage = mileageMatcher.group(1)?.replace(",", "")?.toIntOrNull()
        }

        // 4. Extract Service Type
        val keywords = mapOf(
            "Oil" to "Oil Change",
            "Λάδι" to "Oil Change",
            "Tire" to "Tire Rotation",
            "Ελαστικά" to "Tire Rotation",
            "Brake" to "Brake Service",
            "Φρένα" to "Brake Service",
            "Filter" to "Filter Replacement",
            "Φίλτρο" to "Filter Replacement",
            "Battery" to "Battery Service",
            "Μπαταρία" to "Battery Service",
            "Spark" to "Spark Plugs",
            "Μπουζί" to "Spark Plugs",
            "Minor" to "Minor Service",
            "Major" to "Major Service"
        )
        for ((key, value) in keywords) {
            if (cleanText.contains(key, ignoreCase = true)) {
                serviceType = value
                break
            }
        }

        return ScannedInvoice(
            date = date,
            totalCost = cost,
            mileage = mileage,
            detectedServiceType = serviceType
        )
    }
}
