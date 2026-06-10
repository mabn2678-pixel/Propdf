package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var database: PdfDatabase? = null
    private var repository: PdfRepository? = null

    fun getDatabase(context: Context): PdfDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PdfDatabase::class.java,
                "wps_pdf_reader_db"
            ).fallbackToDestructiveMigration().build()
            database = instance
            instance
        }
    }

    fun getRepository(context: Context): PdfRepository {
        return repository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = PdfRepository(db.pdfDao, db.vocabDao)
            repository = repo
            repo
        }
    }
}
