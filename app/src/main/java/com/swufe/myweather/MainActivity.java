package com.swufe.myweather;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
    TextView provincetxt;
    Boolean area_update = false;
    String todayStr;
    String areaHref;
    String provinceStr;
    TextView areatxt;
    boolean update;
    List<HashMap<String, String>> proList;
    HashMap<String, HashMap<String, String>> areaList;
    SharedPreferences sp;

    @SuppressLint("HandlerLeak")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.spinner2);
        editText = findViewById(R.id.editText);
        provincetxt = findViewById(R.id.provincetxt);
        sp = getSharedPreferences("mydata", Activity.MODE_PRIVATE);

        //获得今天日期
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        todayStr = sdf.format(Calendar.getInstance().getTime());
        Log.i("TAG", todayStr);

        handler = new Handler() {
            @SuppressLint("ShowToast")
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
                    provincetxt.setText(provinceStr);
                    area_update = true;
                    Toast.makeText(MainActivity.this,"天气数据已更新",Toast.LENGTH_SHORT
                    ).show();
                    Log.i(TAG, "onCreate:handler1:主线程获得省份对应地区数据");


                }


            }

        };

        //选择省份
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                provincetxt.setText("...");
                area_update = false;
                TextView province = view.findViewById(R.id.province);
                TextView href = view.findViewById(R.id.proURL);
                provinceStr = String.valueOf(province.getText());
                hrefStr = String.valueOf(href.getText());
                areaHref = null;
                editText.setText("");
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
        update = !todayStr.equals(dateStr);
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

            @SuppressLint("SetTextI18n")
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void afterTextChanged(Editable s) {
                String area = editText.getText().toString();
                if(!area.equals("")) {
                    if (!area_update) {
                        Toast.makeText(MainActivity.this, "请稍等", Toast.LENGTH_SHORT).show();
                    } else {
                        areaHref = null;
                        Log.i(TAG, "showList：点击查询：" + area);

                        for (String areaitem : areaList.keySet()) {
                            if (area.contains(areaitem)) {
                                areaHref = areaitem;
                                Log.i(TAG, "查询地区：" + areaitem);
                                break;
                            }
                        }

                        if (areaHref == null) {
                            Toast.makeText(MainActivity.this, "未查询到该地区", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "已显示" + areaHref + "天气", Toast.LENGTH_SHORT).show();
                            areatxt = findViewById(R.id.area);
                            areatxt.setText(areaHref + "天气");
                            TextView date1 = findViewById(R.id.date1);
                            TextView wind1 = findViewById(R.id.wind1);
                            TextView weather1 = findViewById(R.id.weather1);
                            TextView temp1 = findViewById(R.id.temp1);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date1.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date1"));
                            wind1.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind1"));
                            weather1.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather1"));
                            temp1.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp1"));

                            TextView date2 = findViewById(R.id.date2);
                            TextView wind2 = findViewById(R.id.wind2);
                            TextView weather2 = findViewById(R.id.weather2);
                            TextView temp2 = findViewById(R.id.temp2);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date2.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date2"));
                            wind2.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind2"));
                            weather2.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather2"));
                            temp2.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp2"));

                            TextView date3 = findViewById(R.id.date3);
                            TextView wind3 = findViewById(R.id.wind3);
                            TextView weather3 = findViewById(R.id.weather3);
                            TextView temp3 = findViewById(R.id.temp3);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date3.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date3"));
                            wind3.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind3"));
                            weather3.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather3"));
                            temp3.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp3"));

                            TextView date4 = findViewById(R.id.date4);
                            TextView wind4 = findViewById(R.id.wind4);
                            TextView weather4 = findViewById(R.id.weather4);
                            TextView temp4 = findViewById(R.id.temp4);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date4.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date4"));
                            wind4.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind4"));
                            weather4.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather4"));
                            temp4.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp4"));

                            TextView date5 = findViewById(R.id.date5);
                            TextView wind5 = findViewById(R.id.wind5);
                            TextView weather5 = findViewById(R.id.weather5);
                            TextView temp5 = findViewById(R.id.temp5);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date5.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date5"));
                            wind5.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind5"));
                            weather5.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather5"));
                            temp5.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp5"));

                            TextView date6 = findViewById(R.id.date6);
                            TextView wind6 = findViewById(R.id.wind6);
                            TextView weather6 = findViewById(R.id.weather6);
                            TextView temp6 = findViewById(R.id.temp6);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date6.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date6"));
                            wind6.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind6"));
                            weather6.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather6"));
                            temp6.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp6"));

                            TextView date7 = findViewById(R.id.date7);
                            TextView wind7 = findViewById(R.id.wind7);
                            TextView weather7 = findViewById(R.id.weather7);
                            TextView temp7 = findViewById(R.id.temp7);
                            //Log.i(TAG,"date:"+areaList.get(areaHref).get("date1"));
                            date7.setText(Objects.requireNonNull(areaList.get(areaHref)).get("date7"));
                            wind7.setText(Objects.requireNonNull(areaList.get(areaHref)).get("wind7"));
                            weather7.setText(Objects.requireNonNull(areaList.get(areaHref)).get("weather7"));
                            temp7.setText(Objects.requireNonNull(areaList.get(areaHref)).get("temp7"));

                        }
                    }
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

            if(update){
                //getProvince;
                Log.i(TAG, "run:getProvince...");
                assert doc != null;
                List<HashMap<String, String>> provinecList = WeathterMethod.getProvince(doc);
                //获取Msg对象，用于返回主线程
                Message msg1 = handler.obtainMessage(1);//标识what用于massage
                msg1.obj = provinecList;//编辑msg内容
                handler.sendMessage(msg1);//将msg发送至消息队列
                Log.i(TAG, "getProvince:省份数据发送至主线程，what=1");
            }

            //getAreaList
            Log.i(TAG,"run:getAreaList");
            assert doc != null;
            HashMap<String, HashMap<String, String>> area = (HashMap<String, HashMap<String, String>>) WeathterMethod.getAreaList(doc);
            Message msg2 = handler.obtainMessage(2);
            msg2.obj = area;
            handler.sendMessage(msg2);

            if(areaHref != null) {
                Document areaDoc;
                Log.i(TAG, "查找链接：" + areaHref);
                try {
                    areaDoc = Jsoup.connect(areaHref).get();//从网页中获得doc对象
                    Log.i(TAG, "getWeather:打开页面:" + areaDoc.title());//获得body的title
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @SuppressLint("ShowToast")
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        public void showList(View btn){
            if(areaHref == null){
                Toast.makeText(MainActivity.this,"请输入地区名称",Toast.LENGTH_SHORT).show();
            }else{
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Objects.requireNonNull(areaList.get(areaHref)).get("href")));
                startActivity(intent);
                Log.i(TAG,"onCreate:onItemClick:打开链接："+ Objects.requireNonNull(areaList.get(areaHref)).get("href"));
            }


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

