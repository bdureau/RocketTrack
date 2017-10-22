/*
 * Copyright (C) 2013 François Girard
 * 
 * This file is part of Rocket Finder.
 *
 * Rocket Finder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Rocket Finder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Rocket Finder. If not, see <http://www.gnu.org/licenses/>.*/

package org.rockettrack;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapFragment extends RocketTrackBaseFragment implements OnMapReadyCallback {

	private final static String TAG ="MapFragment";


	// Map reference
	private GoogleMap mMap=null;
	private MapView mapView;
	//private SupportMapFragment mapFragment;
	private View MyView;
	private Bundle MyBundle;

	// Map markers
	private Marker rocketMarker;
	private Circle rocketCircle;
	private Polyline rocketLine;
	private Polyline rocketPath;

	private boolean followMe = false;

	public MapFragment() {
		super();
	}
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			setUpMap();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.map_view, null, false);

		ToggleButton chkFollowMe = (ToggleButton) v.findViewById(R.id.chkFollowMe);
		followMe = chkFollowMe.isChecked();
		chkFollowMe.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				followMe = arg1;
			}

		});
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			 //mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
			//mapFragment.getMapAsync(this);
			//mapFragment.ge
		}
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if(mMap == null) {
			super.onViewCreated(view, savedInstanceState);
			mapView = (MapView) view.findViewById(R.id.map);
			mapView.onCreate(savedInstanceState);
			mapView.onResume();
			mapView.getMapAsync(this);
			MyView =view;
			MyBundle = savedInstanceState;
		}

	}

	/**
	 * When the map is not ready the CameraUpdateFactory cannot be used. This should be called on
	 * all entry points that call methods on the Google Maps API.
	 */
	private boolean checkReady() {
		if (mMap == null) {
			//Toast.makeText(this, "map_not_ready", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	@Override
	public void onResume() {
		Log.d(TAG,"+++ onResume +++");
		super.onResume();
		setUpMapIfNeeded();
		// Force old markers to disappear
		if(checkReady()) {
			mMap.clear();
			// Redraw rocket location.
			onRocketLocationChange();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// Clear out the map markers so they get recreated onResume.
		rocketMarker = null;
		rocketCircle = null;
		rocketLine = null;
		rocketPath = null;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView(); 
		// Stackoverflow says this is necessary, but it seems to cause some troubles too.
		// It basically causes the map to be recreated with the view.  Look into the xml configuration
		// flags for the  SupportMapFragment object itself.
		/*Fragment fragment = (getFragmentManager().findFragmentById(R.id.map));
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		ft.remove(fragment);
		ft.commit();*/
		mMap = null;
	}



	@Override
	public void onMyLocationChange() {
		Log.d(TAG,"onLocationChagned");

		Location location = getMyLocation();
		if(mMap == null)
			return;

		LatLng myPosition = new LatLng(location.getLatitude(),location.getLongitude());

		Location rocketLocation = getRocketLocation();
		if( rocketLocation != null ){
			LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());
			updateRocketLine(location,rocketPosition);
			updateBearing();
		}

		if ( followMe ) {
			float heading = getAzimuth();
			//Toast.makeText(getActivity(), "In follow me1", Toast.LENGTH_SHORT).show();

			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).target(myPosition).bearing(heading).zoom(20).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}

	}


	@Override
	protected void onCompassChange() {
		//Log.d(TAG,"onCompassChange");
		updateBearing();
	}

	private void setUpMapIfNeeded() {

		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mapView = (MapView) MyView.findViewById(R.id.map);
			mapView.onCreate(MyBundle);
			mapView.onResume();
			mapView.getMapAsync(this);

			// Try to obtain the map from the SupportMapFragment.
			/*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			mapFragment.getMapAsync(this);*/

		//	mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}

	}

	private void setUpMap() {

		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		mMap.setMyLocationEnabled(true);
		UiSettings mUiSettings = mMap.getUiSettings();
		mUiSettings.setCompassEnabled(true);
		mUiSettings.setMyLocationButtonEnabled(true);
		mUiSettings.setZoomControlsEnabled(true);

	}

	private void  updateBearing() {
		if ( mMap == null ) {
			return;
		}
		float heading = getAzimuth();
		//Toast.makeText(getActivity(), "Heading: " +heading, Toast.LENGTH_SHORT).show();
		if ( followMe ) {
			//Toast.makeText(getActivity(), "In follow me2", Toast.LENGTH_SHORT).show();

			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).bearing(heading).zoom(20).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}

	}


	@Override
	protected void onRocketLocationChange() {
		if (mMap == null ) {
			return;
		}

		Location rocketLocation = getRocketLocation();

		if(rocketLocation == null)
			return;

		LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());

		//Draw marker at Rocket position
		if(rocketMarker == null) {
			BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
			rocketMarker = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(rocketPosition).icon(rocketIcon));
		} else {
			rocketMarker.setPosition(rocketPosition);
		}
		if ( rocketCircle == null ) {
			CircleOptions options = new CircleOptions()
			.center( rocketPosition )
			.radius( rocketLocation.getAccuracy() )
			.strokeColor(Color.RED)
			.strokeWidth(1.0f)
			.fillColor(Color.RED & 0x22FFFFFF);
			rocketCircle = mMap.addCircle(options);
		} else {
			rocketCircle.setCenter(rocketPosition);
			rocketCircle.setRadius( rocketLocation.getAccuracy() );
		}

		updateBearing();

		List<Location> rocketLocationHistory = getRocketLocationHistory();
		if ( rocketLocationHistory != null && rocketLocationHistory.size() > 0 ) {
			List<LatLng> rocketPosList = new ArrayList<LatLng>(rocketLocationHistory.size());
			for( Location l : rocketLocationHistory ) {
				rocketPosList.add( new LatLng(l.getLatitude(), l.getLongitude()));
			}
			if ( rocketPath == null ) {
				rocketPath = mMap.addPolyline( new PolylineOptions().width(1.0f).color(Color.rgb(0,0,128)));
			}
			rocketPath.setPoints(rocketPosList);
		}

		//Draw line between myPosition and Rocket
		updateRocketLine(rocketLocation,rocketPosition);
	}

	private void updateRocketLine(Location rocketLocation,LatLng rocketPosition) {
		if(getMyLocation() == null || rocketPosition == null)
			return;
		Location myLocation = getMyLocation();
		LatLng myPosition = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());

		//Draw line between myPosition and Rocket

		if (rocketLine == null) {
			rocketLine = mMap.addPolyline(new PolylineOptions()
			.add(myPosition,rocketPosition)
			.width(1.0f)
			.color(Color.WHITE));
		} else {
			List<LatLng> positionList = new ArrayList<LatLng>();
			positionList.add(myPosition);
			positionList.add(rocketPosition);
			rocketLine.setPoints(positionList);
		}
	}

}