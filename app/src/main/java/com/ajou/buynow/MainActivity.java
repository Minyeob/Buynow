package com.ajou.buynow;

import android.content.res.AssetManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Scene;
import com.ajou.buynow.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

//activity 전환을 위해서는 intent를 사용해야한다.
public class MainActivity extends AppCompatActivity implements OnClickListener{
    private static final String PACKAGE_NAME = "com.ajou.buynow"; // 데이터베이스를 Assets폴더에서 기기로 가져올때 디렉토리의 패키지 이름
    private static String DATABASE_NAME = "BUYNOW.db"; // 데이터베이스 이름

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //activity_main.xml을 초기화면으로 하는 Activity를 만든다.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //DB파일을 load 한다.
        loadDBfile();



        //각 기능으로 넘어갈 수 있는 버튼들
        ImageView btn = (ImageView)findViewById(R.id.button1);
        ImageView btn2 = (ImageView)findViewById(R.id.button2);
        ImageView btn3 = (ImageView)findViewById(R.id.button3);


        //button들에 onclicklistener를 set해서 클릭했을 때 해당하는 기능의 액티비티로 넘어갈 수 있도록 한다.
        btn.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //다른 액티비티를 호출할때 정확한 액티비티 클래스 이름까지 명시해야한다.
        //이때 사용되는 객체가 Intent인데, Intent 사용시 특정 액티비티 클래스명을 명시적으로
        //호출하는 방법을 가르켜 명시적 Intent라 한다.
        //호출대상이 되는 클래스는 동적으로 지정할 수 있다.
        Intent intent=new Intent(this, PatternAnalysisActivity_Main.class);
        Intent intent2 =new Intent(this, InquiryMangerActivity_Main.class);
        Intent intent3 = new Intent(this, PriceSearchActivity_Main.class);

        switch(v.getId()){
            //소비패턴분석 버튼을 누르면 PatternAnalysis 액티비티를 호출한다
            case R.id.button1:
            {
                startActivity(intent);//다른 액티비티를 호출하는 매서드
                break;
            }
            //구매내역조회 버튼을 누르면  액티비티를 호출한다
            case R.id.button2:
            {
                startActivity(intent2);
                break;
            }
            //최저가조회 버튼을 누르면 PriceSearch 액티비티를 호출한다.
            case R.id.button3:
            {
                startActivity(intent3);
            }
        }



        //인텐트는 액티비티 호출할때만 사용하는 것이 아니라, 데이터 전달용도로도 사용가능하다.
        //intent.putExtra("msg", et_msg.getText().toString());
        //startActivity(intent);//다른 액티비티를 호출하는 매서드
    }

    /*
	   Function Name : loadDBfile
       Function      : Assets에 있는 데이터베이스 파일을 Asset Manager를 통해 기기로 옮긴다.
	   Input         : None
	   Output        : None
    */
    private void loadDBfile() {

        AssetManager am = this.getResources().getAssets();


        File folder = new File("data/data/"+ PACKAGE_NAME + "/databases");
        if(!folder.exists()) folder.mkdir();

        File file = new File("data/data/" + PACKAGE_NAME + "/databases/" + DATABASE_NAME);
        try{

            file.createNewFile();

            InputStream is = am.open(DATABASE_NAME);
            long filesize = is.available();

            byte[] tempdata = new byte[(int)filesize];

            is.read(tempdata);

            is.close();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(tempdata);
            fos.close();
        }catch(Exception e){

        }

    }

}


