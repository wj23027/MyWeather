package com.swufe.myweather;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public  class WeathterMethod {
    private static String TAG = "WeathterMethod";
    public static List<HashMap<String, String>> getProvince(Document doc){
        List<HashMap<String, String>> itemList = new ArrayList<HashMap<String, String>>();
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
        Log.i(TAG, "getProvince:已获取省份数据");
        return itemList;
    }

    public static List<HashMap<String, String>> getWeather(Document doc,int d){
        List<HashMap<String, String>> itemList = new ArrayList<HashMap<String, String>>();
        //在Document dot中获取所有table内的内容
        Element div_31 = doc.getElementsByTag("div").get(31);
        Element conMidtab= doc.getElementsByClass("conMidtab") .get(d);
        Elements tbodys = conMidtab.getElementsByClass("conMidtab3");
        for(Element tbody:tbodys) {
            //Log.i(TAG,"run:ul:"+ ul);
            //Log.i(TAG,"run:tbody:"+ tbody.text());
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
                //Log.i(TAG, "getWeather:area:" + area);
//                    Log.i(TAG, "getWeather:weatherStr:" + weatherStr);
//                    Log.i(TAG, "getWeather:href:" + href);
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("area", area);
                map.put("weatherStr", weatherStr);
                map.put("href", href);
                itemList.add(map);
            }
        }
        Log.i(TAG, "getWeather:已获取天气数据");
        return itemList;
    }

    public static HashMap<String,HashMap<String, String>> getAreaList(Document doc){
        HashMap<String, HashMap<String, String>> itemList = new HashMap<>();
        //在Document dot中获取所有table内的内容
        Element div_31 = doc.getElementsByTag("div").get(31);
        Element conMidtab= doc.getElementsByClass("conMidtab") .get(1);
        Elements tbodys = conMidtab.getElementsByClass("conMidtab3");
        for(Element tbody:tbodys) {
            Elements trs = tbody.getElementsByTag("tr");
            for (int i = 0; i < trs.size(); i++) {
                Elements tds = trs.get(i).getElementsByTag("td");
                Element a = trs.get(i).getElementsByTag("a").get(0);
                String href = a.attr("href");
                String area, weather1, temp1, weather2, temp2, weather,wind1,wind2;
                if (i == 0) {
                    area = tds.get(1).text();
                } else {
                    area = tds.get(0).text();
                }
                HashMap<String,String> map= new HashMap<>();
                map.put("href", href);
                itemList.put(area,map);
                Log.i(TAG,"area："+area);
            }
        }
        Set set = itemList.keySet();
        for(Object area:set){//编辑某个地区的map
            String a = (String) area;
            HashMap<String,String> map = itemList.get(area);
            Log.i(TAG,"area："+area);
            assert map != null;
            String href = map.get("href");
            Log.i(TAG,"href："+href);
            Document areadoc = null;
            try {
                doc = Jsoup.connect(href).get();//从网页中获得doc对象
                Log.i(TAG, "getAreaList:打开页面:" + doc.title());//获得body的title
                Element ul = doc.getElementsByClass("t clearfix").get(0);
                Elements lis = ul.getElementsByTag("li");
                for(int i = 0;i<7;i++){
                    Element li = lis.get(i);
                    String date = li.getElementsByTag("h1").get(0).text();
                    String weather = li.getElementsByTag("p").get(0).text();
                    String temp = li.getElementsByTag("span").get(0).text()+"/"+li.getElementsByTag("i").get(0).text();
                    String wind = li.getElementsByTag("i").get(1).text();
                   // String wind2 = li.getElementsByTag("em").get(0).getElementsByTag("span").get(1).text();

                    map.put("date"+i,date);
                    map.put("weather"+i,weather);
                    map.put("temp"+i,temp);
                    map.put("wind"+i,wind);
                    //map.put("wind2"+i,wind2);

//
//                    Log.i(TAG,"date:"+date);
//                    Log.i(TAG,"weather:"+weather);
//                    Log.i(TAG,"temp:"+temp);
//                    Log.i(TAG,"wind:"+wind);
                   //Log.i(TAG,"wind2:"+wind2);


                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            itemList.put(a,map);
        }
        Log.i(TAG, "getArea:已获取地区");
        return itemList;
    }
}
