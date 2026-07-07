package com.example.shrinkpdf.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    
    // Test Ad Unit ID for Interstitial
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    fun loadAd(activity: Activity, onAdLoaded: (Boolean) -> Unit = {}) {
        if (interstitialAd != null) {
            onAdLoaded(true)
            return
        }

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                onAdLoaded(true)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
                onAdLoaded(false)
            }
        })
    }

    fun showAd(activity: Activity, isPremium: Boolean, onAdDismissed: () -> Unit) {
        if (isPremium) {
            // User is premium, bypass ads immediately
            onAdDismissed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    // Preload next ad
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
        }
    }
}
