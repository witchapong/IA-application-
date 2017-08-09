package com.indooratlas.android.sdk.examples.mapsoverlay;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.examples.R;
import com.indooratlas.android.sdk.examples.SdkExample;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SdkExample(description = R.string.example_googlemaps_overlay_description)
public class MapsOverlayActivity extends FragmentActivity implements  GoogleMap.OnMapClickListener,
GoogleMap.OnMarkerClickListener, OnMapReadyCallback,GoogleMap.OnMarkerDragListener{

    private static final String TAG = "IndoorAtlasExample";

    private static final float HUE_IABLUE = 200.0f;

    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mMarker;
    private Marker clickedMarker;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    private IAResourceManager mResourceManager;
    private IATask<IAFloorPlan> mFetchFloorPlanTask;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location
    private LatLng mIALatLon;
    private TextView errorDisplay;
    private Button startButton;
    private Button addWaypoint;
    private boolean addWaypointState = false;
    private boolean startState = false;
    private Map<LatLng,Marker> markerMap = new HashMap<LatLng, Marker>();
    private List<ErrorLog> errorLogList = new ArrayList<ErrorLog>();
    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

            Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mIALatLon = latLng;

            if (mMarker == null) {
                // first location, add marker
                mMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(HUE_IABLUE)));
            } else {
                // move existing markers position to received location
                mMarker.setPosition(latLng);
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                final String newId = region.getId();
                // Are we entering a new floor plan or coming back the floor plan we just left?
                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlan(newId);
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }
            }
            showInfo("Enter " + (region.getType() == IARegion.TYPE_VENUE
                    ? "VENUE "
                    : "FLOOR_PLAN ") + region.getId());
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mGroundOverlay != null) {
                // Indicate we left this floor plan but leave it there for reference
                // If we enter another floor plan, this one will be removed and another one loaded
                mGroundOverlay.setTransparency(0.5f);
            }
            showInfo("Enter " + (region.getType() == IARegion.TYPE_VENUE
                    ? "VENUE "
                    : "FLOOR_PLAN ") + region.getId());
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_test);

        errorDisplay = (TextView) findViewById(R.id.textView);
        errorDisplay.setTextColor(Color.RED);

        startButton = (Button) findViewById(R.id.button1);
        addWaypoint = (Button) findViewById(R.id.button2);

        startButton.setBackgroundColor(Color.BLUE);
        addWaypoint.setBackgroundColor(Color.BLUE);

        addWaypoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                addWaypointState = !addWaypointState;
                if(addWaypointState){
                    addWaypoint.setBackgroundColor(Color.RED);
                }
                else{
                    addWaypoint.setBackgroundColor(Color.BLUE);
                }

            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                startState = !startState;
                if(startState){
                    startButton.setBackgroundColor(Color.RED);
                    startButton.setText(String.format("Stop"));
                }
                else{
                    startButton.setBackgroundColor(Color.BLUE);
                    startButton.setText(String.format("Start"));
                }

            }
        });

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // instantiate IALocationManager and IAResourceManager
        mIALocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remember to clean up after ourselves
        mIALocationManager.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }


    /**
     * Sets bitmap of floor plan as ground overlay on Google Maps
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();

        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                            + bitmap.getHeight());
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // N/A
                }

                @Override
                public void onBitmapFailed(Drawable placeHolderDraweble) {
                    showInfo("Failed to load bitmap");
                    mOverlayFloorPlan = null;
                }
            };
        }

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }


    /**
     * Fetches floor plan data from IndoorAtlas server.
     */
    private void fetchFloorPlan(String id) {

        // if there is already running task, cancel it
        cancelPendingNetworkCalls();

        final IATask<IAFloorPlan> task = mResourceManager.fetchFloorPlanWithId(id);

        task.setCallback(new IAResultCallback<IAFloorPlan>() {

            @Override
            public void onResult(IAResult<IAFloorPlan> result) {

                if (result.isSuccess() && result.getResult() != null) {
                    // retrieve bitmap for this floor plan metadata
                    fetchFloorPlanBitmap(result.getResult());
                } else {
                    // ignore errors if this task was already canceled
                    if (!task.isCancelled()) {
                        // do something with error
                        showInfo("Loading floor plan failed: " + result.getError());
                        mOverlayFloorPlan = null;
                    }
                }
            }
        }, Looper.getMainLooper()); // deliver callbacks using main looper

        // keep reference to task so that it can be canceled if needed
        mFetchFloorPlanTask = task;

    }

    /**
     * Helper method to cancel current task if any.
     */
    private void cancelPendingNetworkCalls() {
        if (mFetchFloorPlanTask != null && !mFetchFloorPlanTask.isCancelled()) {
            mFetchFloorPlanTask.cancel();
        }
    }

    private void showInfo(String text) {
        final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.button_close, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    @Override
    public void onMapClick(LatLng point) {

        if(addWaypointState){

            markerMap.put(point,mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .draggable(true)
            ));

            addWaypoint.setBackgroundColor(Color.BLUE);
            addWaypointState = false;

        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if(!startState) {

            if (markerMap.get(marker.getPosition()) != null) {
                markerMap.get(marker.getPosition()).remove();
                markerMap.remove(marker.getPosition());

            }

        }else{


                //errorLogList.add(new ErrorLog(mIALatLon,marker.getPosition()));
                //Log.i(TAG,"Error time stamp is " + errorLogList.get(0));

                Location locationA = new Location("point A");
                locationA.setLatitude(mIALatLon.latitude);
                locationA.setLongitude(mIALatLon.longitude);

                Location locationB = new Location("point B");
                locationB.setLatitude(marker.getPosition().latitude);
                locationB.setLongitude(marker.getPosition().longitude);

                float error = locationA.distanceTo(locationB);
                errorDisplay.setText(String.format("Error = %f m", error));


        }


        Log.i(TAG,"markerMap size = " + markerMap.size());

        return false;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


            // Try to obtain the map from the SupportMapFragment.
        mMap = googleMap;

            //mMap.setMyLocationEnabled(true);
        boolean success = mMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }

        mMap.setOnMapClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setIndoorEnabled(false);

    }

    @Override
    public void onMarkerDragStart(Marker marker) {

        Log.i(TAG,"Marker drag started!!!");

    }

    @Override
    public void onMarkerDrag(Marker marker) {

        Log.i(TAG,"Marker dragged!!!");

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

        Log.i(TAG,"Marker dragged end!!!");

    }
}
