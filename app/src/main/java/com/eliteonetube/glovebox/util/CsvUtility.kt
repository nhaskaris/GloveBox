package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.data.entity.ServiceRecord
import java.text.SimpleDateFormat
import java.util.*

object CsvUtility {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun exportServiceRecords(records: List<ServiceRecord>): String {
        val sb = StringBuilder()
        // Header
        sb.append("Date,ServiceType,Mileage,Cost,Currency,Location,Mechanic,Notes,Parts,Labor,DIY\n")
        
        records.forEach { record ->
            sb.append(escapeCsv(dateFormat.format(Date(record.date)))).append(",")
            sb.append(escapeCsv(record.serviceType)).append(",")
            sb.append(record.mileage).append(",")
            sb.append(record.cost ?: "").append(",")
            sb.append(escapeCsv(record.currency)).append(",")
            sb.append(escapeCsv(record.serviceLocation ?: "")).append(",")
            sb.append(escapeCsv(record.mechanicName ?: "")).append(",")
            sb.append(escapeCsv(record.notes)).append(",")
            sb.append(escapeCsv(record.partsUsed ?: "")).append(",")
            sb.append(record.laborHours ?: "").append(",")
            sb.append(if (record.isDiy) "1" else "0").append("\n")
        }
        return sb.toString()
    }

    fun parseServiceRecords(csvContent: String, vehicleId: Long): List<ServiceRecord> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList() // Only header or empty

        val records = mutableListOf<ServiceRecord>()
        // Skip header
        lines.drop(1).forEach { line ->
            try {
                val parts = splitCsvLine(line)
                if (parts.size >= 2) {
                    val date = try { dateFormat.parse(parts[0])?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                    records.add(ServiceRecord(
                        vehicleId = vehicleId,
                        date = date,
                        serviceType = parts.getOrNull(1) ?: "Imported Service",
                        mileage = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                        cost = parts.getOrNull(3)?.toDoubleOrNull(),
                        currency = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "USD",
                        serviceLocation = parts.getOrNull(5)?.takeIf { it.isNotBlank() },
                        mechanicName = parts.getOrNull(6)?.takeIf { it.isNotBlank() },
                        notes = parts.getOrNull(7) ?: "",
                        partsUsed = parts.getOrNull(8)?.takeIf { it.isNotBlank() },
                        laborHours = parts.getOrNull(9)?.toDoubleOrNull(),
                        isDiy = parts.getOrNull(10) == "1"
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return records
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentPart = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    currentPart.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentPart.toString())
                currentPart = StringBuilder()
            } else {
                currentPart.append(c)
            }
            i++
        }
        result.add(currentPart.toString())
        return result
    }
}
