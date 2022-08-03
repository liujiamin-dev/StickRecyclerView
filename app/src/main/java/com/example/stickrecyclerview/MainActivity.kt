package com.example.stickrecyclerview

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stickrecyclerview.databinding.ActivityMainBinding
import com.example.stickrecyclerview.databinding.ItemBaseBinding

class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.rv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            setSelectedItemAtCentered(true)
            adapter = DemoAdapter(this@MainActivity, binding.rv)
        }
    }

    class DemoAdapter(private val mContext:Context, private val rv: TvRecyclerView) : RecyclerView.Adapter<DemoAdapter.VH>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(mContext).inflate(R.layout.item_base, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.binding?.tv?.text = "position:${position}"
        }

        override fun getItemCount(): Int {
            return 50
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding = DataBindingUtil.bind<ItemBaseBinding>(itemView)
        }
    }
}