package com.example.dipodbolotov

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener as OnTabSelectedListener

class MainActivity : AppCompatActivity() {

    var pageNumber = 0
    var pages = arrayOf(PageData("https://developerslife.ru/latest/"), PageData("https://developerslife.ru/hot/"), PageData("https://developerslife.ru/top/"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toggle(R.id.imageButton, false)
        loading(true);
        GlobalScope.async{
            val page = pages[pageNumber]
            loadImages(page).await()?.let { page.images.addAll(it) }
            runOnUiThread {
                loading(false);
                showImage(page, 0);
            }
        }

        findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
            pages[pageNumber].prevImage();
            showImage(pages[pageNumber]);
            toggle(R.id.imageButton, pages[pageNumber].currentPost > 0);
        }

        findViewById<ImageButton>(R.id.imageButton2).setOnClickListener {
            val page = pages[pageNumber]
            page.nextImage();
            loading(true);
            GlobalScope.async{
                if (page.images.size <= page.currentPost) {
                    loadImages(page).await()?.let { page.images.addAll(it) }
                    System.out.println("New loaded");
                }
                runOnUiThread {
                    loading(false);
                    toggle(R.id.imageButton, pages[pageNumber].currentPost > 0)
                    if (page.images.size > page.currentPost) {
                        showImage(page)
                    } else {
                        toggle(R.id.imageButton2, false)
                    }
                }
            }
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                pageNumber = tabLayout.selectedTabPosition;
                val page = pages[pageNumber]
                loading(true);
                GlobalScope.async{
                    if (page.images.size <= page.currentPost) {
                        val ims = loadImages(page).await()
                        ims?.let { page.images.addAll(it) }
                    }
                    runOnUiThread {

                        loading(false);
                        toggle(R.id.imageButton,  pages[pageNumber].currentPost > 0)

                        if (page.currentPost < page.images.size) {
                            toggle(R.id.imageButton2, true)
                            showImage(page);
                        }
                        else {
                            findViewById<ImageView>(R.id.imageView).visibility = View.INVISIBLE;
                            findViewById<TextView>(R.id.textView2).visibility = View.VISIBLE;
                            findViewById<TextView>(R.id.textView).setText("");
                            toggle(R.id.imageButton, false)
                            toggle(R.id.imageButton2, false)
                        }
                    }
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}


        })
    }


    fun toggle(id: Int, on: Boolean) {
        findViewById<ImageButton>(id).isEnabled = on;
        findViewById<ImageButton>(id).alpha = if (on) 1.0f else 0.5f;
    }

    fun loading(on: Boolean) {
        findViewById<ProgressBar>(R.id.progressBar).visibility = if (on) View.VISIBLE else View.GONE;
        findViewById<ImageView>(R.id.imageView).visibility = if (!on) View.VISIBLE else View.GONE;
    }



    fun loadImages(pd: PageData): Deferred<List<Pair<String, String>>?> {
        return GlobalScope.async {
            val url = URL(pd.url())
            var result = (JSONObject(url.readText()).optJSONArray("result")
                ?.let {
                    0.until(it.length()).map { i ->
                        it.optJSONObject(i)
                    }
                }
                ?.map {
                    it -> Pair(it["description"] as String, (it["gifURL"] as String).replaceFirst("http", "https"))
                })
                return@async result
            }
        }

    fun showImage(pd: PageData, index: Int = -1) {
        findViewById<ImageView>(R.id.imageView).visibility = View.VISIBLE;
        findViewById<TextView>(R.id.textView2).visibility = View.INVISIBLE;
        var i = if (index >= 0) index else pd.currentPost
        this@MainActivity.findViewById<TextView>(R.id.textView).setText(
            pd.images[i].first
        )
        Glide.with(this@MainActivity).load(pd.images[i].second).diskCacheStrategy(
            DiskCacheStrategy.ALL
        ).into(imageView)
    }

    class PageData {
        var currentPost = 0;
        var currentPage = 0;
        var images = ArrayList<Pair<String, String>>();
        private var url = "https://developerslife.ru/latest/"

        constructor(url: String) {
            this.url = url
        }


        fun url(page: Int = this.currentPage): String {
            return url + page + "?json=true"
        }

        fun nextImage() {
            currentPost++;
            if (currentPost % 5 == 0) {
                currentPage++;
            }
        }

        fun prevImage() {
            currentPost--;
            if (currentPost % 5 == 0) {
                currentPage = max(currentPage - 1, 0);
            }
        }
    }
}


