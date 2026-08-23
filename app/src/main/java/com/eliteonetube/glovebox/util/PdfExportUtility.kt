package com.eliteonetube.glovebox.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtility {

    /**
     * Generates a comprehensive PDF document including vehicle info, service history, and fuel logs.
     */
    fun generateFullVehicleHistoryPdf(
        context: Context,
        vehicle: Vehicle,
        records: List<ServiceRecord>,
        fuelLogs: List<FuelLog>,
        includeCosts: Boolean = true,
        includeShop: Boolean = true,
        includeMechanic: Boolean = true,
        includeFuel: Boolean = true,
        includeSummary: Boolean = true,
        preferredCurrency: String = "USD"
    ): File {
        val converter = CurrencyUtility
        // ...
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        val margin = 40f
        var currentY = 50f

        // Paints
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
            color = Color.BLACK
        }

        val sectionHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 16f
            color = Color.rgb(0, 102, 204)
        }

        val tableHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.BLACK
        }

        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9f
            color = Color.BLACK
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun checkNewPage() {
            if (currentY > pageHeight - 60f) {
                pdfDocument.finishPage(currentPage)
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                currentY = 50f
            }
        }

        // 1. Title
        canvas.drawText("Full Vehicle History Report", margin, currentY, titlePaint)
        currentY += 40f

        // 2. Vehicle Information Section
        canvas.drawText("Vehicle Information", margin, currentY, sectionHeaderPaint)
        currentY += 25f

        val infoCol1 = margin
        val infoCol2 = margin + 200f

        val vehicleInfo = listOf(
            "Nickname" to (vehicle.nickname ?: "N/A"),
            "Make/Model" to "${vehicle.make} ${vehicle.model}",
            "Year" to vehicle.year.toString(),
            "VIN" to (vehicle.vin ?: "N/A"),
            "Fuel Type" to (vehicle.fuelType ?: "N/A"),
            "Current Odometer" to "${vehicle.odometer} ${vehicle.odometerUnit}",
            "License Plate" to (vehicle.licensePlate ?: "N/A"),
            "Color" to (vehicle.color ?: "N/A")
        )

        vehicleInfo.chunked(2).forEach { pair ->
            canvas.drawText("${pair[0].first}: ${pair[0].second}", infoCol1, currentY, bodyPaint)
            if (pair.size > 1) {
                canvas.drawText("${pair[1].first}: ${pair[1].second}", infoCol2, currentY, bodyPaint)
            }
            currentY += 15f
        }
        currentY += 20f

        // 3. Maintenance History Section
        canvas.drawText("Maintenance History", margin, currentY, sectionHeaderPaint)
        currentY += 20f

        val mColDate = margin
        val mColType = margin + 70f
        val mColMileage = margin + 200f
        val mColCost = margin + 300f
        val mColLocation = margin + 380f

        canvas.drawText("Date", mColDate, currentY, tableHeaderPaint)
        canvas.drawText("Service Type", mColType, currentY, tableHeaderPaint)
        canvas.drawText("Mileage", mColMileage, currentY, tableHeaderPaint)
        if (includeCosts) {
            canvas.drawText("Cost", mColCost, currentY, tableHeaderPaint)
        }
        if (includeShop || includeMechanic) {
            canvas.drawText("Location/Mechanic", mColLocation, currentY, tableHeaderPaint)
        }
        currentY += 5f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        currentY += 15f

        var totalMaintenanceCostConverted = 0.0
        var totalFuelCostConverted = 0.0
        
        records.sortedByDescending { it.date }.forEach { record ->
            checkNewPage()

            val dateStr = dateFormat.format(Date(record.date))
            val typeStr = if (record.serviceType == "Other") record.otherDescription ?: "Other" else record.serviceType
            val costVal = record.cost ?: 0.0
            totalMaintenanceCostConverted += converter.convert(costVal, record.currency, preferredCurrency)

            canvas.drawText(dateStr, mColDate, currentY, bodyPaint)
            canvas.drawText(typeStr.take(25), mColType, currentY, bodyPaint)
            canvas.drawText("${record.mileage} ${vehicle.odometerUnit}", mColMileage, currentY, bodyPaint)
            
            if (includeCosts) {
                canvas.drawText(formatCost(costVal, record.currency), mColCost, currentY, bodyPaint)
            }

            val loc = when {
                record.isDiy -> "DIY"
                record.mechanicName != null -> {
                    if (includeShop && includeMechanic) "${record.serviceLocation ?: "Shop"} (${record.mechanicName})"
                    else if (includeShop) record.serviceLocation ?: "Shop"
                    else if (includeMechanic) record.mechanicName
                    else ""
                }
                else -> {
                    if (includeShop) record.serviceLocation ?: "N/A"
                    else ""
                }
            }
            if (includeShop || includeMechanic) {
                canvas.drawText(loc.take(30), mColLocation, currentY, bodyPaint)
            }

            currentY += 15f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 5f
        }
        currentY += 30f

        // 4. Fuel Consumption History Section
        if (includeFuel) {
            checkNewPage()
            canvas.drawText("Fuel Consumption History", margin, currentY, sectionHeaderPaint)
            currentY += 20f

            val fColDate = margin
            val fColOdo = margin + 70f
            val fColAmount = margin + 170f
            val fColCost = margin + 270f
            val fColStation = margin + 370f

            canvas.drawText("Date", fColDate, currentY, tableHeaderPaint)
            canvas.drawText("Odometer", fColOdo, currentY, tableHeaderPaint)
            canvas.drawText("Amount", fColAmount, currentY, tableHeaderPaint)
            if (includeCosts) {
                canvas.drawText("Total Cost", fColCost, currentY, tableHeaderPaint)
            }
            if (includeShop) {
                canvas.drawText("Station/Location", fColStation, currentY, tableHeaderPaint)
            }
            currentY += 5f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 15f

            fuelLogs.sortedByDescending { it.date }.forEach { log ->
                checkNewPage()

                val dateStr = dateFormat.format(Date(log.date))
                totalFuelCostConverted += converter.convert(log.totalCost, log.currency, preferredCurrency)

                canvas.drawText(dateStr, fColDate, currentY, bodyPaint)
                canvas.drawText("${log.odometer} ${vehicle.odometerUnit}", fColOdo, currentY, bodyPaint)
                canvas.drawText("%.2f L".format(log.amount), fColAmount, currentY, bodyPaint)
                if (includeCosts) {
                    canvas.drawText(formatCost(log.totalCost, log.currency), fColCost, currentY, bodyPaint)
                }
                if (includeShop) {
                    canvas.drawText((log.location ?: "N/A").take(30), fColStation, currentY, bodyPaint)
                }

                currentY += 15f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                currentY += 5f
            }
            currentY += 40f
        } else {
            // Still calculate total fuel cost for summary if summary is requested, but maybe not?
            // User said "remove include fuel consumption history". Usually implies don't show it.
            // If they exclude history but include summary, summary might be confusing.
            // Let's just skip the section.
        }

        // 5. Summary Section
        if (includeSummary) {
            checkNewPage()
            canvas.drawText("Summary (All values converted to $preferredCurrency)", margin, currentY, sectionHeaderPaint)
            currentY += 25f

            val summaryPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 12f
                color = Color.BLACK
            }

            if (includeCosts) {
                canvas.drawText("Total Maintenance Cost:", margin, currentY, bodyPaint)
                canvas.drawText(formatCost(totalMaintenanceCostConverted, preferredCurrency), margin + 150f, currentY, summaryPaint)
                currentY += 20f

                if (includeFuel) {
                    canvas.drawText("Total Fuel Cost:", margin, currentY, bodyPaint)
                    canvas.drawText(formatCost(totalFuelCostConverted, preferredCurrency), margin + 150f, currentY, summaryPaint)
                    currentY += 20f

                    canvas.drawLine(margin, currentY, margin + 250f, currentY, linePaint)
                    currentY += 25f

                    val grandTotalPaint = Paint().apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 14f
                        color = Color.rgb(204, 0, 0)
                    }
                    canvas.drawText("GRAND TOTAL:", margin, currentY, summaryPaint)
                    canvas.drawText(formatCost(totalMaintenanceCostConverted + totalFuelCostConverted, preferredCurrency), margin + 150f, currentY, grandTotalPaint)
                }
            } else {
                canvas.drawText("Total Records Exported:", margin, currentY, bodyPaint)
                canvas.drawText("${records.size + (if (includeFuel) fuelLogs.size else 0)}", margin + 150f, currentY, summaryPaint)
            }
        }

        pdfDocument.finishPage(currentPage)

        val fileName = "FullReport_${vehicle.make}_${vehicle.model}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    /**
     * Generates a PDF document for the vehicle's service history.
     */
    fun generateVehicleServiceHistoryPdf(
        context: Context,
        vehicle: Vehicle,
        records: List<ServiceRecord>,
        preferredCurrency: String = "USD"
    ): File {
        val converter = CurrencyUtility
        // ...
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
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
        var totalCostConverted = 0.0

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

            totalCostConverted += converter.convert(record.cost ?: 0.0, record.currency, preferredCurrency)
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
        val totalCostStr = "Total Cost (converted to $preferredCurrency): ${formatCost(totalCostConverted, preferredCurrency)}"
        canvas.drawText(totalCostStr, colCost, currentY, headerPaint)

        pdfDocument.finishPage(currentPage)

        val file = File(context.cacheDir, "ServiceHistory_${vehicle.make}_${vehicle.model}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    private fun formatCost(cost: Double?, currency: String): String {
        val symbol = CurrencyUtility.getCurrencySymbol(currency)
        return "$symbol%.2f".format(cost ?: 0.0)
    }
}
