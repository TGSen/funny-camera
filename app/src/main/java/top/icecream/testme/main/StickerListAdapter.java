package top.icecream.testme.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import top.icecream.testme.R;
import top.icecream.testme.opengl.CameraRender;


/**
 * AUTHOR: 86417
 * DATE: 4/21/2017
 */

public class StickerListAdapter extends RecyclerView.Adapter<StickerListAdapter.ViewHolder> implements View.OnClickListener {

    private Context context;
    private List<String> stickerName;
    private List<Bitmap> stickerIcon;
    private final CameraRender cameraRender;

    public StickerListAdapter(Context context, CameraRender cameraRender) {
        super();
        this.context = context;
        this.cameraRender = cameraRender;
        String[] stickersArr = context.getResources().getStringArray(R.array.sticker);
        stickerName = Arrays.asList(stickersArr);
        stickerIcon = new LinkedList<>();
        stickerIcon.add(null);
        stickerIcon.add(BitmapFactory.decodeResource(context.getResources(), R.raw.glasses, null));
        stickerIcon.add(BitmapFactory.decodeResource(context.getResources(), R.raw.nose, null));
        stickerIcon.add(BitmapFactory.decodeResource(context.getResources(), R.raw.mustache, null));
        stickerIcon.add(BitmapFactory.decodeResource(context.getResources(), R.raw.face, null));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((TextView) holder.view.findViewById(R.id.textView)).setText(stickerName.get(position));
        ((ImageView) holder.view.findViewById(R.id.stickerIV)).setImageBitmap(stickerIcon.get(position));
        holder.view.setTag(position);
    }

    @Override
    public int getItemCount() {
        return stickerName.size();
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();
        cameraRender.selectSticker(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        View view;

        ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }
    }
}
