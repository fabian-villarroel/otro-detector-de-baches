package com.example.detectorbaches;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location ubicacionActual;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Bitmap fotoCapturada = null;
    private List<Marker> listaMarcadoresBaches = new ArrayList<>();

    // Cambia IP_DE_TU_SERVIDOR por la IP local de tu computador (ej: 192.168.1.15)
    private static final String URL_OBTENER_BACHES = "http://192.168.100.26/baches_api/obtener_baches.php";
    private static final String URL_GUARDAR_BACHE = "http://192.168.100.26/baches_api/guardar_bache.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnTomarFoto.setOnClickListener(v -> abrirCamara());

        pedirPermisos();
    }

    private void pedirPermisos() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Centrar el mapa entre Iquique y Alto Hospicio
        LatLng iquiqueAltoHospicio = new LatLng(-20.244, -70.138);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(iquiqueAltoHospicio, 13f));

        obtenerUbicacionYVerificarAlerta();
        cargarBachesDesdeBD();

        // Al hacer clic en un punto amarillo, muestra la foto y descripción
        mMap.setOnMarkerClickListener(marker -> {
            String fotoBase64 = (String) marker.getTag();
            mostrarDetalleBache(marker.getTitle(), marker.getSnippet(), fotoBase64);
            return true;
        });
    }

    private void obtenerUbicacionYVerificarAlerta() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    ubicacionActual = location;
                    verificarBachesCercanos(location);
                }
            });
        }
    }

    private void cargarBachesDesdeBD() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, URL_OBTENER_BACHES, null,
                response -> {
                    mMap.clear();
                    listaMarcadoresBaches.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject bacheObj = response.getJSONObject(i);
                            double lat = bacheObj.getDouble("latitud");
                            double lng = bacheObj.getDouble("longitud");
                            String desc = bacheObj.getString("descripcion");
                            String foto = bacheObj.optString("foto_base64", "");

                            LatLng pos = new LatLng(lat, lng);

                            // Crear marcador con punto AMARILLO
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(pos)
                                    .title("Bache reportado")
                                    .snippet(desc)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                            if (marker != null) {
                                marker.setTag(foto);
                                listaMarcadoresBaches.add(marker);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (ubicacionActual != null) {
                        verificarBachesCercanos(ubicacionActual);
                    }
                },
                error -> Toast.makeText(this, "Error al cargar baches de MySQL", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(jsonArrayRequest);
    }

    // Alerta de proximidad si el conductor está a menos de 100 metros[cite: 1]
    private void verificarBachesCercanos(Location miUbicacion) {
        for (Marker marker : listaMarcadoresBaches) {
            Location locBache = new Location("");
            locBache.setLatitude(marker.getPosition().latitude);
            locBache.setLongitude(marker.getPosition().longitude);

            float distanciaMetros = miUbicacion.distanceTo(locBache);
            if (distanciaMetros < 100) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ ¡PRECAUCIÓN!")
                        .setMessage("Hay un bache reportado a unos " + (int) distanciaMetros + " metros de tu posición.")
                        .setPositiveButton("Entendido", null)
                        .show();
                break;
            }
        }
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Bundle extras = data.getExtras();
            fotoCapturada = (Bitmap) extras.get("data");
            pedirDescripcionYGuardar();
        }
    }

    private void pedirDescripcionYGuardar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Describir el bache");

        final EditText input = new EditText(this);
        input.setHint("Ej: Bache grande en la pista derecha");
        builder.setView(input);

        builder.setPositiveButton("Guardar reporte", (dialog, which) -> {
            String descripcion = input.getText().toString();
            if (ubicacionActual != null && fotoCapturada != null) {
                guardarBacheEnMySQL(descripcion, ubicacionActual.getLatitude(), ubicacionActual.getLongitude(), fotoCapturada);
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación o la foto", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void guardarBacheEnMySQL(String descripcion, double lat, double lng, Bitmap bitmap) {
        new Thread(() -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String fotoString = Base64.encodeToString(byteArray, Base64.DEFAULT);

            runOnUiThread(() -> {
                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_GUARDAR_BACHE,
                        response -> {
                            Toast.makeText(this, "¡Bache reportado con éxito!", Toast.LENGTH_SHORT).show();
                            cargarBachesDesdeBD(); // Recargar puntos en el mapa
                        },
                        error -> Toast.makeText(this, "Error al guardar en MySQL", Toast.LENGTH_SHORT).show()) {

                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        params.put("descripcion", descripcion);
                        params.put("latitud", String.valueOf(lat));
                        params.put("longitud", String.valueOf(lng));
                        params.put("foto_base64", fotoString);
                        return params;
                    }
                };

                Volley.newRequestQueue(this).add(stringRequest);
            });
        }).start();
    }

    private void mostrarDetalleBache(String titulo, String descripcion, String fotoBase64) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titulo);

        ImageView imageView = new ImageView(this);
        if (fotoBase64 != null && !fotoBase64.isEmpty()) {
            byte[] decodedString = Base64.decode(fotoBase64, Base64.DEFAULT);
            Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        }

        builder.setMessage(descripcion);
        builder.setView(imageView);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }
}