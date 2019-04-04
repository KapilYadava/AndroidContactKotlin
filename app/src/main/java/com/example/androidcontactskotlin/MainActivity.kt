package com.example.androidcontactskotlin

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), View.OnClickListener {


    private val PICK_CONTACT = 1
    private var uri: Uri? = null
    private val projection = arrayOf(
        ContactsContract.Data.CONTACT_ID,
        ContactsContract.CommonDataKinds.Email.HAS_PHONE_NUMBER,
        ContactsContract.Data.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Email.DATA
    )
    private val projection2 = arrayOf(
        ContactsContract.Data.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    private var contact_id: Int? = null
    private var hasNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pick.setOnClickListener(this)
        update.setOnClickListener(this)
        name.inputType = InputType.TYPE_NULL
        phone.inputType = InputType.TYPE_NULL
        email.inputType = InputType.TYPE_NULL
        update.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            uri = data?.data


            val cursor = contentResolver?.query(uri, projection, null, null, null, null)
            cursor!!.moveToFirst()

            val id = cursor.getColumnIndex(projection[0])
            val name = cursor.getColumnIndex(projection[2])
            val email = cursor.getColumnIndex(projection[3])
            this.name.text = Editable.Factory.getInstance().newEditable(cursor.getString(name))
            this.email.text = Editable.Factory.getInstance().newEditable(cursor.getString(email))
            contact_id = cursor.getInt(id)
            hasNumber = cursor.getString(cursor.getColumnIndex(projection[1]))

            if (hasNumber.equals("1")) {
                val where = ContactsContract.Data.CONTACT_ID + " = ?"
                val cursorPhone = contentResolver?.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection2,
                    where,
                    arrayOf(contact_id.toString()),
                    null,
                    null
                )
                cursorPhone!!.moveToFirst()
                this.phone.text = Editable.Factory.getInstance()
                    .newEditable(cursorPhone.getString(cursorPhone.getColumnIndex(projection2[1])))
            }

            update.isEnabled = true
            this.name.inputType = InputType.TYPE_CLASS_TEXT
            phone.inputType = InputType.TYPE_CLASS_PHONE
            this.email.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
    }

    override fun onClick(view: View?) {

        when (view) {
            update -> {
                updateContact()
            }
            else -> {

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS),
                        2
                    )
                } else {
                    var intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI)
                    startActivityForResult(intent, PICK_CONTACT)
                }
            }
        }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            2 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    updateContact()
                } else {
                    Toast.makeText(
                        this,
                        "Until you grant the permission, we can\'t display the names",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }

    }

    private fun updateContact() {

        if (name.text.isEmpty() || email.text.isEmpty() || phone.text.isEmpty()) {
            Utility().showToast(this, "All fields are mandatory")
            return
        }

        val where = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"
        val phoneParam: Array<String> =
            arrayOf<String>(contact_id.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        val nameParam: Array<String> =
            arrayOf<String>(contact_id.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        val emailParam: Array<String> =
            arrayOf<String>(contact_id.toString(), ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)

        var list = ArrayList<ContentProviderOperation>()
        list.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(where, phoneParam)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.text.toString())
                .build()
        )
        list.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(where, emailParam)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, email.text.toString())
                .build()
        )
        list.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(where, nameParam)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name.text.toString())
                .build()
        )

        contentResolver.applyBatch(ContactsContract.AUTHORITY, list)
        Toast.makeText(this, "Contact Updated Successfully!", Toast.LENGTH_LONG).show()
        email.text.clear()
        phone.text.clear()
        name.text.clear()
        name.inputType = InputType.TYPE_NULL
        phone.inputType = InputType.TYPE_NULL
        email.inputType = InputType.TYPE_NULL
        update.isEnabled = false
    }
}
