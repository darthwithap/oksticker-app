/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.darthwithap.okstickerapp;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class StickerPackListActivity extends AddStickerPackActivity {
    public static final String EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list";
    private static final int STICKER_PREVIEW_DISPLAY_LIMIT = 5;
    private static final String TAG = "Anlytics";
    private LinearLayoutManager packLayoutManager;
    private RecyclerView packRecyclerView;
    private StickerPackListAdapter allStickerPacksListAdapter;
    private WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    private ArrayList<StickerPack> stickerPackList;
    private AdView mAdView;
    private TextView error_message;
    private RecyclerView packRecyclerViewFirebase;
    private Tracker mTracker;
    public Analytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_list);
        error_message = findViewById(R.id.error_message);

        MobileAds.initialize(this, initializationStatus -> {});
        mAdView = findViewById(R.id.adView);
        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getString(R.string.google_admob_banner_ads_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        analytics = new Analytics(this);
        analytics.logAppStart();
        AnalyticsApplication application = new AnalyticsApplication(this);
        mTracker = application.getDefaultTracker();

        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.google_admob_native_ads_id))
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        if (isDestroyed()) {
                            unifiedNativeAd.destroy();
                            return;
                        }
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());

        packRecyclerView = findViewById(R.id.sticker_pack_list);
        packRecyclerViewFirebase = findViewById(R.id.sticker_pack_list_firebase);
        stickerPackList = getIntent().getParcelableArrayListExtra(EXTRA_STICKER_PACK_LIST_DATA);
        showStickerPackList(stickerPackList);
        error_message.setVisibility(View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
//            getSupportActionBar().setDisplayShowCustomEnabled(true);
//            getSupportActionBar().setDisplayShowTitleEnabled(false);
//            LayoutInflater inflator = LayoutInflater.from(this);
            String title = getResources().getQuantityString(R.plurals.title_activity_sticker_packs_list, stickerPackList.size());
            getSupportActionBar().setTitle(title);
            //View view = inflator.inflate(R.layout.title_view, null);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_open_whatsapp) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteListCheckAsyncTask = new WhiteListCheckAsyncTask(this);
        whiteListCheckAsyncTask.execute(stickerPackList.toArray(new StickerPack[0]));
        Log.i(TAG, "Setting screen name: " + "StickerPackList");
        mTracker.setScreenName("Image~" + "StickerPackList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    private void showStickerPackList(List<StickerPack> stickerPackList) {
        allStickerPacksListAdapter = new StickerPackListAdapter(stickerPackList, onAddButtonClickedListener, getApplicationContext());
        packRecyclerView.setAdapter(allStickerPacksListAdapter);
        packLayoutManager = new LinearLayoutManager(this);
        packLayoutManager.setOrientation(RecyclerView.VERTICAL);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                packRecyclerView.getContext(),
                packLayoutManager.getOrientation()
        );
        packRecyclerView.addItemDecoration(dividerItemDecoration);
        packRecyclerView.setLayoutManager(packLayoutManager);
        packRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::recalculateColumnCount);
    }


    private final StickerPackListAdapter.OnAddButtonClickedListener onAddButtonClickedListener = pack -> addStickerPackToWhatsApp(pack.identifier, pack.name);


    private void recalculateColumnCount() {
        final int previewSize = getResources().getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size);
        int firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition();
        StickerPackListItemViewHolder viewHolder = (StickerPackListItemViewHolder) packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
        if (viewHolder != null) {
            final int widthOfImageRow = viewHolder.imageRowView.getMeasuredWidth();
            final int max = Math.max(widthOfImageRow / previewSize, 1);
            int maxNumberOfImagesInARow = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max);
            int minMarginBetweenImages = (widthOfImageRow - maxNumberOfImagesInARow * previewSize) / (maxNumberOfImagesInARow - 1);
            allStickerPacksListAdapter.setImageRowSpec(maxNumberOfImagesInARow, minMarginBetweenImages);
        }
    }


    static class WhiteListCheckAsyncTask extends AsyncTask<StickerPack, Void, List<StickerPack>> {
        private final WeakReference<StickerPackListActivity> stickerPackListActivityWeakReference;

        WhiteListCheckAsyncTask(StickerPackListActivity stickerPackListActivity) {
            this.stickerPackListActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @Override
        protected final List<StickerPack> doInBackground(StickerPack... stickerPackArray) {
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity == null) {
                return Arrays.asList(stickerPackArray);
            }
            for (StickerPack stickerPack : stickerPackArray) {
                stickerPack.setIsWhitelisted(WhitelistCheck.isWhitelisted(stickerPackListActivity, stickerPack.identifier));
            }
            return Arrays.asList(stickerPackArray);
        }

        @Override
        protected void onPostExecute(List<StickerPack> stickerPackList) {
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity != null) {

                stickerPackListActivity.allStickerPacksListAdapter.setStickerPackList(stickerPackList);
                stickerPackListActivity.allStickerPacksListAdapter.notifyDataSetChanged();
            }
        }
    }
}
