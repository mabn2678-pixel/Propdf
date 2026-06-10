package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainLayout
import com.example.ui.PdfViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate the main PDF reader ViewModel with the application level context
        val viewModel = PdfViewModel(applicationContext)
        
        setContent {
            MainLayout(viewModel = viewModel)
        }
    }
}
