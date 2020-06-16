package com.swufe.myweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class WeatherList extends AppCompatActivity implements Runnable {
    ArrayAdapter<String> adapter;
    Spinner spinner;
    TextView dateTom;
    ListView listView;
    Handler handler;
    String  hrefStr = "http://www.weather.com.cn/textFC/beijing.shtml";


    String TAG = "WeatherList";
    List<HashMap<String, String>> proList;
    List<HashMap<String, String>> weatherList = new ArrayList<>();
    private SimpleAdapter listItemAdapter;
    private SimpleAdapter listItemAdapter2;
    String todayStr;
    String dateStr;
    String tomorrow;
    Boolean update;
    SharedPreferences sp;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint({"HandlerLeak", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);

        spinner = findViewById(R.id.provinceSpinner);
        dateTom = findViewById(R.id.todayDate);

        listView = findViewById(R.id.tempListView);
        proList = new ArrayList<>();
        sp = getSharedPreferences("mydata", Activity.MODE_PRIVATE);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        todayStr = sdf.format(Calendar.getInstance().getTime());

//        Calendar tom =Calendar.getInstance();
//        tom.add(Calendar.DATE,1);
//        final String tomorrow = sdf.format(tom.getTime());



//        Thread t = new Thread(this);
//        t.start();
        handler = new Handler(){//用于获取其他线程中的消息
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void handleMessage(@NonNull Message msg) {//获得数据队列
                super.handleMessage(msg);
                if(msg.what == 3){
                    tomorrow = (String) msg.obj;
                    Log.i(TAG,"onCreate:handler3:获得天气日期:"+tomorrow);
//                    dateTom.setText(tomorrow);
                }
                if(msg.what == 1){//判断数据是哪个线程返回的
                    proList = (List<HashMap<String, String>>)msg.obj;
                    Log.i(TAG,"onCreate:handler1:主线程获得省份数据");
//                    listItemAdapter= new SimpleAdapter(WeatherList.this, proList,//数据源
//                            R.layout.activity_province_list,//布局实现
//                            new String[] {"province","href"},
//                            new int[]{R.id.province,R.id.proURL}
//                    );
//                    spinner.setAdapter(listItemAdapter);
//                    Log.i(TAG,"onCreate:展示省份列表");



                    //保存list数据及更新日期
                    Gson gson = new Gson();
                    String json = gson.toJson(proList);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("date",todayStr);
                    editor.putString("data",json);
                    editor.putString("tomorrow",tomorrow);

                    editor.apply();
                    Log.i(TAG, "onCreate:handler1:sp保存省份及更新日期");
                }
                if(msg.what == 2){
                    weatherList = (List<HashMap<String, String>>)msg.obj;
                    Log.i(TAG,"onCreate:handler2:主线程获得天气数据");
//                    for(HashMap<String,String> map:weatherList){
//                        Log.i(TAG, "获得："+Objects.requireNonNull(map.get("area")));
//                        //Log.i(TAG, Objects.requireNonNull(map.get("href")));
//                    }
                    listItemAdapter2= new SimpleAdapter(WeatherList.this, weatherList,//数据源
                            R.layout.activity_weather_item,//布局实现
                            new String[] {"area","weatherStr","href"},
                            new int[]{R.id.area,R.id.weather,R.id.areaHref}
                    );
                    listView.setAdapter(listItemAdapter2);
                    Log.i(TAG,"onCreate:展示天气列表");

                }

            }

        };

        //获取sp保存的list数据至proList

        dateStr = sp.getString("date", "");
        update = !todayStr.equals(dateStr);
        //Log.i(TAG, "onCreatedata:"+data);
        Log.i(TAG, "onCreate:今日日期："+todayStr+"，上次更新日期："+dateStr+"，是否更新："+update);
        tomorrow = sp.getString("tomorrow","");
        dateTom.setText("明日天气  "+tomorrow);
        Log.i(TAG, "onCreate:明天天气："+tomorrow);

        Log.i(TAG, "onCreate:获取sp中省份数据...");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<HashMap<String,String>>>() {
        }.getType();
        String data = sp.getString("data", "");
        proList = gson.fromJson(data, listType);
        if(proList !=null){
            Log.i(TAG,"onCreate:已获取sp省份数据");

            //在布局中展示list数据
            listItemAdapter= new SimpleAdapter(WeatherList.this, proList,//数据源
                    R.layout.activity_province_list,//布局实现
                    new String[] {"province","href"},
                    new int[]{R.id.province,R.id.proURL}
            );
            spinner.setAdapter(listItemAdapter);
            Log.i(TAG,"onCreate:展示省份列表");
        }
        //Log.i(TAG, "proList！=null");
//            for(HashMap<String,String> map:proList){
//                //Log.i(TAG, Objects.requireNonNull(map.get("province")));
//                //Log.i(TAG, Objects.requireNonNull(map.get("href")));//
//            }



        //选择省份
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView province= view.findViewById(R.id.province);
                TextView href= view.findViewById(R.id.proURL);
                String provinceStr = String.valueOf(province.getText());
                hrefStr = String.valueOf(href.getText());
                Log.i(TAG,"onCreate:onItemSelected:点击省份："+provinceStr);
                //Log.i(TAG,"onCreate:listView:onItemSelected:链接："+hrefStr);
                Log.i(TAG,"onCreate:onItemSelected:开启子线程...");
                Thread t = new Thread(WeatherList.this);
                t.start();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView area= view.findViewById(R.id.area);
                TextView href= view.findViewById(R.id.areaHref);
                String hrefStr = String.valueOf(href.getText());
                Log.i(TAG,"onCreate:onItemClick:点击地区："+ area.getText());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(hrefStr));
                startActivity(intent);
                Log.i(TAG,"onCreate:onItemClick:打开链接："+hrefStr);
            }
        });

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        Log.i(TAG, "run:子线程...");
        Document doc = null;
        try {
            doc = Jsoup.connect(hrefStr).get();//从网页中获得doc对象
            Log.i(TAG,"getWeather:打开页面:"+ doc.title());//获得body的title

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(update) {
            //getTomorrowDate
            assert doc != null;
            String date = doc.getElementsByTag("ul").get(8).getElementsByTag("li").get(1).text();
            Message msg3 = handler.obtainMessage(3);
            msg3.obj = date;//编辑msg内容
            handler.sendMessage(msg3);//将msg发送至消息队列
            Log.i(TAG,"getTomorrowDate::"+ "天气日期"+date+"发送至主线程,what=3");
            //getProvince;
            Log.i(TAG, "run:getProvince...");
            List<HashMap<String, String>> provinecList = WeathterMethod.getProvince(doc);
            //获取Msg对象，用于返回主线程
            Message msg1 = handler.obtainMessage(1);//标识what用于massage
            msg1.obj = provinecList;//编辑msg内容
            handler.sendMessage(msg1);//将msg发送至消息队列
            Log.i(TAG, "getProvince:省份数据发送至主线程，what=1");


        }

        //getWeather;
        Log.i(TAG, "run:getWeather...");
        assert doc != null;
        List<HashMap<String, String>> weather=WeathterMethod.getWeather(doc);
//        for(HashMap<String,String> map:weather){
//            Log.i(TAG,"weather:"+ Objects.requireNonNull(map.get("area")));
//            Log.i(TAG,"weather:"+ Objects.requireNonNull(map.get("href")));
//        }

        //获取Msg对象，用于返回主线程
        Message msg2 = handler.obtainMessage(2);//标识what用于massage
        msg2.obj = weather;//编辑msg内容
        handler.sendMessage(msg2);//将msg发送至消息队列
        Log.i(TAG,"getWeather:天气数据发送至主线程，what=2");

    }




}
