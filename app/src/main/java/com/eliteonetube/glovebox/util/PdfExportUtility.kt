package com.eliteonetube.glovebox.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtility {

    /**
     * Generates a PDF document for the vehicle's service history.
     * 
     * @param context The application context.
     * @param vehicle The vehicle entity.
     * @param records The list of service records for the vehicle.
     * @return The File object pointing to the generated PDF in the cache directory.
     */
    fun generateVehicleServiceHistoryPdf(
        context: Context,
        vehicle: Vehicle,
        records: List<ServiceRecord>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        
        val margin = 40f
        var currentY = 50f
        
        // Paints
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = Color.BLACK
        }
        
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.BLACK
        }
        
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.BLACK
        }
        
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // 1. Header
        canvas.drawText("Vehicle Service History", margin, currentY, titlePaint)
        currentY += 40f
        
        val vehicleName = vehicle.nickname ?: "${vehicle.year} ${vehicle.make} ${vehicle.model}"
        canvas.drawText("Vehicle: $vehicleName", margin, currentY, headerPaint)
        currentY += 20f
        
        val vehicleDetails = "${vehicle.year} ${vehicle.make} ${vehicle.model} | VIN: ${vehicle.vin ?: "N/A"}"
        canvas.drawText(vehicleDetails, margin, currentY, bodyPaint)
        currentY += 40f
        
        // 2. Table Headers
        val colDate = margin
        val colType = margin + 80f
        val colMileage = margin + 230f
        val colCost = margin + 320f
        val colLocation = margin + 400f

        canvas.drawText("Date", colDate, currentY, headerPaint)
        canvas.drawText("Type", colType, currentY, headerPaint)
        canvas.drawText("Mileage", colMileage, currentY, headerPaint)
        canvas.drawText("Cost", colCost, currentY, headerPaint)
        canvas.drawText("Location", colLocation, currentY, headerPaint)
        
        currentY += 5f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 20f
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        var totalCost = 0.0

        // 3. Service Records
        records.forEach { record ->
            // Check for page overflow
            if (currentY > pageHeight - 80f) {
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 50f
            }
            
            val dateStr = dateFormat.format(Date(record.date))
            val typeStr = if (record.serviceType == "Other") record.otherDescription ?: "Other" else record.serviceType
            val mileageStr = "${record.mileage} ${vehicle.odometerUnit}"
            val costStr = formatCost(record.cost, record.currency)
            val locationStr = when {
                record.isDiy -> "DIY"
                record.mechanicName != null -> "${record.serviceLocation ?: "Shop"} (${record.mechanicName})"
                else -> record.serviceLocation ?: ""
            }
            
            canvas.drawText(dateStr, colDate, currentY, bodyPaint)
            
            // Handle long type strings (basic truncation)
            val truncatedType = if (typeStr.length > 25) typeStr.take(22) + "..." else typeStr
            canvas.drawText(truncatedType, colType, currentY, bodyPaint)
            
            canvas.drawText(mileageStr, colMileage, currentY, bodyPaint)
            canvas.drawText(costStr, colCost, currentY, bodyPaint)
            
            val truncatedLoc = if (locationStr.length > 20) locationStr.take(17) + "..." else locationStr
            canvas.drawText(truncatedLoc, colLocation, currentY, bodyPaint)
            
            totalCost += (record.cost ?: 0.0)
            currentY += 15f
            
            // Draw notes if present
            if (record.notes.isNotBlank()) {
                val notesStr = "Notes: ${record.notes}"
                val truncatedNotes = if (notesStr.length > 100) notesStr.take(97) + "..." else notesStr
                canvas.drawText(truncatedNotes, colType, currentY, bodyPaint.apply { textSize = 8f })
                bodyPaint.textSize = 10f // Reset
                currentY += 15f
            }
            
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 20f
        }
        
        // 4. Footer
        if (currentY > pageHeight - 60f) {
            pdfDocument.finishPage(currentPage)
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = 50f
        }
        
        currentY += 20f
        canvas.drawText("Total Records: ${records.size}", margin, currentY, headerPaint)
        val totalCostStr = "Total Cost: ${formatCost(totalCost, records.firstOrNull()?.currency ?: "USD")}"
        canvas.drawText(totalCostStr, colCost, currentY, headerPaint)
        
        pdfDocument.finishPage(currentPage)
        
        val file = File(context.cacheDir, "ServiceHistory_${vehicle.make}_${vehicle.model}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        return file
    }
    
    private fun formatCost(cost: Double?, currency: String): String {
        val symbol = when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$currency "
        }
        return "$symbol%.2f".format(cost ?: 0.0)
    }
}