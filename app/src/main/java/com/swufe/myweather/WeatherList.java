package com.swufe.myweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WeatherList extends AppCompatActivity implements Runnable {
    ArrayAdapter<String> adapter;
    Spinner spinner;
    Handler handler;
    String  hrefStr;

    String TAG = "WeatherList";
    List<HashMap<String, String>> proList;
    private SimpleAdapter listItemAdapter;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);
        spinner = findViewById(R.id.provinceSpinner);
        SharedPreferences sp = getSharedPreferences("mydata", Activity.MODE_PRIVATE);
        proList = new ArrayList<HashMap<String, String>>();

        for(int i = 0;i<100;i++){
            HashMap<String,String>  map = new HashMap<String,String>();
            map.put("province","province"+i);
            map.put("href","href"+i);
            proList.add(map);        }


        listItemAdapter= new SimpleAdapter(this, proList,//数据源
                R.layout.activity_province_list,//布局实现
                new String[] {"province","href"},
                new int[]{R.id.province,R.id.proURL}
        );
        spinner.setAdapter(listItemAdapter);

        Log.i(TAG, "onCreate:开启子线程");
//        Thread t = new Thread(this);
//        t.start();
        handler = new Handler(){//用于获取其他线程中的消息
            @Override
            public void handleMessage(@NonNull Message msg) {//获得数据队列
                super.handleMessage(msg);
                if(msg.what == 1){//判断数据是哪个线程返回的
                    proList = (List<HashMap<String, String>>)msg.obj;
                    Log.i(TAG,"onCreate:handler:获得数据队列中的list数据");
                    //保存list数据及更新日期
                    SharedPreferences sp = getSharedPreferences("mydata", Activity.MODE_PRIVATE);
                    Gson gson = new Gson();
                    String json = gson.toJson(proList);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("data",json);
                    editor.apply();
                    Log.i(TAG, "onCreate:handler:将数据保存至sp");
                }


            }
        };



        //获取sp保存的list数据至proList
        String data = sp.getString("data", "");
        Log.i(TAG, "data:"+data);
        Log.i(TAG, "onCreate:获取sp中的数据");

        Gson gson = new Gson();
        Type listType = new TypeToken<List<HashMap<String,String>>>() {
        }.getType();
        proList = gson.fromJson(data, listType);

        if(proList !=null){
            Log.i(TAG, "proList！=null");
//            for(HashMap<String,String> map:proList){
//                //Log.i(TAG, Objects.requireNonNull(map.get("province")));
//                //Log.i(TAG, Objects.requireNonNull(map.get("href")));//
//            }
            Log.i(TAG,"onCreate:SharedPreferences:已获取sp保存的list数据");

            //在布局中展示list数据
            listItemAdapter= new SimpleAdapter(this, proList,//数据源
                    R.layout.activity_province_list,//布局实现
                    new String[] {"province","href"},
                    new int[]{R.id.province,R.id.proURL}
            );
            spinner.setAdapter(listItemAdapter);
            Log.i(TAG,"onCreate:利用list数据重置listView");
        }

        //选择省份
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView province= view.findViewById(R.id.province);
                TextView href= view.findViewById(R.id.proURL);
                String provinceStr = String.valueOf(province.getText());
                hrefStr = String.valueOf(href.getText());
                Log.i(TAG,"onCreate:listView:onItemSelected:省份："+provinceStr);
                Log.i(TAG,"onCreate:listView:onItemSelected:链接："+hrefStr);
                Thread t = new Thread(WeatherList.this);
                t.start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        Log.i(TAG, "run:子线程");
        //getProvince;
        List<HashMap<String, String>> provinecList=getProvince();
        Log.i(TAG, "run:已获取省份数据至provinecList");

        //获取Msg对象，用于返回主线程
        Message msg1 = handler.obtainMessage(1);//标识what用于massage
        msg1.obj = provinecList;//编辑msg内容
        handler.sendMessage(msg1);//将msg发送至消息队列
        Log.i(TAG,"run:子线程的provinecList数据发送至消息队列Message");


        //getWeather;
        List<HashMap<String, String>> weatherList=getWeather();
        Log.i(TAG, "run:已获取天气数据至weatherList");

        //获取Msg对象，用于返回主线程
        Message msg2 = handler.obtainMessage(2);//标识what用于massage
        msg2.obj = weatherList;//编辑msg内容
        handler.sendMessage(msg2);//将msg发送至消息队列
        Log.i(TAG,"run:子线程的weatherList数据发送至消息队列Message");

    }




    public List<HashMap<String, String>> getProvince(){
        List<HashMap<String, String>> itemList = new ArrayList<HashMap<String, String>>();
        Document doc = null;
        try {
            doc = Jsoup.connect("http://www.weather.com.cn/textFC/beijing.shtml").get();//从网页中获得doc对象
            Log.i(TAG,"getProvince:打开页面:"+ doc.title());//获得body的title
            Elements divs = doc.getElementsByTag("div");//在Document dot中获取所有table内的内容
            Element div = divs.get(26);
            //Log.i(TAG,"run:ul:"+ ul);
            Elements as = div.getElementsByTag("a");
            for(int i = 0;i<as.size();i++){
                String province = as.get(i).text();
                //Log.i(TAG, "run:province:"+ province);
                String href = "http://www.weather.com.cn/"+as.get(i).attr("href");
                //Log.i(TAG, "run:href:"+ href);
                HashMap<String,String>  map = new HashMap<String,String>();
                map.put("province",province);
                map.put("href",href);
                itemList.add(map);
            }
            Log.i(TAG, "getProvince:已获取省份数据至itemList");
        }catch (IOException e) {
            Log.e(TAG,"run:"+e.toString());
            e.printStackTrace();
        }
        return itemList;
    }

    public List<HashMap<String, String>> getWeather(){
        List<HashMap<String, String>> itemList = new ArrayList<HashMap<String, String>>();
        Document doc = null;
        try {
            doc = Jsoup.connect(hrefStr).get();//从网页中获得doc对象
            Log.i(TAG,"getWeather:打开页面:"+ doc.title());//获得body的title
            //在Document dot中获取所有table内的内容
            String date = doc.getElementsByTag("ul").get(8).getElementsByTag("li").get(1).text();
            Log.i(TAG,"getWeather:明日:"+ date+"天气");
            Element div_31 = doc.getElementsByTag("div").get(31);
            Log.i(TAG,"run:div_31:"+ div_31.text());
            Element conMidtab= doc.getElementsByClass("conMidtab") .get(1);
            Log.i(TAG,"run:conMidtab:"+ conMidtab.text());
            Elements tbodys = conMidtab.getElementsByClass("conMidtab3");
            for(Element tbody:tbodys) {
                //Log.i(TAG,"run:ul:"+ ul);
                Log.i(TAG,"run:tbody:"+ tbody.text());
                Elements trs = tbody.getElementsByTag("tr");
                for (int i = 0; i < trs.size(); i++) {
                    Elements tds = trs.get(i).getElementsByTag("td");
                    Element a = trs.get(i).getElementsByTag("a").get(0);
                    String href = a.attr("href");
                    String area, weather1, temp1, weather2, temp2, weather;
                    if (i == 0) {
                        area = tds.get(1).text();
                        weather1 = tds.get(2).text();
                        temp1 = tds.get(4).text();
                        weather2 = tds.get(5).text();
                        temp2 = tds.get(7).text();
                    } else {
                        area = tds.get(0).text();
                        weather1 = tds.get(1).text();
                        temp1 = tds.get(3).text();
                        weather2 = tds.get(4).text();
                        temp2 = tds.get(6).text();
                    }
                    if (weather1.equals(weather2)) {
                        weather = weather1;
                    } else {
                        weather = weather1 + "转" + weather2;
                    }

                    String temp = temp2 + "℃~" + temp1 + "℃";
                    String weatherStr = weather+"  "+temp;

                    Log.i(TAG, "getWeather:area:" + area);
                    Log.i(TAG, "getWeather:weatherStr:" + weatherStr);
                    Log.i(TAG, "getWeather:href:" + href);
                    //Log.i(TAG, "run:href:"+ href);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("area", area);
                    map.put("weatherStr", weatherStr);
                    map.put("href", href);
                    itemList.add(map);
                }
            }
            Log.i(TAG, "getProvince:已获取天气数据至itemList");
        }catch (IOException e) {
            Log.e(TAG,"getProvince:"+e.toString());
            e.printStackTrace();
        }
        return itemList;
    }
}
