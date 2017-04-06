# Android-CoolMenu

Gradle依赖

    compile 'com.crazysunj:coolmenu:1.0.0'
  
## 控件的四大特点

1. 可旋转：支持普通旋转和惯性旋转，主要是对触摸事件的分析，本控件没有用GestureDetector，条件纯手工，这也方便学习，这也是控件发布的主要原因
2. 可拖拽：支持长按拖拽，当然这也是触摸事件，我觉得比较精妙的地方是对数据的处理
3. 炫酷的动画：大多数动画在该控件都有涉及，例如拖拽结束，显示，隐藏和布局改动时
4. 灵活性：相信大家都用过RecyclerView，用过的都说好，哈哈，我第一次想到它是因为LayoutManager这个类，大家可以随意改动自己菜单的布局，多炫酷啊！可惜后来写着写着，就忘了，有空我会想想怎么实现，当然大家都可以提意见，剩下的还剩它的Adapter，你也可以根据type提供不同的view，但是我去掉了它的回收机制，我想这个菜单应该不需要大量的回收吧(当然有些地方还是需要做的，确实做得不够好)，除了用适配器，用户还可以直接在xml中添加子view，但两者不兼容

那它到底长什么样呢？看看炫酷的马赛克吧！网不好的同学，还是下载一个自己玩玩吧！

![](/picture/img_coolmenu.gif)

关于适配器方面只提供以下3个刷新方法，因为我想菜单的数量并不是太多，太多实在太丑了，但我并没有做数量的限制，这个由用户自己决定。

```
public final void notifyDataSetChanged() {

    mObservable.notifyChanged();
}

public final void notifyItemInserted(int position) {
    mObservable.notifyItemInserted(position);
}

public final void notifyItemRemoved(int position) {
    mObservable.notifyItemRemoved(position);
}
```

接口提供

```
public interface OnItemClickListener {

    void onItemClick(View view, int position);
}

public interface OnItemDragListener {

    void onDragStart(View view, int position);

    void onDragMove(View view, float rawX, float rawY, int position);

    void onDragEnd(View view, int position);
}

public interface OnItemFlingListener {

    void onFlingStart();

    void onFlingEnd();
}
```

属性提供

	<attr name="centerLayout" /> //菜单中间布局

	<attr name="isDrag" />	//可拖拽

	<attr name="isFling" />	 //可惯性滑动

博客地址：[http://crazysunj.com/](http://crazysunj.com/)


### License

> ```
> Copyright 2016 Sun Jian
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
> ```
