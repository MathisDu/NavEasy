package com.example.lauzhack26_multiplatform.server.repository

import com.example.lauzhack26_multiplatform.model.Report
import com.example.lauzhack26_multiplatform.model.ReportStatus

interface ReportRepository {
    suspend fun getAll(): List<Report>
    suspend fun getById(id: String): Report?
    suspend fun add(report: Report): Report
    suspend fun remove(id: String): Boolean
    suspend fun updateStatus(id: String, status: ReportStatus): Report?
    suspend fun incrementVote(id: String, voteType: String): Report?
}
