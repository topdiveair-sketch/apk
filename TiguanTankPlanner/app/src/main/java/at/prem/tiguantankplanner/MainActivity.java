package at.prem.tiguantankplanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 42;
    private static final String HOME = "Aggsbach Markt, Österreich";
    private EditText destinationInput, rangeInput, consumptionInput;
    private TextView locationText, resultText;
    private Location currentLocation;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(20 * getResources().getDisplayMetrics().density);
        box.setPadding(p,p,p,p);
        scroll.addView(box);

        TextView title = new TextView(this); title.setText("Tiguan TankPlaner"); title.setTextSize(28); box.addView(title);
        TextView sub = new TextView(this); sub.setText("6,0 l Diesel/100 km • Rückfahrt nach Aggsbach Markt"); box.addView(sub);

        Button locate = new Button(this); locate.setText("Aktuellen Standort ermitteln"); box.addView(locate);
        locationText = new TextView(this); locationText.setText("Standort: noch nicht ermittelt"); box.addView(locationText);

        destinationInput = new EditText(this); destinationInput.setHint("Ziel, z.B. Wien Hauptbahnhof"); box.addView(destinationInput);
        rangeInput = new EditText(this); rangeInput.setHint("Restreichweite laut Tiguan (km)"); rangeInput.setInputType(2|8192); box.addView(rangeInput);
        consumptionInput = new EditText(this); consumptionInput.setHint("Verbrauch l/100 km"); consumptionInput.setText("6.0"); consumptionInput.setInputType(2|8192); box.addView(consumptionInput);

        Button calc = new Button(this); calc.setText("Fahrt prüfen"); box.addView(calc);
        Button maps = new Button(this); maps.setText("Route in Google Maps öffnen"); box.addView(maps);
        resultText = new TextView(this); box.addView(resultText);
        setContentView(scroll);

        locate.setOnClickListener(v -> getLocation());
        calc.setOnClickListener(v -> calculate());
        maps.setOnClickListener(v -> openMaps());
    }

    private void getLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); return;
        }
        try {
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            currentLocation = loc;
            if (loc != null) locationText.setText(String.format(Locale.GERMANY,"Standort: %.5f, %.5f",loc.getLatitude(),loc.getLongitude()));
            else locationText.setText("Standort noch nicht verfügbar – GPS kurz aktiv lassen.");
        } catch (SecurityException e) { locationText.setText("Standortberechtigung fehlt."); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) getLocation();
    }

    private void calculate() {
        String dest = destinationInput.getText().toString().trim();
        if (dest.isEmpty() || rangeInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this,"Ziel und Restreichweite eingeben.",Toast.LENGTH_LONG).show(); return;
        }
        try {
            double range = Double.parseDouble(rangeInput.getText().toString().replace(',','.'));
            double cons = Double.parseDouble(consumptionInput.getText().toString().replace(',','.'));
            double litersPerKm = cons / 100.0;
            double approxUsableLiters = range * litersPerKm;
            resultText.setText(String.format(Locale.GERMANY,
                "Restreichweite: %.0f km\nVerbrauch: %.1f l/100 km\nRechnerisch verfügbar: ca. %.1f l Diesel\n\nGoogle Maps öffnet die Fahrt vom aktuellen Standort über %s und anschließend zurück nach Aggsbach Markt.\n\nHinweis: Für die wirtschaftlichste Tankentscheidung entlang der Strecke muss die App aktuelle Tankpreise und echte Routenkilometer online vergleichen.",
                range, cons, approxUsableLiters, dest));
        } catch (Exception e) {
            Toast.makeText(this,"Bitte gültige Zahlen eingeben.",Toast.LENGTH_LONG).show();
        }
    }

    private void openMaps() {
        String dest = destinationInput.getText().toString().trim();
        if (dest.isEmpty()) { Toast.makeText(this,"Ziel eingeben.",Toast.LENGTH_SHORT).show(); return; }
        try {
            String origin = currentLocation == null ? "" : String.format(Locale.US,"%.6f,%.6f",currentLocation.getLatitude(),currentLocation.getLongitude());
            String url = "https://www.google.com/maps/dir/?api=1&travelmode=driving" +
                    (origin.isEmpty()?"":"&origin="+enc(origin)) +
                    "&destination="+enc(HOME)+"&waypoints="+enc(dest);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) { Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    private static String enc(String s) throws Exception { return URLEncoder.encode(s,"UTF-8"); }
}
