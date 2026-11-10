package com.example.detectorbaches;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etCorreo, etClave;
    private Button btnEnviarRegistro;

    // Cambia IP_DE_TU_SERVIDOR por la IP local de tu PC (ej. 192.168.1.15)
    private static final String URL_REGISTRO = "http://192.168.100.26/baches_api/registro.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etCorreo = findViewById(R.id.etCorreo);
        etClave = findViewById(R.id.etClave);
        btnEnviarRegistro = findViewById(R.id.btnEnviarRegistro);

        btnEnviarRegistro.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String correo = etCorreo.getText().toString().trim();
        String clave = etClave.getText().toString().trim();

        if (correo.isEmpty() || clave.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_REGISTRO,
                response -> {
                    if (response.contains("exito")) {
                        Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MapActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error en registro: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(RegisterActivity.this, "Error de red: " + error.getMessage(), Toast.LENGTH_SHORT).show()) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parametros = new HashMap<>();
                parametros.put("correo", correo);
                parametros.put("clave", clave);
                return parametros;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}