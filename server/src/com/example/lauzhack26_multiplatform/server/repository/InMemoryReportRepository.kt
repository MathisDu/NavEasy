package com.example.lauzhack26_multiplatform.server.repository

import com.example.lauzhack26_multiplatform.model.Report
import java.util.UUID

class InMemoryReportRepository : ReportRepository {

    private val reports = mutableMapOf<String, Report>()

    override suspend fun getAll(): List<Report> {
        return reports.values.toList()
    }

    override suspend fun getById(id: String): Report? {
        return reports[id]
    }

    override suspend fun add(report: Report): Report {
        reports[report.id] = report
        return report
    }

    override suspend fun remove(id: String): Boolean {
        return reports.remove(id) != null
    }

    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
}
