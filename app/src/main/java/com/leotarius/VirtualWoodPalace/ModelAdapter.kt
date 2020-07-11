package com.leotarius.VirtualWoodPalace

import android.graphics.Color
import android.graphics.ColorSpace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_model.view.*

const val selectedModelColor = Color.YELLOW
const val unselectedModelColor = Color.GRAY

class ModelAdapter(
    val models: List<Model>
) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>()
{
    var selectedModel = MutableLiveData<Model>()
    var selectedModelIndex: Int = 0

    inner class ModelViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_model, parent, false)
        return ModelViewHolder(view)
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {

        if(selectedModelIndex == holder.layoutPosition){
            holder.itemView.setBackgroundColor(selectedModelColor)
            selectedModel.value = models.get(selectedModelIndex)
        } else{
            holder.itemView.setBackgroundColor(unselectedModelColor)
        }

        holder.itemView.apply {
            ivThumbnail.setImageResource(models[position].imageResourceId)
            tvTitle.text = models[position].title

            setOnClickListener {
                selectModel(holder)
            }
        }
    }

    private fun selectModel(holder: ModelViewHolder){
        if(selectedModelIndex != holder.layoutPosition){
            holder.itemView.setBackgroundColor(selectedModelColor)
            notifyItemChanged(selectedModelIndex)
            selectedModelIndex = holder.layoutPosition
            selectedModel.value = models[selectedModelIndex]
        }
    }
}