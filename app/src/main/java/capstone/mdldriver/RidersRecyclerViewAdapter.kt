package capstone.mdldriver

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.rider_row_item.view.nameTextView


class RidersRecyclerViewAdapter(var riderList: List<Rider>) : RecyclerView.Adapter<RidersRecyclerViewAdapter.ViewHolder>() {

    fun updateRiders(newRiderList: List<Rider>) {
        riderList = newRiderList
        notifyDataSetChanged()
    }

    //this method is returning the view for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RidersRecyclerViewAdapter.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.rider_row_item, parent, false)
        return ViewHolder(v)
    }

    //this method is binding the data on the list
    override fun onBindViewHolder(holder: RidersRecyclerViewAdapter.ViewHolder, position: Int) {
        holder.bindItems(riderList[position])
    }

    //this method is giving the size of the list
    override fun getItemCount() = riderList.size

    //the class is hodling the list view
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindItems(rider: Rider) {
            itemView.nameTextView.text = rider._id
        }
    }
}