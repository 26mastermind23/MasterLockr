package com.example.masterlockr

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MasterTest"
        private const val TABLE_USERS = "users"
        private const val KEY_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    // Define InsertResult enum class
    sealed class InsertResult {
        data class Success(val userId: Long) : InsertResult()
        object UsernameExists : InsertResult()
        object Error : InsertResult()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUserTable = (
                "CREATE TABLE $TABLE_USERS("
                        + "$KEY_ID INTEGER PRIMARY KEY,"
                        + "$KEY_USERNAME TEXT,"
                        + "$KEY_PASSWORD TEXT"
                        + ")"
                )
        db.execSQL(createUserTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertUser(username: String, password: String): InsertResult {
        val db = this.writableDatabase

        // Check if the username already exists
        if (isUsernameExists(username)) {
            // Username already exists, return an error message
            return InsertResult.UsernameExists
        }

        val values = ContentValues()
        values.put(KEY_USERNAME, username)
        values.put(KEY_PASSWORD, password)

        // Insert the user into the database
        val result = db.insert(TABLE_USERS, null, values)

        return if (result != -1L) {
            // Successfully inserted the user, return a success message or user ID
            InsertResult.Success(result)
        } else {
            // Failed to insert the user, return an error message
            InsertResult.Error
        }
    }

    fun isValidUserCredentials(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $KEY_USERNAME = ? AND $KEY_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    private fun isUsernameExists(username: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $KEY_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        val usernameExists = cursor.count > 0
        cursor.close()
        return usernameExists
    }

    fun logAllUsers() {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_USERS", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                val username = cursor.getString(cursor.getColumnIndex(KEY_USERNAME))
                val password = cursor.getString(cursor.getColumnIndex(KEY_PASSWORD))

                val logMessage = "ID: $id, Username: $username, Password: $password"
                Log.d("DatabaseHelper", logMessage)

            } while (cursor.moveToNext())
        } else {
            Log.d("DatabaseHelper", "No users found in the database")
        }

        cursor.close()
    }
}
