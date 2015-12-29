/*
 *
 *  Copyright (c) 2015 SameBits UG. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.samebits.beacon.locator.ui.activity;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.samebits.beacon.locator.BeaconLocatorApp;
import com.samebits.beacon.locator.R;
import com.samebits.beacon.locator.model.IManagedBeacon;
import com.samebits.beacon.locator.ui.fragment.DetectedBeaconsFragment;
import com.samebits.beacon.locator.ui.fragment.ScanFragment;
import com.samebits.beacon.locator.ui.fragment.ScanRadarFragment;
import com.samebits.beacon.locator.ui.fragment.TrackedBeaconsFragment;
import com.samebits.beacon.locator.util.Constants;
import com.samebits.beacon.locator.util.DialogBuilder;

import org.altbeacon.beacon.BeaconManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainNavigationActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawer;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.nav_view)
    NavigationView navigationView;

    BeaconManager mBeaconManager;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, MainNavigationActivity.class);
    }

    @OnClick(R.id.fab)
    void navAction() {

        Fragment currentFragment = getFragmentInstance(R.id.content_frame);
        String tag = currentFragment.getTag();
        switch (tag) {
            case Constants.TAG_FRAGMENT_SCAN_LIST:
            case Constants.TAG_FRAGMENT_SCAN_RADAR:
                ((ScanFragment) currentFragment).scanStartStopAction();
                break;

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);
        ButterKnife.bind(this);

        setupToolbar();

        navigationView.setNavigationItemSelectedListener(this);

        mBeaconManager = BeaconLocatorApp.from(this).getComponent().beaconManager();

        checkPermissions();
        verifyBluetooth();

        readExtras();

        if (null == savedInstanceState || mBeacon != null) {
            launchTrackedListView();
        }

    }

    private void setupToolbar() {

        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(Constants.TAG, "coarse location permission granted");
                } else {
                    final Dialog permFailedDialog = DialogBuilder.createSimpleOkErrorDialog(
                            this,
                            getString(R.string.dialog_error_functionality_limited),
                            getString(R.string.error_message_location_access_not_granted)
                    );
                    permFailedDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            //finish();
                        }
                    });
                    permFailedDialog.show();
                }
                return;
            }
        }
    }

    @TargetApi(23)
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                final Dialog permDialog = DialogBuilder.createSimpleOkErrorDialog(
                        this,
                        getString(R.string.dialog_error_need_location_access),
                        getString(R.string.error_message_location_access_need_tobe_granted)
                );
                permDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                permDialog.show();
            }
        }
    }

    @TargetApi(18)
    private void verifyBluetooth() {

        try {
            if (!mBeaconManager.checkAvailability()) {

                final Dialog bleDialog = DialogBuilder.createSimpleOkErrorDialog(
                        this,
                        getString(R.string.dialog_error_ble_not_enabled),
                        getString(R.string.error_message_please_enable_bluetooth)
                );
                bleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                bleDialog.show();

            }
        } catch (RuntimeException e) {

            final Dialog bleDialog = DialogBuilder.createSimpleOkErrorDialog(
                    this,
                    getString(R.string.dialog_error_ble_not_supported),
                    getString(R.string.error_message_bluetooth_le_not_supported)
            );
            bleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }
            });
            bleDialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation ui item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_scan_radar:
                launchRadarScanView();
                break;
            case R.id.nav_scan_around:
                launchScanBeaconView();
                break;
            case R.id.nav_settings:
                launchSettingsActivity();
                break;
            case R.id.nav_tracked_list:
                launchTrackedListView();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void addScanBeaconFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            if (checkFragmentInstance(R.id.content_frame, DetectedBeaconsFragment.class) == null) {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.content_frame, DetectedBeaconsFragment.newInstance(), Constants.TAG_FRAGMENT_SCAN_LIST)
                        .commit();
            }
        }
    }

    private void addRadarScanFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            if (checkFragmentInstance(R.id.content_frame, ScanRadarFragment.class) == null) {
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.content_frame, ScanRadarFragment.newInstance(), Constants.TAG_FRAGMENT_SCAN_RADAR)
                        .commit();
            }
        }
    }

    private void addTrackedListFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            Fragment frg = checkFragmentInstance(R.id.content_frame, TrackedBeaconsFragment.class);
            if ( frg == null) {
                TrackedBeaconsFragment tFrg = TrackedBeaconsFragment.newInstance();
                if (mBeacon != null) {
                    Bundle bundles = new Bundle();
                    bundles.putParcelable(Constants.ARG_BEACON, mBeacon);
                    tFrg.setArguments(bundles);
                }
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.content_frame, tFrg, Constants.TAG_FRAGMENT_TRACKED_BEACON_LIST)
                        .commit();
            }
        }
    }

    public void hideFab() {
        fab.setVisibility(View.GONE);
    }

    public void swappingFabAway() {
        fab.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.pop_down);
        fab.startAnimation(animation);
    }

    public void swappingFabUp() {
        fab.clearAnimation();
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.pop_up);
        fab.startAnimation(animation);
    }

    public void swappingFloatingScanIcon(boolean isScanning) {
        if (isScanning) {
            setFabIcon(R.drawable.ic_portable_wifi_off_white_24dp);
        } else {
            setFabIcon(R.drawable.ic_track_changes_white_24dp);
        }
    }

    public void swappingFloatingIcon() {
        Fragment currentFragment = getFragmentInstance(R.id.content_frame);
        String tag = currentFragment.getTag();
        switch (tag) {
            case Constants.TAG_FRAGMENT_SCAN_LIST:
            case Constants.TAG_FRAGMENT_SCAN_RADAR:
                setFabIcon(R.drawable.ic_track_changes_white_24dp);
                break;
            default:
                setFabIcon(R.drawable.ic_add_white_24dp);
        }
    }

    private void setFabIcon(final int resId) {
        fab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                fab.setImageResource(resId);
                fab.show();
            }
        });
    }

    private void launchScanBeaconView() {
        addScanBeaconFragment();
    }

    private void launchRadarScanView() {
        addRadarScanFragment();
    }

    private void launchTrackedListView() {
        addTrackedListFragment();
    }

}
