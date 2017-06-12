package com.example.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


//显示天气信息的活动
public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;
    public SwipeRefreshLayout swipeRefresh;
    private ScrollView weatherLayout;
    private Button navButton;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    private String mWeatherId;//用于记录城市天气的id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

    /*    //将背景图和状态栏融合在一起
       if(Build.VERSION.SDK_INT>=21){

           //拿到当前活动的DecorView
            View decorView = getWindow().getDecorView();

            //setSystemUiVisibility()方法改变系统UI的显示,这里的参数表示活动的布局会显示在状态栏上面
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

           //将状态栏设置成透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
     }*/


        //初始化控件
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);//设置下拉刷新条的颜色
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);


        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气数据
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);//第一次无缓存，请求天气时隐藏滚动条weatherLayout。
            requestWeather(mWeatherId);
        }

        //调用swipeRefresh.setOnRefreshListener（）来设置一个下拉刷新的监听器，当触发了下拉刷新操作时，
        // 就会回调这个监听器的onRefresh()方法，我们在这里调用requestWeather（）方法请求天气信息即可
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

//在左上角的那个按钮的点击事件中，调用DrawLayout的openDrawer()方法来打开滑动菜单
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

//尝试从SharedPreferences中读取缓存的背景图片，如果有缓存，就直接使用Glide来加载这张图片，
// 如果没有就调用loadBingPic()方法去请求今日的必应背景图
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }


    }


    //根据天气id请求城市天气信息
    public void requestWeather(final String weatherId) {

        //1.使用参数传进来的天气id和我们申请的API Key拼装出一个接口地址
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=aee3b8fdb0cf4c1fa4c010503b5be677";

        //2.调用HttpUtil.sendOkHttpRequest()方法来向该地址发送请求，服务器会将相应的天气信息以JSON格式返回。
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

            @Override
            //3.在onResponse()回调中将返回的JSON数据转换成Weather对象
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string(); //服务器返回的具体内容responseText

                //调用Utility.handleWeatherResponse()解析服务器返回的数据数据
                final Weather weather = Utility.handleWeatherResponse(responseText);

                //将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //判断服务器返回的状态，返回OK说明请求天气成功。
                        if (weather != null && "ok".equals(weather.status)) {

                            //将返回的数据存储到SharedPreferences中
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                                    (WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();


                            mWeatherId = weather.basic.weatherId;
                            //显示内容
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT)
                                    .show();
                        }
                        swipeRefresh.setRefreshing(false);//用于表示刷新事件结束
                    }
                });

            }


            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT)
                                .show();
                        swipeRefresh.setRefreshing(false);//用于刷新事件结束，并隐藏刷新进度条
                    }
                });
            }


        });
        loadBingPic();
    }


    //下次再进入WeatherActivity时，由于缓存已经存在，因此会直接解析并显示天气信息，不会再发起网络请求


    //加载必应每日一图
    private void loadBingPic() {
        final String requestBingPic = "http://guolin.tech/api/bing_pic";

//调用HttpUtil.sendOkHttpRequest() 方法获取必应背景图的链接
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {


            @Override
            public void onResponse(Call call, Response response) throws IOException {

                //将必应背景图的链接缓存到SharedPreferences当中
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences
                        (WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();

                //将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //最后使用Glide加载这张图片
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }


    //处理并展示Weather实体类中的数据：从weather中获取数据并显示到相应的控件上。
    private void showWeatherInfo(Weather weather) {


        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "°c";
        String weatherInfo = weather.now.more.info;


        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();


        for (Forecast forecast : weather.forecastList) {

            //在循环中动态加载forecast_item布局，并设置相应数据，并添加到父布局当中
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);

        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);

        }

        String comfort = "舒适度:" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议:" + weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        //让ScrollView重新变得可见
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

    }

}
