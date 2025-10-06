package com.example.masterlockr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors
import com.example.masterlockr.DatabaseHelper.InsertResult
import android.util.Log



class MainActivity : AppCompatActivity() {
    private lateinit var logoImageView: ImageView
    private lateinit var plusButton: Button
    private lateinit var addMediaText: TextView
    private lateinit var priceEditText: EditText
    private lateinit var generateLinkButton: Button
    private var lastGeneratedLink: String? = null

    // Database Helper
    private lateinit var dbHelper: DatabaseHelper

    private var userLoggedIn = false
    private var selectedImageUri: Uri? = null
    private var linkGenerated = false // Add a boolean flag
    private var previousSelectedImageUri: Uri? = null // Add a variable to store the previously selected image URI

    companion object {
        private const val REQUEST_CODE_PICK_FILE = 101
        private const val SERVER_UPLOAD_URL = "http://192.168.0.17:5000/upload"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this) // Instantiate your DatabaseHelper

        dbHelper.logAllUsers() // Log all user records upon launching the app

        logoImageView = findViewById(R.id.smallLogo)

        plusButton = findViewById(R.id.btnPlus)
        setCustomFont(plusButton)

        addMediaText = findViewById(R.id.tvAddMedia)
        setCustomFont(addMediaText)

        priceEditText = findViewById(R.id.etPrice)
        setCustomFont(priceEditText)

        generateLinkButton = findViewById(R.id.btnGenerateLink)
        setCustomFontForButton(generateLinkButton)

        // Set initial value for the price field
        priceEditText.setText(formatPrice(0.00))

        plusButton.setOnClickListener {
            // Check if the user is logged in
            if (isUserLoggedIn()) {
                // If logged in, proceed with file upload
                pickFile()
            } else {
                // If not logged in, show registration dialog
                showLoginDialog()
            }
        }

        generateLinkButton.setOnClickListener {
            // Check if the user is logged in
            if (isUserLoggedIn()) {
                // If logged in, generate and display the link
                generateAndDisplayLink()
            } else {
                // If not logged in, show registration dialog
                showLoginDialog()
            }
        }

        // Additional logic for editing the price field
        priceEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Validate and format the entered price
                validateAndFormatPrice()
            }
        }

        // Set CurrencyTextWatcher to the priceEditText
        priceEditText.addTextChangedListener(CurrencyTextWatcher())
    }

    private fun setCustomFont(view: View) {
        val customFont = Typeface.createFromAsset(assets, "SF-Pro-Regular.otf")
        when (view) {
            is TextView -> view.typeface = customFont
            is EditText -> view.typeface = customFont
        }
    }

    private fun setCustomFontForButton(button: Button) {
        val customFont = Typeface.createFromAsset(assets, "SF-Pro-Regular.otf")
        button.typeface = customFont
    }

    private fun isUserLoggedIn(): Boolean {
        // Implement your actual authentication logic here
        // For simplicity, using a boolean flag
        return userLoggedIn
    }

    private fun pickFile() {
        // Reset the linkGenerated flag when the user uploads new files
        linkGenerated = false

        // Check if the user is logged in
        if (!isUserLoggedIn()) {
            // If not logged in, show login dialog
            showLoginDialog()
        } else {
            // If logged in, proceed with file selection
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null) {
            // Get the selected image URI
            selectedImageUri = data.data

            // Check if the selected image URI is different from the previous one
            if (selectedImageUri != previousSelectedImageUri) {
                // If different, reset the linkGenerated flag and update the previousSelectedImageUri
                linkGenerated = false
                previousSelectedImageUri = selectedImageUri
            }

            // Perform actions with the selected file, e.g., display it
            displayUploadedFile(selectedImageUri)
        }
    }

    private fun displayUploadedFile(fileUri: Uri?) {
        // Logic to display the uploaded file, e.g., set it to the plusButton background
        if (fileUri != null) {
            // Set the background of plusButton to the uploaded image
            plusButton.background = getDrawableFromUri(fileUri)
        } else {
            // Handle the case where fileUri is null (optional)
            Toast.makeText(this@MainActivity, "Cannot display null image", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to get Drawable from Uri
    private fun getDrawableFromUri(uri: Uri): Drawable? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            Drawable.createFromStream(inputStream, uri.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun showLoginDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.login_layout, null)

        builder.setView(dialogView)
            .setTitle("Login")
            .setPositiveButton("Login") { dialog, _ ->
                // Implement login logic here
                val usernameEditText = dialogView.findViewById<EditText>(R.id.etUsernameLogin)
                val passwordEditText = dialogView.findViewById<EditText>(R.id.etPasswordLogin)

                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

                // Check if both username and password are not empty
                if (username.isNotBlank() && password.isNotBlank()) {
                    val isValidCredentials = dbHelper.isValidUserCredentials(username, password)

                    if (isValidCredentials) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        userLoggedIn = true
                    } else {
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Display a message indicating that both fields are required
                    Toast.makeText(this, "Both username and password are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Register") { dialog, _ ->
                // Implement registration logic here
                // Access the EditText views from dialogView to get user input
                showRegistrationDialog()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showRegistrationDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.registration_layout, null)

        builder.setView(dialogView)
            .setTitle("Register now")
            .setNegativeButton("Register") { dialog, _ ->
                // Access the EditText views from dialogView to get user input
                val usernameEditText = dialogView.findViewById<EditText>(R.id.etUsername)
                val passwordEditText = dialogView.findViewById<EditText>(R.id.etPassword)

                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

                // Check if both username and password are not empty
                if (username.isNotBlank() && password.isNotBlank()) {
                    // Call your registration function from the DatabaseHelper or perform registration logic
                    Executors.newSingleThreadExecutor().execute {
                        val registrationResult = dbHelper.insertUser(username, password)

                        // Handle the registration result
                        runOnUiThread {
                            when (registrationResult) {
                                is InsertResult.Success -> {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    // Optionally, automatically log in the user after registration
                                    userLoggedIn = true
                                    // Dismiss the registration dialog
                                    dialog.dismiss()
                                    // Directly log in after successful registration
                                    showLoginDialog()
                                }

                                is InsertResult.UsernameExists -> {
                                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                                }

                                is InsertResult.Error -> {
                                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                                }

                                else -> {
                                    // Handle unexpected result
                                    Toast.makeText(this, "Unexpected registration result", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } else {
                    // Display a message indicating that both fields are required
                    Toast.makeText(this, "Both username and password are required", Toast.LENGTH_SHORT).show()
                }
            }

        builder.create().show()
    }

    private fun generateAndDisplayLink() {
        // Check if a file is selected
        if (selectedImageUri == null) {
            Toast.makeText(this@MainActivity, "Please pick a file first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if a link has already been generated for the current file
        if (!linkGenerated) {
            // Generate the link only if it has not been generated before
            lifecycleScope.launch {
                // Perform network operation in a background thread
                val result = withContext(Dispatchers.IO) {
                    uploadFile(selectedImageUri)
                }
                handleUploadResult(result)

                // Set the flag to true to indicate that a link has been generated
                linkGenerated = true

                // Start DisplayImageActivity with the uploaded image URI
                startActivity(Intent(this@MainActivity, DisplayImageActivity::class.java).apply {
                    putExtra(DisplayImageActivity.EXTRA_IMAGE_URI, selectedImageUri)
                })
            }
        }

        // Copy the last generated link to the clipboard
        lastGeneratedLink?.let { copyToClipboard(it) }
        Toast.makeText(this@MainActivity, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }


    private fun uploadFile(imageUri: Uri?): String? {
        return if (imageUri != null) {
            try {
                // Open an input stream from the image URI
                val inputStream = contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    val url = URL(SERVER_UPLOAD_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.doOutput = true
                    connection.useCaches = false
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Connection", "Keep-Alive")
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****")
                    val outputStream = DataOutputStream(connection.outputStream)

                    // Set parameter name as "uploaded_file" which is defined in the server
                    val fieldName = "uploaded_file"

                    // Send parameter
                    outputStream.writeBytes("--*****\r\n")
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\";filename=\"image.jpg\"\r\n")
                    outputStream.writeBytes("\r\n")

                    // Read the image content and write it to the output stream
                    val buffer = ByteArray(1024)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } > 0) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.writeBytes("\r\n")
                    outputStream.writeBytes("--*****--\r\n")

                    // Close the streams
                    inputStream.close()
                    outputStream.flush()
                    outputStream.close()

                    // Get the server response code and message
                    val responseCode = connection.responseCode
                    val responseMessage = connection.responseMessage

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Server returns HTTP_OK, read the server response
                        val serverInputStream = connection.inputStream
                        val result = serverInputStream.bufferedReader().readText()

                        // Close the input stream
                        serverInputStream.close()

                        result
                    } else {
                        "Server returned non-OK status: $responseCode $responseMessage"
                    }
                } else {
                    Log.e("UploadFile", "Failed to open input stream from URI")
                    "Failed to open input stream from URI"
                }
            } catch (e: IOException) {
                e.printStackTrace()
                "Error during file upload: ${e.message}"
            }
        } else {
            "Image URI is null"
        }
    }

    private fun handleUploadResult(result: String?) {
        // Handle the result from the server
        if (result != null) {
            // Copy the generated link to the clipboard
            copyToClipboard(result)
            Toast.makeText(this@MainActivity, "Link copied to clipboard", Toast.LENGTH_SHORT)
                .show()

            // Set the flag to true to indicate that a link has been generated
            linkGenerated = true

            // Store the last generated link
            lastGeneratedLink = result
        } else {
            Toast.makeText(this@MainActivity, "Failed to upload file", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun copyToClipboard(text: String?) {
        if (text != null) {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Copied Link", text)
            clipboardManager.setPrimaryClip(clipData)
        } else {
            // Handle the case where the text is null (optional)
            Toast.makeText(this@MainActivity, "Cannot copy null text to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndFormatPrice() {
        // Get the entered price
        var enteredPrice = priceEditText.text.toString().replace("[^\\d.]".toRegex(), "")

        // Limit the length to 5 digits
        enteredPrice = enteredPrice.take(5)

        // Parse the entered price to a double
        val price = enteredPrice.toDoubleOrNull() ?: 0.00

        // Apply minimum and maximum limits
        val validatedPrice = when {
            price < 5.00 -> 5.00
            price > 20000.00 -> 20000.00
            else -> price
        }

        // Format the price with currency symbol and update the EditText
        val formattedPrice = String.format("$%.2f", validatedPrice)
        priceEditText.setText(formattedPrice)
    }

    inner class CurrencyTextWatcher : TextWatcher {

        private var mEditing: Boolean = false

        override fun afterTextChanged(s: Editable?) {
            if (!mEditing) {
                mEditing = true

                val digits = s.toString().replace("\\D".toRegex(), "")
                val nf = NumberFormat.getCurrencyInstance()

                try {
                    val formatted = nf.format(digits.toDouble() / 100.0)
                    s?.replace(0, s.length, formatted)
                } catch (nfe: NumberFormatException) {
                    s?.clear()
                }

                mEditing = false
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No implementation needed
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // No implementation needed
        }
    }

    private fun formatPrice(price: Double): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)
        return numberFormat.format(price)
    }
}