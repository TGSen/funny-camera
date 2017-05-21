package top.icecream.testme.main;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import top.icecream.testme.R;


/**
 * AUTHOR: 86417
 * DATE: 4/21/2017
 */

public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.ViewHolder> implements View.OnClickListener {

    private Context context;
    private List<String> filters;

    public FilterListAdapter(Context context) {
        super();
        this.context = context;
        String[] filtersArr = context.getResources().getStringArray(R.array.filter);
        filters = Arrays.asList(filtersArr);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_filter_list, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((TextView) holder.view.findViewById(R.id.textView)).setText(filters.get(position));
        holder.view.setTag(position);
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    @Override
    public void onClick(View v) {
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        View view;

        ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }
    }
}
