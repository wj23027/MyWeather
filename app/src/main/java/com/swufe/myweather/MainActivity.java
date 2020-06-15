package com.swufe.myweather;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements Runnable {

    String TAG = "MainActivity";
    Spinner spinner;
    EditText editText;
    String hrefStr = "http://www.weather.com.cn/textFC/beijing.shtml";
    Handler handler;
    String todayStr;
    String areaHref;
    List<HashMap<String, String>> proList;
    HashMap<String, HashMap<String, String>> areaList;
    SharedPreferences sp;

    @SuppressLint("HandlerLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.spinner2);
        editText = findViewById(R.id.editText);
        sp = getSharedPreferences("mydata", Activity.MODE_PRIVATE);

        //获得今天日期
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        todayStr = sdf.format(Calendar.getInstance().getTime());
        Log.i("TAG", todayStr);

        handler = new Handler() {
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {//判断数据是哪个线程返回的
                    proList = (List<HashMap<String, String>>) msg.obj;
                    Log.i(TAG, "onCreate:handler1:主线程获得省份数据");
                    //保存list数据及更新日期
                    Gson gson = new Gson();
                    String json = gson.toJson(proList);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("date", todayStr);
                    editor.putString("data", json);
                    editor.apply();
                    Log.i(TAG, "onCreate:handler1:sp保存省份及更新日期");
                }
                if(msg.what == 2){
                    areaList = (HashMap<String, HashMap<String, String>>) msg.obj;
                    Log.i(TAG, "onCreate:handler1:主线程获得省份对应地区数据");


                }


//                if(msg.what == 2){
//                    weatherList = (List<HashMap<String, String>>)msg.obj;
//                    Log.i(TAG,"onCreate:handler2:主线程获得天气数据");
////                    for(HashMap<String,String> map:weatherList){
////                        Log.i(TAG, "获得："+Objects.requireNonNull(map.get("area")));
////                        //Log.i(TAG, Objects.requireNonNull(map.get("href")));
////                    }
//                    listItemAdapter2= new SimpleAdapter(WeatherList.this, weatherList,//数据源
//                            R.layout.activity_weather_item,//布局实现
//                            new String[] {"area","weatherStr","href"},
//                            new int[]{R.id.area,R.id.weather,R.id.areaHref}
//                    );
//                    listView.setAdapter(listItemAdapter2);
//                    Log.i(TAG,"onCreate:展示天气列表");
//
//                }

            }

        };

        //选择省份
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView province = view.findViewById(R.id.province);
                TextView href = view.findViewById(R.id.proURL);
                String provinceStr = String.valueOf(province.getText());
                hrefStr = String.valueOf(href.getText());
                Log.i(TAG, "onCreate:onItemSelected:点击省份：" + provinceStr);
                //Log.i(TAG,"onCreate:listView:onItemSelected:链接："+hrefStr);
                Log.i(TAG, "onCreate:onItemSelected:开启子线程...");
                Thread t = new Thread(MainActivity.this);
                t.start();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //获取sp保存的list数据至proList

        String dateStr = sp.getString("date", "");
        boolean update = !todayStr.equals(dateStr);
        //Log.i(TAG, "onCreatedata:"+data);
        Log.i(TAG, "onCreate:今日日期：" + todayStr + "，上次更新日期：" + dateStr + "，是否更新：" + update);


        Log.i(TAG, "onCreate:获取sp中省份数据...");
        Gson gson = new Gson();
        Type listType = new TypeToken<List<HashMap<String, String>>>() {
        }.getType();
        String data = sp.getString("data", "");
        proList = gson.fromJson(data, listType);
        if(proList !=null){
            Log.i(TAG,"onCreate:已获取sp省份数据");
            //展示省份列表
            SimpleAdapter listItemAdapter = new SimpleAdapter(MainActivity.this, proList,//数据源
                    R.layout.activity_province_list,//布局实现
                    new String[]{"province", "href"},
                    new int[]{R.id.province, R.id.proURL}
            );
            spinner.setAdapter(listItemAdapter);
            Log.i(TAG,"onCreate:展示省份列表");
        }


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                areaHref = null;
                String area = editText.getText().toString();
                Log.i(TAG,"showList：点击查询："+area);

                for(String areaitem :areaList.keySet()){
                    if(area.contains(areaitem)){
                        areaHref = areaitem;
                        Log.i(TAG,"查询地区："+areaitem+"href:"+areaHref);
                        break;
                    }
                }
                if(areaHref == null){
                    Toast.makeText(MainActivity.this,"没有查询到改地区",Toast.LENGTH_SHORT).show();
                }else {
                    TextView date1 = findViewById(R.id.date1);
                    TextView wind1 = findViewById(R.id.wind1);
                    TextView weather1 = findViewById(R.id.weather1);
                    TextView temp1 = findViewById(R.id.temp1);
                    //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                    date1.setText(areaList.get(areaHref).get("date1"));
                    wind1.setText(areaList.get(areaHref).get("wind1"));
                    weather1.setText(areaList.get(areaHref).get("weather1"));
                    temp1.setText(areaList.get(areaHref).get("temp1"));

                }

            }
        });
    }

        @Override
        public void run () {
            Log.i(TAG, "run:子线程...");
            Document doc = null;
            try {
                doc = Jsoup.connect(hrefStr).get();//从网页中获得doc对象
                Log.i(TAG, "getWeather:打开页面:" + doc.title());//获得body的title

            } catch (IOException e) {
                e.printStackTrace();
            }

            //getProvince;
            Log.i(TAG, "run:getProvince...");
            List<HashMap<String, String>> provinecList = WeathterMethod.getProvince(doc);
            //获取Msg对象，用于返回主线程
            Message msg1 = handler.obtainMessage(1);//标识what用于massage
            msg1.obj = provinecList;//编辑msg内容
            handler.sendMessage(msg1);//将msg发送至消息队列
            Log.i(TAG, "getProvince:省份数据发送至主线程，what=1");

            //getAreaList
            Log.i(TAG,"run:getAreaList");
            HashMap<String, HashMap<String, String>> area = (HashMap<String, HashMap<String, String>>) WeathterMethod.getAreaList(doc);
            Message msg2 = handler.obtainMessage(2);
            msg2.obj = area;
            handler.sendMessage(msg2);

            if(areaHref != null) {
                Document areaDoc = null;
                Log.i(TAG, "查找链接：" + areaHref);
                try {
                    areaDoc = Jsoup.connect(areaHref).get();//从网页中获得doc对象
                    Log.i(TAG, "getWeather:打开页面:" + areaDoc.title());//获得body的title
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void showList(View btn){



//            Document areaDoc = null;






        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.weather_menu){
            Intent config = new Intent(this, WeatherList.class);
            Log.i(TAG,"打开页面：WeatherList");
            //startActivity(config);
            startActivityForResult(config,1);

        }

        return super.onOptionsItemSelected(item);
    }
}

