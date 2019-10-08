# LoopViewPager
适用于在Android4.X及以下环境实现如下效果。

---

### GIF
![gif](https://github.com/elzhao/LoopViewPager/blob/master/demo.gif)

ps：在Android4.X及以下环境使用ViewPager、RecyclerView等难以实现中间的图片显示在左右两边的前面效果，在Android5.0及以上环境通过设置View.setTranslationZ()比较容易。

---

### 添加到项目中

在项目根目录下的build.gradle里加：
```
repositories {
    maven { url "https://jitpack.io" }
}
```

在项目的app模块下的build.gradle里加：
```
dependencies {
    implementation 'com.github.elzhao:LoopViewPager:v1.0'
}
```

---

### 代码示例
XML：
```
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <com.elzhao.loopviewpager.LoopViewPager
        android:id="@+id/loopViewPager"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_centerInParent="true" />

</RelativeLayout>
```
MainActivity.java
```
public class MainActivity extends Activity implements LoopViewPager.OnPageChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private LoopViewPager mLoopViewPager;
    private SimpleAdapter mAdapter;
    private List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Lg.setModuleName(TAG);
        for (int i = 0; i < 20; i ++) {
            mDataList.add(String.valueOf(i));
        }

        mLoopViewPager = (LoopViewPager) findViewById(R.id.loopViewPager);
        mAdapter = new SimpleAdapter(this, mDataList);
        mLoopViewPager.setAdapter(mAdapter);
        mLoopViewPager.addOnPageChangeListener(this);
        mLoopViewPager.setPageTransformer(new ScaleTransfer());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoopViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onPageSelected(int position) {
        Lg.i("onPageSelected position: " + position);
    }
}
```
SimpleAdapter.java (Adapter实现方法和RecyclerView.RecyclerView.Adapter相同)
```
public class SimpleAdapter extends LoopPagerAdapter<SimpleAdapter.SimpleHolder> {

    private Context mContext;
    private List<String> mDataList;

    SimpleAdapter(Context context, List<String> dataList) {
        mContext = context;
        mDataList = dataList;
    }

    @Override
    public int getCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    @Override
    public SimpleAdapter.SimpleHolder onCreateViewHolder(ViewGroup parent, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_view, parent, false);
        return new SimpleHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleAdapter.SimpleHolder viewHolder, int position) {
        String content = mDataList.get(position);
        viewHolder.tvContent.setText(content);
    }

    @Override
    public int getItemCountInPage() {
        return 3;
    }

    class SimpleHolder extends LoopViewPager.ViewHolder{

        TextView tvContent;

        SimpleHolder(View itemView) {
            super(itemView);
            tvContent = (TextView) itemView.findViewById(R.id.tv_content);
        }
    }
}
```

---

### 其他API
###### 1、LoopPagerAdapter
- 重写getItemCountInPage方法，设置每页显示item个数；

- 客户端调用notifyDataSetChanged()更新数据;

- 重写getItemViewType方法，实现不同类型item显示;

###### 2、LoopViewPager.PageTransformer
- 实现接口可自定义滑动效果，使用方法与ViewPager.PageTransformer相同；

###### 3、LoopViewPager
- setCurrentItem(int item)，跳转到需要显示的项。
