package com.example.smartled.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smartled.util.Security

class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "smartled.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Usuario por defecto: admin / 1234 (con hash)
        val cv = ContentValues().apply {
            put("username", "admin")
            put("password_hash", Security.sha256("1234"))
        }
        db.insert("users", null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun validateUser(user: String, pass: String): Boolean {
        val hash = Security.sha256(pass)
        readableDatabase.rawQuery(
            "SELECT 1 FROM users WHERE username=? AND password_hash=? LIMIT 1",
            arrayOf(user, hash)
        ).use { c -> return c.moveToFirst() }
    }
}