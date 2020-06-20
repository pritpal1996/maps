package com.example.map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryDataEventListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference  myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> dangerousArea;
    private IOnLoadLocationListner listner;
    private DatabaseReference myCity;
    private Location lastLocation;
    private  GeoQuery geoQuery;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        buildLocationRequest();
                        buildL0cationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                        initArea();
                        settingGeoFire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You Must Enable Premission", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }

    private void initArea() {
        myCity=FirebaseDatabase.getInstance()
                .getReference("dabgerousarea")
                .child("My City");
        listner=this

               myCity .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MyLatLng>  latLngList=new ArrayList<>();
                        for (DataSnapshot locationSnapshot: dataSnapshot.getChildren())
                        {
                            MyLatLng latLng=locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(LatLng);
                        }
                        listner.onLoadLocationSucess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                      listner.onLoadLocationFailed(databaseError.getMessage);
                    }
                });
        myCity.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // update dangerous area list
                 List<MyLatLng>  latLngList=new ArrayList<>();
                for (DataSnapshot locationSnapshot: dataSnapshot.getChildren())
                {
                    MyLatLng latLng=locationSnapshot.getValue(MyLatLng.class);
                    latLngList.add(LatLng);
                }
                listner.onLoadLocationSuccess(LatLngList);
                dangerousArea= new ArrayList<>();
                for (MyLatLng myLatLng: latLngs)
                {
                    LatLng.convert=new LatLng(myLatLng.getLatitude(),myLatLng.getLongtude());
                    dangerousArea.add(convert);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        /**
         * this method is to submit the array  to firebase
         */
        // after submit this area , we will comment
        /*
        FirebaseDatabase .getInstance()
                .getReference("Dangreous area")
                .child("myCity")
                .setValue(dangerousArea)
                .addOnCanceledListener(new OnCompleteListner<void>)(){
            @Override
                    public void onComplete<@NonNull Task<void> task{
                Toast.makeText(MapsActivity.this, " updated", Toast.LENGTH_SHORT).show();
            } }
            .addOnFaliureListner(new OnFailureListener()){
            @Override
                    public void onFaliure(@NonNull Exception e){
                Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }
*/

    }

    private void addUserMarker() {
        geoFire.setLocation("You", new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (currentUser != null) currentUser.remove();
                currentUser = mMap.addMarker(new MarkerOptions());
                // .position(newLatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                // .title("YOU");
                // after add marker , move camera
                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(), v .12 .0f));
            }
        });
    }

    private void settingGeoFire() {
        myLocationRef= FirebaseDatabase.getInstance().getReference("My Location");
        geoFire= new GeoFire(myLocationRef);
    }

    private void buildL0cationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (mMap != null) {
                    lastLocation=locationRequest.getLastLocation();
                    addUserMarker();

                }

            }
        };
    }


    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (fusedLocationProviderClient != null)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            // Add Circle on Dangreous
          addCircleArea();

            // Create GeoQuery when user in dangerous location
             geoQuery = geoFire.queryAtLocation(new GeoLocation(LatLng.latitude,LatLng.longitude), 0.5f)//500m
            geoQuery.addGeoQueryDataEventListener(MapsActivity.this);


        }

    private void addCircleArea() {
        if (geoQuery!=null)
        {
            geoQuery.removeGeoQueryEventListener(this);
            geoQuery.removeAllListeners();
        }

        for(LatLng latLng:dangerousArea)
        {
            mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(500)
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)// 22  is transparent code
                    .strokeWidth(5.0f)
            );
    }

}

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
       // sendNotification("EDMTDev",String.format("%s entered the dangerous area", key));
       // private void sendNotification(String title,String context){

        }
    }

    @Override
    public void onDataExited(DataSnapshot dataSnapshot) {
        //sendNotification("EDMTDev",String.format("%s leave the dangerous area", key));
    }

    @Override
    public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
        //sendNotification("EDMTDev",String.format("%s move within the dangerous area", Key));

    }

    @Override
    public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, "+error.getMessage()", Toast.LENGTH_SHORT).show();
        //private void sendNotification(String title,String context){
          //  Toast.makeText(this, ""+content, Toast.LENGTH_SHORT).show();
        }
        String NOTIFICATION_CHANNEL_ID="edmt_multiple_location";

        NotificationManager notificationManager= (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                {
                    NotificationChannel notificationChannel=new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My Notification"
                    ,NotificationManager.IMPORTANCE_DEFAULT);
                    // cofig
                    notificationChannel.setDescription("Channel Description");
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(Color.RED);
                    notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
                    notificationChannel.enableVibration(true);
                    notificationManager.createNotificationChannel(notificationChannel);

                }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID);
                builder.setContentTitle(title)
                        .setContentText(content)
                        .setAutoCancel(false)
                        .setSmallIcon(R.mipmap.ic_launcher)
                         .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        Notification notification = builder.build();
                NotificationManager .notify(new Random().nextInt() notification);
    }
}
@Override
public void onLoadLocationSucess(List<MyLatLng>latLngs){
    dangerousArea= new ArrayList<>();
    for (MyLatLng myLatLng: latLngs)
    {
    LatLng.convert=new LatLng(myLatLng.getLatitude(),myLatLng.getLongtude());
    dangerousArea.add(convert);
    }
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(MapsActivity.this);
    if (mMap!=null)
    {
        mMap.clear();
        addUserMarker();


        // add user marker

        // add circle on dangerous area
        addCircleArea();
    }
}
@Override
public  void onLocationFailed(String message){
    Toast.makeText(this, ""+ message, Toast.LENGTH_SHORT).show();
}