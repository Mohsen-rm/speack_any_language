package com.ajiashi.speakanylanguage.activity.welcom.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ajiashi.speakanylanguage.databinding.OnboardingActivityBinding

class OnBoardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      val  binding = OnboardingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
