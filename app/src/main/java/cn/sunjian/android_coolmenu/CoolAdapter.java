package cn.sunjian.android_coolmenu;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import cn.sunjian.coolmenu.CoolMenu;

/**
 * Created by sunjian on 2017/1/22.
 */

public class CoolAdapter extends CoolMenu.Adapter<CoolAdapter.MyViewHolder> {

    private List<String> list;

    private int movePos = -1;

    private int[] mImgArr = new int[]{R.mipmap.red, R.mipmap.orange, R.mipmap.yellow, R.mipmap.green, R.mipmap.grass, R.mipmap.blue, R.mipmap.purple};

    public CoolAdapter(List<String> list) {
        this.list = list;
    }

    public void add(String add) {
        if (list == null) return;
        if (movePos < 0) {
            movePos = list.size();
        } else {
            movePos++;
        }

        list.add(add);
        int size = list.size();
        notifyItemInserted(size - 1);
    }

    public void remove() {
        if (list == null || list.isEmpty()) return;
        movePos = -1;
        int size = list.size();
        list.remove(size - 1);
        notifyItemRemoved(size - 1);
    }

    public void clear() {
        list.clear();
        movePos = -1;
        notifyDataSetChanged();
    }

    public void addList(List<String> addList) {
        list.addAll(addList);
        movePos = -1;
        notifyDataSetChanged();
    }

    public void change() {
        if (list == null || list.isEmpty()) return;
        int size = list.size();
        movePos = -1;
        list.clear();
        for (int i = 0; i < size; i++) {
            list.add("刷新" + i);
        }

        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.center_cool_menu, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        String text = list.get(position);
        holder.textView.setText(text);
        holder.textView.setBackgroundResource(mImgArr[(movePos < 0 ? position : movePos) % 7]);

        //事件无效的
//        holder.textView.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Toast.makeText(v.getContext(), "onTouch--position:" + position, Toast.LENGTH_SHORT).show();
//                return false;
//            }
//        });
//        holder.textView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(v.getContext(), "onClick---position:" + position, Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    static class MyViewHolder extends CoolMenu.ViewHolder {

        private TextView textView;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.tv);
        }
    }
}


