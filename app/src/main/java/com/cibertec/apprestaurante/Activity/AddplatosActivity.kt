package com.cibertec.apprestaurante.Activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner


import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cibertec.apprestaurante.Categoria.CategoriaFirestore
import com.cibertec.apprestaurante.Categoria.CategoriaViewModel
import com.cibertec.apprestaurante.Plato.ProductViewModel

import com.cibertec.apprestaurante.R
import com.cibertec.apprestaurante.database.Categoria
import com.cibertec.apprestaurante.database.Plato
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class AddplatosActivity:  AppCompatActivity() {

    private lateinit var categoria: String
    private lateinit var platoImageUri: Uri

    private lateinit var platoViewModel: ProductViewModel
    private lateinit var categiriaViewModel: CategoriaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_platos)

        platoViewModel = run {
            ViewModelProvider(this)[ProductViewModel::class.java]
        }

        categiriaViewModel = run {
            ViewModelProvider(this)[CategoriaViewModel::class.java]
        }

        categiriaViewModel.getCategoriasFirebase()
        val spinner: Spinner = findViewById(R.id.editTextCategory)
        var arrayCategorias = emptyArray<String>()

        categiriaViewModel.listCategoriasMutable.observe(this) { categorias ->
            arrayCategorias = categorias.map { it.nombre }.toTypedArray()
            val adaptador: ArrayAdapter<String> = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                arrayCategorias
            )
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adaptador
            val btnInsertarImagen = findViewById<ImageButton>(R.id.editTextInsertarImagen)
            btnInsertarImagen.setOnClickListener {
                seleccionarImagenDesdeGaleria()
            }

            val btnGuardadPlato = findViewById<ImageButton>(R.id.btnguardarplato)
            btnGuardadPlato.setOnClickListener {
                guardarPlatoConImagen()
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (arrayCategorias.isNotEmpty()) {
                        categoria = arrayCategorias[position]
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Implementación si es necesario
                }
            }
        }
    }
    fun seleccionarImagenDesdeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }
    private fun guardarPlatoConImagen() {
        val db = FirebaseFirestore.getInstance()

        val edtNombre = findViewById<EditText>(R.id.editTextName)
        val nombre = edtNombre.text.toString()
        val edtDescripcion = findViewById<EditText>(R.id.editTextDescription)
        val descripcion = edtDescripcion.text.toString()
        val edtPrecio = findViewById<EditText>(R.id.editTextPrecio)
        val precio = edtPrecio.text.toString()

        if (::categoria.isInitialized && ::platoImageUri.isInitialized) {
            val storageReference: StorageReference = FirebaseStorage.getInstance()
                .getReference("imagenes_platos/$nombre.jpg")

            storageReference.putFile(platoImageUri)
                .addOnSuccessListener {
                    storageReference.downloadUrl
                        .addOnSuccessListener { uri ->
                            val imagenUrl = uri.toString()
                            val plato = hashMapOf(
                                "descripcion" to descripcion,
                                "imagen" to imagenUrl,
                                "nombre_produc" to nombre,
                                "precio" to precio

                            )

                            db.collection("categoria")
                                .document(categoria)
                                .update("productos", FieldValue.arrayUnion(plato))
                                .addOnSuccessListener {

                                    showBasicAlertDialog(
                                        this,
                                        "Se agregó el plato exitosamente",
                                        "Nombre: $nombre\nCategoría: $categoria\nDescripción: $descripcion\nPrecio: S/$precio"
                                    )
                                    edtNombre.setText("")
                                    edtDescripcion.setText("")
                                    edtPrecio.setText("")
                                }
                                .addOnFailureListener { e ->

                                    showBasicAlertDialog(this, "Error al agregar el plato", e.message ?: "")
                                }
                        }
                }
                .addOnFailureListener { e ->
                    showBasicAlertDialog(this, "Error al subir la imagen", e.message ?: "")
                }
        } else {
            showBasicAlertDialog(this, "Selecciona una categoría y una imagen antes de guardar", "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                platoImageUri = data.data!!
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun showBasicAlertDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }
    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }
}