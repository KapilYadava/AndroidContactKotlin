package com.example.androidcontactskotlin

import android.content.Context
import android.widget.Toast

class Utility {

    fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}