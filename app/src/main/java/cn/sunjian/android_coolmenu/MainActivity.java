package cn.sunjian.android_coolmenu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.sunjian.coolmenu.CoolMenu;

public class MainActivity extends AppCompatActivity {

    private CoolAdapter adapter;
    private CoolMenu coolMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        coolMenu = (CoolMenu) findViewById(R.id.cool);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.add(String.format("第%d个", i));
        }
        adapter = new CoolAdapter(list);
        coolMenu.setOnItemClickListener(new CoolMenu.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, String.format("我是第%d个", position), Toast.LENGTH_SHORT).show();
            }
        });
        coolMenu.setOnItemDragListener(new CoolMenu.OnItemDragListener() {
            @Override
            public void onDragStart(View view, int position) {
                Log.d("CoolActivity", String.format("onDragStart---我是第%d个", position));
            }

            @Override
            public void onDragMove(View view, float rawX, float rawY, int position) {
                Log.d("CoolActivity", String.format("onDragMove---我是第%d个,处在 x:%f  y:%f", position, rawX, rawY));
            }

            @Override
            public void onDragEnd(View view, int position) {
                Log.d("CoolActivity", String.format("onDragEnd---我是第%d个", position));
            }
        });
        coolMenu.setOnItemFlingListener(new CoolMenu.OnItemFlingListener() {
            @Override
            public void onFlingStart() {
                Log.d("CoolActivity", "onFlingStart");
            }

            @Override
            public void onFlingEnd() {
                Log.d("CoolActivity", "onFlingEnd");
            }
        });
        coolMenu.setAdapter(adapter);
    }

    public void click1(View view) {
        adapter.add("我是新来的");
    }

    public void click2(View view) {
        adapter.remove();
    }

    public void click3(View view) {
        adapter.clear();
    }

    public void click4(View view) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            list.add(String.format("新%d", i));
        }
        adapter.addList(list);
    }

    boolean isDrag = false;

    boolean isFling = false;

    public void click5(View view) {
        coolMenu.setDrag(isDrag);
        isDrag = !isDrag;
    }

    public void click6(View view) {
        coolMenu.setFling(isFling);
        isFling = !isFling;
    }

    public void click7(View view) {
        coolMenu.show();
    }

    public void click8(View view) {
        coolMenu.dismiss();
    }

    public void click9(View view) {
        adapter.change();
    }

    public void click10(View view) {
        startActivity(new Intent(this, CoolActivity.class));
    }

}

