package com.example.gorillagroove.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gorillagroove.App
import com.example.gorillagroove.R
import com.example.gorillagroove.client.HttpClient
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.model.User
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.utils.URLs
import com.example.gorillagroove.utils.content
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.new_content_main.button_login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

private const val MainActivityTag = "MainActivity"

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val httpClient = HttpClient()

    private lateinit var userRepository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRepository = UserRepository(GroovinDB.getDatabase().userRepository())

        runBlocking {
            withContext(Dispatchers.IO) {
                val user = userRepository.lastLoggedInUser()
                Log.i(MainActivityTag, "Last logged in user=$user")
                if (user != null) {
                    if (user.deviceId == null) {
                        val deviceId = UUID.randomUUID().toString()
                        userRepository.updateDeviceId(user.id, deviceId)

                        val anotherOne = userRepository.lastLoggedInUser()
                        if (anotherOne != null) startActivity(createPlaylistIntent(anotherOne))
                    } else {
                        startActivity(createPlaylistIntent(user))
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        emailField = findViewById(R.id.edit_text_email)
        passwordField = findViewById(R.id.edit_text_password)

        emailField.requestFocus()

        button_login.setOnClickListener {
            val loginRequest = LoginRequest(emailField.content(), passwordField.content())
            httpClient.post(URLs.LOGIN, loginRequest, LoginResponse::class) { response ->
                if (response.statusCode == 403) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Incorrect login credentials, please try again",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@post
                }

                val (email, username, token) = response.data

                val existingUser: User? = userRepository.findUser(email)

                if (existingUser != null) {
                    if (existingUser.deviceId == null) {
                        val deviceId = UUID.randomUUID().toString()
                        userRepository.updateDeviceId(existingUser.id, deviceId)
                    }
                    userRepository.updateToken(existingUser, token)

                    startActivity(createPlaylistIntent(existingUser))
                } else {
                    val deviceId = UUID.randomUUID().toString()
                    val newUser = userRepository.createUser(username, email, token, deviceId)

                    startActivity(createPlaylistIntent(newUser))
                }

                runOnUiThread {
                    emailField.text.clear()
                    passwordField.text.clear()
                }
            }
        }
    }

    private fun createPlaylistIntent(user: User): Intent {
        val request = UpdateDeviceRequest(deviceId = user.deviceId!!)
        httpClient.put(URLs.DEVICE, request)

        val intent = Intent(applicationContext, PlaylistActivity::class.java)
        intent.putExtra("token", user.token)
        intent.putExtra("username", user.userName)
        intent.putExtra("email", user.email)
        intent.putExtra("deviceId", user.deviceId)
        return intent
    }

    private data class LoginRequest(val email: String, val password: String)

    private data class LoginResponse(val email: String, val username: String, val token: String)

    private data class UpdateDeviceRequest(
        val deviceId: String,
        val deviceType: String = "ANDROID",
        val version: String = App.VERSION
    )
}
