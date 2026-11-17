package com.estebanmiretti.gps

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NuevoDepartamento : AppCompatActivity() {

    private var myListView: ListView? = null
    private var adapter: ArrayAdapter<String>? = null
    private var listaDeptos: ArrayList<String> = ArrayList()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_departamento)

        myListView = findViewById(R.id.listView1)
        cargarDepartamentos()

        val btnAgregarCiudad = findViewById<Button>(R.id.btnAgregarDepto)
        btnAgregarCiudad.setOnClickListener {
            val editTextValor = findViewById<EditText>(R.id.editTextValor)
            val nombreDepto = editTextValor.text.toString().trim()

            if (nombreDepto.isEmpty()) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.msg_validation),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                agregarDepartamento(nombreDepto)
                val datos = Intent().apply {
                    putExtra("variable", nombreDepto)
                }
                setResult(Activity.RESULT_OK, datos)
                finish()
            }
        }

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listaDeptos
        )
        myListView?.adapter = adapter

        myListView?.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { parent, _, position, _ ->
                val seleccionado = parent.getItemAtPosition(position) as? String
                borrarDepartamento(seleccionado)
            }
    }

    private fun cargarDepartamentos() {
        val prefs = getDefaultSharedPreferences(this)
        val jsonText = prefs.getString("deptos", null) ?: return

        try {
            val type = object : TypeToken<ArrayList<String>>() {}.type
            val cargados: ArrayList<String>? = gson.fromJson(jsonText, type)
            if (cargados != null) {
                listaDeptos = cargados
            }
        } catch (e: Exception) {
            // Respaldo por si existen datos guardados con el formato anterior separado por comas
            listaDeptos = ArrayList(
                jsonText.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            )
        }
    }

    private fun guardarCambios() {
        val prefs = getDefaultSharedPreferences(applicationContext)
        val jsonText = gson.toJson(listaDeptos)
        prefs.edit().putString("deptos", jsonText).apply()
        adapter?.notifyDataSetChanged()
    }

    private fun agregarDepartamento(depto: String) {
        if (listaDeptos.any { it.equals(depto, ignoreCase = true) }) return
        listaDeptos.add(depto)
        guardarCambios()
    }

    private fun borrarDepartamento(depto: String?): Boolean {
        if (depto == null || !listaDeptos.contains(depto)) return false

        listaDeptos.remove(depto)
        guardarCambios()

        Toast.makeText(
            applicationContext,
            "Se eliminó $depto",
            Toast.LENGTH_LONG
        ).show()

        return true
    }
}
