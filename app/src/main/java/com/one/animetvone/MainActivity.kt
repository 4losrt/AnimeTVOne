package com.one.animetvone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import com.one.animetvone.data.repository.FavoritesManager
import com.one.animetvone.data.source.AnimeListDiskCache
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.data.source.HistoryManager
import com.one.animetvone.ui.navigation.AppNavigation
import com.one.animetvone.ui.theme.AnimetvoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Lazy: must not touch the provider before CoverImageFetcher.init() etc.
    // run in onCreate(), otherwise the first sync/prefetch would run with a
    // null appContext and disk cover cache would be silently skipped.
    private val animeRepository: com.one.animetvone.data.repository.AnimeRepository by lazy {
        AnimeRepositoryProvider.instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoverImageFetcher.init(applicationContext)
        FavoritesManager.init(applicationContext)
        HistoryManager.init(applicationContext)
        AnimeListDiskCache.init(applicationContext)

        // Configure Coil image loader with memory and disk cache for fast poster loading on TV
        val imageLoader = ImageLoader.Builder(applicationContext)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100 MB disk cache for covers
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        // Warm up and cache animelist.json at startup on Dispatchers.IO for 0ms tab switching
        lifecycleScope.launch(Dispatchers.IO) {
            animeRepository.warmUp()
        }

        enableEdgeToEdge()
        setContent {
            AnimetvoneTheme {
                MainTvApp()
            }
        }
    }
}

@Composable
fun MainTvApp(
    modifier: Modifier = Modifier
) {
    AppNavigation(modifier = modifier)
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun MainTvAppPreview() {
    AnimetvoneTheme {
        MainTvApp()
    }
}
