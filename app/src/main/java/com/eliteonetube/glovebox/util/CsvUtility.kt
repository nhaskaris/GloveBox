package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.data.entity.ServiceRecord
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

object CsvUtility {
    fun exportServiceRecords(records: List<ServiceRecord>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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

    /**
     * Parses a CSV produced by [exportServiceRecords].
     *
     * Rows are skipped (and counted in [CsvImportResult.skipped]) when they have no valid date or
     * service type, or when they duplicate a record in [existing] or an earlier row of the file.
     */
    fun parseServiceRecords(
        csvContent: String,
        vehicleId: Long,
        existing: List<ServiceRecord> = emptyList()
    ): CsvImportResult {
        val rows = parseRows(csvContent.removePrefix("\uFEFF"))
        if (rows.size <= 1) return CsvImportResult(emptyList(), 0)

        val format = newDateFormat()
        val seen = existing.mapTo(mutableSetOf()) { duplicateKey(format.format(Date(it.date)), it.serviceType, it.mileage, it.cost) }
        val records = mutableListOf<ServiceRecord>()
        var skipped = 0

        rows.drop(1).forEach { parts ->
            val date = parts.getOrNull(0)?.trim()?.let { parseStrictDate(format, it) }
            val serviceType = parts.getOrNull(1)?.trim().orEmpty()
            if (date == null || serviceType.isEmpty()) {
                skipped++
                return@forEach
            }
            val mileage = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            val cost = parts.getOrNull(3)?.trim()?.toDoubleOrNull()

            if (!seen.add(duplicateKey(format.format(Date(date)), serviceType, mileage, cost))) {
                skipped++
                return@forEach
            }

            records.add(
                ServiceRecord(
                    vehicleId = vehicleId,
                    date = date,
                    serviceType = serviceType,
                    mileage = mileage,
                    cost = cost,
                    currency = parts.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "USD",
                    serviceLocation = parts.getOrNull(5)?.takeIf { it.isNotBlank() },
                    mechanicName = parts.getOrNull(6)?.takeIf { it.isNotBlank() },
                    notes = parts.getOrNull(7) ?: "",
                    partsUsed = parts.getOrNull(8)?.takeIf { it.isNotBlank() },
                    laborHours = parts.getOrNull(9)?.trim()?.toDoubleOrNull(),
                    isDiy = parts.getOrNull(10)?.trim() == "1"
                )
            )
        }
        return CsvImportResult(records, skipped)
    }

    private fun newDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private fun parseStrictDate(format: SimpleDateFormat, text: String): Long? {
        val position = ParsePosition(0)
        val date = format.parse(text, position) ?: return null
        return if (position.index == text.length) date.time else null
    }

    private fun duplicateKey(day: String, type: String, mileage: Int, cost: Double?) =
        listOf(day, type.trim().lowercase(), mileage, cost)

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /** RFC 4180 style parser: handles quoted fields containing commas, quotes and line breaks. */
    private fun parseRows(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() { row.add(field.toString()); field.setLength(0) }
        fun endRow() {
            endField()
            // Ignore completely blank lines.
            if (row.size > 1 || row[0].isNotBlank()) rows.add(row)
            row = mutableListOf()
        }

        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes && c == '"' && content.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                inQuotes -> field.append(c)
                c == ',' -> endField()
                c == '\r' -> { if (content.getOrNull(i + 1) == '\n') i++; endRow() }
                c == '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}

data class CsvImportResult(val records: List<ServiceRecord>, val skipped: Int)
