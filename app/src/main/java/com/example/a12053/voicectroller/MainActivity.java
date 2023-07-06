package com.example.a12053.voicectroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements OnClickListener {

    //存放听写分析结果文本
    private HashMap<String, String> hashMapTexts = new LinkedHashMap<String, String>();
    private Button b_btn;  //初始化控件
    private EditText e_text;
    private WebView webView;
    private Timer timer;
    public int number;

    public String appName = "应用的名称"; // 将 "应用的名称" 替换为你已有的应用名称变量
    PackageManager packageManager;
    Intent launchIntent;
    SpeechRecognizer hearer;  //听写对象

    RecognizerDialog dialog;  //讯飞提示框

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // 在此处创建并显示弹窗对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("提醒");
            builder.setMessage(number+"秒钟已经过去了！");
            builder.setPositiveButton("确定", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_btn = (Button) findViewById(R.id.listen_btn);
        e_text = (EditText) findViewById(R.id.content_et);

        b_btn.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.listen_btn:

                // 语音配置对象初始化
                SpeechUtility.createUtility(MainActivity.this, SpeechConstant.APPID + "=5b932d09");

                // 1.创建SpeechRecognizer对象，第2个参数：本地听写时传InitListener
                hearer = SpeechRecognizer.createRecognizer( MainActivity.this, null);
                // 交互动画
                dialog = new RecognizerDialog(MainActivity.this, null);
                // 2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
                hearer.setParameter(SpeechConstant.DOMAIN, "iat"); // domain:域名
                hearer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                hearer.setParameter(SpeechConstant.ACCENT, "mandarin"); // mandarin:普通话

                //3.开始听写
                dialog.setListener(new RecognizerDialogListener() {  //设置对话框

                    @Override
                    public void onResult(RecognizerResult results, boolean isLast) {
                        // TODO 自动生成的方法存根
                        Log.d("Result", results.getResultString());
                        //(1) 解析 json 数据<< 一个一个分析文本 >>
                        StringBuffer strBuffer = new StringBuffer();
                        try {
                            JSONTokener tokener = new JSONTokener(results.getResultString());
                            Log.i("TAG", "Test"+results.getResultString());
                            Log.i("TAG", "Test"+results.toString());
                            JSONObject joResult = new JSONObject(tokener);

                            JSONArray words = joResult.getJSONArray("ws");
                            for (int i = 0; i < words.length(); i++) {
                                // 转写结果词，默认使用第一个结果
                                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                                JSONObject obj = items.getJSONObject(0);
                                strBuffer.append(obj.getString("w"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//            		String text = strBuffer.toString();
                        // (2)读取json结果中的sn字段
                        String sn = null;
                        try {
                            JSONObject resultJson = new JSONObject(results.getResultString());
                            sn = resultJson.optString("sn");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //(3) 解析语音文本<< 将文本叠加成语音分析结果  >>
                        hashMapTexts.put(sn, strBuffer.toString());
                        StringBuffer resultBuffer = new StringBuffer();  //最后结果
                        for (String key : hashMapTexts.keySet()) {
                            resultBuffer.append(hashMapTexts.get(key));
                        }

                        e_text.setText(resultBuffer.toString());
                        e_text.requestFocus();//获取焦点
                        e_text.setSelection(1);//将光标定位到文字最后，以便修改

                        //打开网页
                        if(resultBuffer.toString().contains("搜索")){
                            String keyword = resultBuffer.toString().replace("搜索","");
                            Log.d("MyTag",keyword);
                            String url = "https://www.bing.com/search?q=" + keyword;
                            //webView = findViewById(R.id.webView);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        }
                        //打开定时器
                        if(resultBuffer.toString().contains("提醒我") || resultBuffer.toString().contains("定时"))
                        {
                            String input=resultBuffer.toString();
                            Pattern pattern = Pattern.compile("\\d+"); // 匹配一个或多个数字
                            Matcher matcher = pattern.matcher(input);
                            if(matcher.find()) {
                                number = Integer.parseInt(matcher.group());
                                Log.d("MyTag",matcher.group());
                            }
                            //未检索到数字
                            else{
                                return;
                            }
                            //定时器
                            // 安排x分钟后执行提醒
                            long delayMillis = (long) number *  1000; // 毫秒数
                            handler.postDelayed(runnable, delayMillis);
                        }

                        //打电话
                        if(resultBuffer.toString().contains("拨打") || resultBuffer.toString().contains("打电话给")){
                            String input=resultBuffer.toString();
                            Pattern pattern = Pattern.compile("\\d+"); // 匹配一个或多个数字
                            Matcher matcher = pattern.matcher(input);
                            if(matcher.find()) {
                                System.out.println("matcher.group()=  "+matcher.group());
                                Uri uri =Uri.parse("tel:"+matcher.group());
                                Intent it = new Intent(Intent.ACTION_DIAL,uri);
                                startActivity(it);
                            }
                            //未检索到数字
                            else{
                                return;
                            }
                        }
                    }

                    @Override
                    public void onError(SpeechError error) {
                        // TODO 自动生成的方法存根
                        error.getPlainDescription(true);
                    }
                });

                dialog.show();  //显示对话框

                break;
            default:
                break;
        }
    }
}
