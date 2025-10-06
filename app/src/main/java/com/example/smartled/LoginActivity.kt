package com.example.smartled

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartled.db.AppDatabaseHelper

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val db = AppDatabaseHelper(this)
        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPass)
        val tvMsg  = findViewById<TextView>(R.id.tvMsg)
        val btn    = findViewById<Button>(R.id.btnLogin)

        btn.setOnClickListener {
            val u = etUser.text.toString().trim()
            val p = etPass.text.toString().trim()
            if (db.validateUser(u, p)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvMsg.text = "Usuario o contraseña inválidos"
            }
        }
    }
}