package com.appkings.murbs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // get intent, action and MIME type
        val intent = getIntent()
        val action = intent.getAction()
        val type = intent.getType()

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent) // handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent) // handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent) // handle multiple images being sent
            }
        } else {
            val tomain = Intent(this, MainActivity::class.java)
            startActivity(tomain)
        }
    }

    fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            val i = Intent(getBaseContext(), MainActivity::class.java)
            i.putExtra("s_uri", sharedText)
            startActivity(i)
            finish()
        }
    }

    // ~ this thing kinda not working at the moment; anybody want to help?
    private fun handleSendImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("s_img", imageUri.toString())
            startActivity(i)
            finish()
        } else {
            Toast.makeText(this, "Error occurred, URI is invalid", Toast.LENGTH_LONG).show()
        }
    }

    fun handleSendMultipleImages(intent: Intent) {
        val imageUris = intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)
        if (imageUris != null) {
            // update UI to reflect multiple images being shared
        }
    }
}