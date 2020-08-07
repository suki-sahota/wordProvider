package com.example.wordprovider

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.*
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val REQ_PERMISSION = 9287
    private var listOfContactsRaw = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallback(this)
        }
        getPermissionRuntime()

        // Test connectivity
        btn_connectivity.setOnClickListener {
            Log.d(TAG, "onCreate: isNetworkConnected = $isNetworkConnected")
            Log.d(TAG, " ")

            Log.d(TAG, "onCreate: Capabilities: ")
            Log.d(TAG, "onCreate: internetCapable = $internetCapable")
            Log.d(TAG, "onCreate: netNotCongested = $netNotCongested")
            Log.d(TAG, "onCreate: netNotMetered = $netNotMetered")
            Log.d(TAG, " ")

            Log.d(TAG, "onCreate: Transports: ")
            Log.d(TAG, "onCreate: bluetoothTransport = $bluetoothTransport")
            Log.d(TAG, "onCreate: cellularTransport = $cellularTransport")
            Log.d(TAG, "onCreate: wifiTransport = $wifiTransport")
            Log.d(TAG, " ")
            Log.d(TAG, " ")
        }

        // Display one string from WordProvider
        btn_one.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            displayOneEntry()
        }

        // Display WordProvider list
        btn_list.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            displayEntries()
        }

        // Insert string into WordProvider
        btn_insert.setOnClickListener {
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            insertEntryToWordProvider()
        }

        // Read from contacts and insert into WordProvider
        btn_read_contacts.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            readContacts()
        }

        // Write contact into ContactsContract
        btn_write_contacts.setOnClickListener {
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            insertAContact()
        }

        // To allow scrollable TextView
        tv_provider_display.movementMethod = ScrollingMovementMethod()
    }

    private fun getPermissionRuntime() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS), REQ_PERMISSION)
    }

    private fun insertEntryToWordProvider() { // Inserts into local WordProvider
        val contentValues = ContentValues()
        // Add a value to the set
        contentValues.put(WordProvider.CONTENT_PATH, et_insert_name.text.toString())
        et_insert_name.text.clear()
        contentResolver.insert(
            WordProvider.CONTENT_URI,
            contentValues
        )
        displayEntries()
    }

    private fun insertAContact() { // Add to ContactsContract; does not insert into local WordProvider
        val batchOperation
                = arrayListOf<ContentProviderOperation>()
        batchOperation.add(ContentProviderOperation.newInsert(
            ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
        )
        // Include phone number for contact
        batchOperation.add(ContentProviderOperation.newInsert(
            ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            .withValue(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                et_insert_number.text.toString()
            )
            .withValue(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, "1")
            .build()
        )
        // Include name for contact
        batchOperation.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            .withValue(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                et_insert_name.text.toString()
            )
            .build()
        )
        et_insert_name.text.clear()
        et_insert_number.text.clear()
        contentResolver.applyBatch(ContactsContract.AUTHORITY, batchOperation)
    }

    private fun displayEntries() { // Display all the local WordProvider words
        val stringBuilder = StringBuilder()

        val cursor = contentResolver.query(WordProvider.CONTENT_URI,
        arrayOf(WordProvider.CONTENT_PATH),
        null,
        null,
        null)?.apply {
            while (moveToNext()) {
                stringBuilder.append(
                    getString(getColumnIndexOrThrow("words")) + "\n"
                )
            }
        tv_provider_display.text = stringBuilder.toString()
        }
        if (cursor != null && !cursor.isClosed) cursor.close()
    }

    private fun displayOneEntry() { // Display one item from the local WordProvider
        val cursor = contentResolver.query(WordProvider.CONTENT_URI,
        arrayOf(WordProvider.CONTENT_PATH),
        WordProvider.WORD_ID + " = ?",
        arrayOf("0"), // Which word we want to get in list
        null)?.apply {
            while (moveToNext()) {
                tv_provider_display.text = getString(getColumnIndexOrThrow("words"))
            }
        }
        if (cursor != null && !cursor.isClosed) cursor.close()
    }

    private fun readContacts() { // Read from ContactsContract and insert into local WordProvider
        val selectionProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER)
        // SQL -> SELECT ID.DISPLAY.NUMBER FROM
        val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        selectionProjection,
        null,
        null,
        null)?.apply {
            while (moveToNext()) {
                listOfContactsRaw.add(getString(getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone._ID))
                )

                val contentValues = ContentValues()
                contentValues.put(WordProvider.CONTENT_PATH, getString(
                    getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID))
                    + " " +
                    getString(getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    + " " +
                    getString(getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                )
                // Insert each contact into Word Provider
                contentResolver.insert(
                    WordProvider.CONTENT_URI,
                    contentValues
                )
            }
            displayEntries()
        }
        if (cursor != null && !cursor.isClosed) cursor.close()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun registerNetworkCallback(context: Context) { // Register network callback once
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    isNetworkConnected = true // Sets true automatically when network available
                }

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    isNetworkConnected = !blocked // Network available when not blocked
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    internetCapable = networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET)
                    netNotCongested = networkCapabilities.hasCapability(NET_CAPABILITY_NOT_CONGESTED)
                    netNotMetered = networkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED)

                    bluetoothTransport = networkCapabilities.hasTransport(TRANSPORT_BLUETOOTH)
                    cellularTransport = networkCapabilities.hasTransport(TRANSPORT_CELLULAR)
                    wifiTransport = networkCapabilities.hasTransport(TRANSPORT_WIFI)
                }

                override fun onLost(network: Network) {
                    isNetworkConnected = false // Sets false automatically when network lost
                }

                override fun onUnavailable() {
                    isNetworkConnected = false // Sets false automatically when network unavailable
                }
            })

            isNetworkConnected = false // Set false when initially registering network callback
        } catch (e: Exception) {
            isNetworkConnected = false
        }
    }

    companion object { // Network Check (used with LiveData type application)
        // CONNECTIVITY
        var isNetworkConnected = false // Current connectivity of this device

        // CAPABILITIES
        var internetCapable = false // Indicates that this network should be able to reach the internet.
        var netNotCongested = false // Indicates that this network is not congested.
        var netNotMetered = false // If an application needs an un-metered network for a bulk transfer.

        // TRANSPORTS
        var bluetoothTransport = false // Indicates this network uses a Bluetooth transport.
        var cellularTransport = false // Indicates this network uses a Cellular transport.
        var wifiTransport = false // Indicates this network uses a Wi-Fi transport.
    }
}