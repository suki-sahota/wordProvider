package com.example.wordprovider

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.os.Bundle
import android.provider.ContactsContract
import android.text.method.ScrollingMovementMethod
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    val REQ_PERMISSION = 9287
    private var listOfContactsRaw = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPermissionRuntime()

        btn_list.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            displayEntries()
        }

        btn_one.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            displayOneEntry()
        }

        btn_insert.setOnClickListener {
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            insertEntryToWordProvider()
        }

        btn_read_contacts.setOnClickListener {
            et_insert_name.text.clear()
            et_insert_number.text.clear()
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            readContacts()
        }

        btn_write_contacts.setOnClickListener {
            val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0) // Hide the soft keyboard
            insertAContact()
        }

        // To allow scrollable TextView
        tv_provider_display.movementMethod = ScrollingMovementMethod()
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

    private fun insertAContact() { // Does not insert into the local WordProvider
        val batchOperation
                = arrayListOf<ContentProviderOperation>()
        batchOperation.add(ContentProviderOperation.newInsert(
            ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
        )
        batchOperation.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
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
        // ------------
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
                contentResolver.insert(
                    WordProvider.CONTENT_URI,
                    contentValues
                )
            }
            displayEntries()
        }
        if (cursor != null && !cursor.isClosed) cursor.close()
    }

    private fun getPermissionRuntime() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS), REQ_PERMISSION)
    }
}