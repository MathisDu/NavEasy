package com.example.lauzhack26_multiplatform.server.repository

import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus
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

    override suspend fun updateStatus(id: String, status: ReportStatus): Report? {
        val report = reports[id] ?: return null
        val updated = report.copy(status = status)
        reports[id] = updated
        return updated
    }

    override suspend fun incrementVote(id: String, voteType: String): Report? {
        val report = reports[id] ?: return null
        val updated = report.copy(resolvedVotes = report.resolvedVotes + 1)
        reports[id] = updated
        return updated
    }

    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
}
