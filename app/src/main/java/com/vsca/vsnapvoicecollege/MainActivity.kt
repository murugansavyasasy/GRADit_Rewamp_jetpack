package com.vsca.vsnapvoicecollege

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.vsca.vsnapvoicecollege.ui.navigation.AppNavHost
import com.vsca.vsnapvoicecollege.ui.theme.GRADit_RewampTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fully transparent system bars so each screen's own background shows
        // through them. Icon contrast is set per screen via SystemBarIcons().
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            GRADit_RewampTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
