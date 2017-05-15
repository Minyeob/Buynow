// PatternAnalysisActivity_Main.java
/*
	File Name : PatternAnalysisActivity_Main.java
    Function  : BUYNOW 어플리케이션에서 소비주기패턴조회 액티비티의 클래스이다.
                큰 틀에서 기능은 다음과 같다
                    1) 소비주기계산 후 데이터베이스 업데이트,
                    2) 리스트뷰로 소비주기패턴 보여주기
                    3) 사야 될 물건을 리스트로 뽑아주기
	Author    : 김지환
	Date      : 2016/11/27
*/
package com.ajou.buynow;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static java.lang.Integer.parseInt;

public class PatternAnalysisActivity_Main extends AppCompatActivity{

    ListView listView1; // 소비주기패턴조회 액티비티의 메인 리스트뷰
    ListView cartView;  // 사야될 물건을 보여주는 카트의 리스트뷰
    PatternAnalysisActivity_ListAdapter listAdapter; // 메인 리스트뷰의 어댑터
    PatternAnalysisActivity_CartListAdapter cartListAdapter; // 카트 리스트뷰의 어댑터
    List<PatternAnalysisActivity_ListItem> listItems; // 메인 리스트뷰의 아이템을 카트 리스튜뷰와 공유하기 위한 Global Variable

    public static final String TAG = "PattenAnalysisActivity"; // Log를 위한 TAG
    private static final String PACKAGE_NAME = "com.ajou.buynow"; // 데이터베이스를 Assets폴더에서 기기로 가져올때 디렉토리의 패키지 이름

    private SQLiteDatabase db; // 데이터베이스 객체
    private PatternAnalysisActivity_DatabaseHelper dbHelper; // 데이터베이스 헬퍼 객채
    private int DATABASE_VERSION = 1; // 데이터베이스 헬퍼 객체가 사용할 데이터베이스 버전

    private static final double ALPHA = 0.8; // 소비주기분석 알고리즘에서 사용할 Factor이다
    private static final int HIGHLIGHT_MIN_DAYS = 5; // 소비주기패턴조회에서 일정 기간 이하로 남은 아이템을 하이라이트 해주기 위한 기준

    private static String DATABASE_NAME = "BUYNOW.db"; // 데이터베이스 이름

    // 데이터베이스 스키마의 테이블 이름과 속성이 바뀌는걸 감안해 스트링 상수로 지정하였다
    private static String PRODUCT_TABLE_NAME = "PRODUCT";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID = "productID";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME = "productName";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_PRICE = "price";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";

    private static String ORDER_HISTORY_TABLE_NAME = "ORDER_HISTORY";
    private static String ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE = "orderDate";
    private static String ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID = "productID";

    private static String CONSUMPTION_CYCLE_TABLE_NAME = "CONSUMPTION_CYCLE";
    private static String CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_PERIOD = "period";
    private static String CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CREATE_DATE = "createDate";
    private static String CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";

    private static String CATEGORY_TABLE_NAME = "CATEGORY";
    private static String CATEGORY_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";
    private static String CATEGORY_TABLE_ATTRIBUTE_NAME_SUPER_CATEGORY_NAME = "superCategoryName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_analysis_main);

        getConsumptionPeriod(); // 구매내역에서 소비주기를 뽑아낸다

        updateConsumptionPeriod(); // 기존의 분석된 소비주기에서 새로운 구매내역 정보가 있다면 업데이트 해준다

        listView1 = (ListView) findViewById(R.id.listView01); // 리스트뷰 찾기

        listAdapter = getListAdapter(this); // 리스트뷰 어댑터를 만든다

        listView1.setAdapter(listAdapter); // 리스트뷰 어댑터를 셋팅

        // 리스트뷰 아이템 클릭시 발생할 이벤트
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PatternAnalysisActivity_ListItem curItem = (PatternAnalysisActivity_ListItem) listAdapter.getItem(position);

            }
        });
    }
    /*
	   Class Name  : PatternAnalysisActivity_DatabaseHelper
	   Function    : SQLiteOpenHelper를 상속해 데이터베이스와 관련된 연산을 돕는다
    */
    private class PatternAnalysisActivity_DatabaseHelper extends SQLiteOpenHelper {

        public PatternAnalysisActivity_DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {

        }

        public void onOpen(SQLiteDatabase db) {

        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
    /*
       Class Name : PatternAnalysisActivity_ListAdapter
       Function   : 소비주기패턴을 리스트로 뿌려줄때 사용되는 리스트뷰 어댑터 클래스이다
    */
    private class PatternAnalysisActivity_ListAdapter extends BaseAdapter {

        private Context context;
        private List<PatternAnalysisActivity_ListItem> items = new ArrayList<>();

        public PatternAnalysisActivity_ListAdapter(Context context) {
            this.context = context;
        }

        public void addItem(PatternAnalysisActivity_ListItem item) {
            this.items.add(item);
        }

        public void setListItems(List<PatternAnalysisActivity_ListItem> listItems) {
            this.items = listItems;
        }

        public int getCount() {
            return this.items.size();
        }

        public Object getItem(int position) {
            return this.items.get(position);
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        public boolean isSelectable(int position) {
            try {
                return this.items.get(position).isSelectable();
            }catch(IndexOutOfBoundsException ex) {
                return false;
            }
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            PatternAnalysisActivity_ListView listView;

            if(convertView == null) {
                listView = new PatternAnalysisActivity_ListView(this.context, this.items.get(position));
            }else {
                listView = (PatternAnalysisActivity_ListView)convertView;

                listView.setIcon(this.items.get(position).getIcon());
                listView.setProductTitle(this.items.get(position).getProductTitle());
                listView.setPeriod(this.items.get(position).getPeriod());
                listView.setLeftPeriod(this.items.get(position).getLeftPeriod());
                listView.setProgressBar(this.items.get(position).getProgressBar_max(),this.items.get(position).getProgressBar_progress());
                listView.setBackgroundColor(this.items.get(position).getBackGroundFlag());

            }

            return listView;
        }
    }
    /*
       Class Name : PatternAnalysisActivity_ListItem
       Function   : 소비주기패턴을 리스트로 담기는 내용이 되는 리스트 아이템 클래스이다
    */
    private class PatternAnalysisActivity_ListItem {

        private int leftPeriodToBeCompared;

        private Drawable icon;

        private String productTitle;
        private String period;
        private String leftPeriod;

        private int progressBar_max;
        private int progressBar_progress;

        private boolean backgroundFlag = false;

        private boolean selectable = true;

        public PatternAnalysisActivity_ListItem(int leftPeriodToBeCompared,Drawable icon, String productTitle, String period, String leftPeriod,int progressBar_max, int progressBar_progress, boolean backgroundFlag) {
            this.leftPeriodToBeCompared = leftPeriodToBeCompared;
            this.icon = icon;
            this.productTitle = productTitle;
            this.period = period;
            this.leftPeriod = leftPeriod;
            this.progressBar_max = progressBar_max;
            this.progressBar_progress = progressBar_progress;
            this.backgroundFlag = backgroundFlag;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setLeftPeriodToBeCompared(int leftPeriodToBeCompared) {
            this.leftPeriodToBeCompared = leftPeriodToBeCompared;
        }

        public int getLeftPeriodToBeCompared() { return this.leftPeriodToBeCompared; }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getProductTitle() { return this.productTitle; }

        public void setProductTitle(String productTitle) {
            this.productTitle = productTitle;
        }

        public String getPeriod() { return period; }

        public void setPeriod(String period) {
            this.period = period;
        }

        public String getLeftPeriod() { return leftPeriod; }

        public void setLeftPeriod(String leftPeriod) {
            this.leftPeriod = leftPeriod;
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public int getProgressBar_max() {
            return this.progressBar_max;
        }

        public void setProgressBar_max(int max) {
            this.progressBar_max = max;
        }

        public int getProgressBar_progress() {
            return this.progressBar_progress;
        }

        public void setProgressBar_progress(int progress) {
            this.progressBar_progress = progress;
        }

        public boolean getBackGroundFlag() { return this.backgroundFlag; }

        public void setBackgroundFlag(Boolean backgroundFlag) {
            this.backgroundFlag = backgroundFlag;
        }

        public int compareTo(PatternAnalysisActivity_ListItem other) {
            if(this.productTitle.equals(other.getProductTitle()) && this.period.equals(other.getPeriod()) && this.leftPeriod.equals(other.getLeftPeriod())) {
                return 0;
            }else {
                return -1;
            }
        }

    }
    /*
       Class Name : PatternAnalysisActivity_ListView
       Function   : 소비주기패턴을 리스트로 뿌려줄때 리스트 아이템을 객체화 시켜주는 리스트뷰 클래스이다
    */
    private class PatternAnalysisActivity_ListView extends LinearLayout {

        private ImageView icon;
        private TextView productTitle;
        private TextView period;
        private TextView leftPeriod;
        private ProgressBar progressBar;
        private LinearLayout linearLayout;

        private static final int BASIC_BACKGROUND_COLOR = 0xee000000;
        private static final int HIGHLIGHT_BACKGROUND_COLOR = 0xddff2200;

        public PatternAnalysisActivity_ListView(Context context, PatternAnalysisActivity_ListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.pattern_analysis_listitem, this, true);

            linearLayout = (LinearLayout)findViewById(R.id.listItemLayout);

            icon = (ImageView)findViewById(R.id.icon);
            icon.setImageDrawable(aItem.getIcon());

            productTitle = (TextView)findViewById(R.id.productTitle);
            productTitle.setText(aItem.getProductTitle());

            period = (TextView)findViewById(R.id.period);
            period.setText(aItem.getPeriod());

            leftPeriod = (TextView) findViewById(R.id.leftPeriod);
            leftPeriod.setText(aItem.getLeftPeriod());

            progressBar = (ProgressBar)findViewById(R.id.progressBar);
            progressBar.setIndeterminate(false);
            progressBar.setMax(aItem.getProgressBar_max());
            progressBar.setProgress(aItem.getProgressBar_progress());

            if(aItem.getBackGroundFlag()) {
                leftPeriod.setTextColor(HIGHLIGHT_BACKGROUND_COLOR);
            }else {
                leftPeriod.setTextColor(BASIC_BACKGROUND_COLOR);
            }
        }

        public void setProductTitle(String data) {
            productTitle.setText(data);
        }

        public void setPeriod(String data) { period.setText(data); }

        public void setLeftPeriod(String data) {
            leftPeriod.setText(data);
        }

        public void setProgressBar(int max, int progress) {
            progressBar.setMax(max);
            progressBar.setProgress(progress);
        }

        public void setIcon(Drawable icon) {
            this.icon.setImageDrawable(icon);
        }

        public void setBackgroundColor(Boolean flag) {
            if(flag) {
                leftPeriod.setTextColor(HIGHLIGHT_BACKGROUND_COLOR);
            }else {
                leftPeriod.setTextColor(BASIC_BACKGROUND_COLOR);
            }

        }
    }
    /*
       Class Name : ListComparator
       Function   : 소비주기패턴을 리스트로 뿌려줄때 ArrayList를 남은 기간에 맞게 내림차순으로 정렬하기위해
                     사용되는 Comparator클래스이다
    */
    private class ListComparator implements Comparator<PatternAnalysisActivity_ListItem> {
        @Override
        public int compare(PatternAnalysisActivity_ListItem o1, PatternAnalysisActivity_ListItem o2) {

            if(Math.abs(o1.getLeftPeriodToBeCompared()) > Math.abs(o2.getLeftPeriodToBeCompared())) {
                return 1;
            }else if(Math.abs(o1.getLeftPeriodToBeCompared()) < Math.abs(o2.getLeftPeriodToBeCompared())) {
                return -1;
            }else {
                if(o1.getLeftPeriodToBeCompared() > 0 & o2.leftPeriodToBeCompared < 0) {
                    return -1;
                }else if(o1.getLeftPeriodToBeCompared() < 0 & o2.getLeftPeriodToBeCompared() > 0) {
                    return 1;
                }else {
                    return 0;
                }
            }
        }
    }
    /*
       Class Name : PatternAnalysisActivity_CartListAdapter
       Function   : 사야될 물건을 보여주는 카트 리스트뷰를 뿌려줄때 사용되는 리스트 어댑터이다
    */
    private class PatternAnalysisActivity_CartListAdapter extends BaseAdapter {

        private Context context;
        private List<PatternAnalysisActivity_CartListItem> items = new ArrayList<>();

        public PatternAnalysisActivity_CartListAdapter(Context context) {
            this.context = context;
        }

        public void addItem(PatternAnalysisActivity_CartListItem item) {
            this.items.add(item);
        }

        public void setListItems(List<PatternAnalysisActivity_CartListItem> listItems) {
            this.items = listItems;
        }

        public int getCount() {
            return this.items.size();
        }

        public Object getItem(int position) {
            return this.items.get(position);
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        public boolean isSelectable(int position) {
            try {
                return this.items.get(position).isSelectable();
            }catch(IndexOutOfBoundsException ex) {
                return false;
            }
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            PatternAnalysisActivity_CartListView listView;

            if(convertView == null) {
                listView = new PatternAnalysisActivity_CartListView(this.context, this.items.get(position));
            }else {
                listView = (PatternAnalysisActivity_CartListView)convertView;

                listView.setIcon(this.items.get(position).getIcon());
                listView.setCategoryName(this.items.get(position).getCategoryName());
                listView.setProductName(this.items.get(position).getProductName());
                listView.setProductPrice(this.items.get(position).getProductPrice());
            }

            return listView;
        }
    }
    /*
       Class Name : PatternAnalysisActivity_CartListItem
       Function   : 사야될 물건을 보여주는 카트 리스트뷰에 담기는 리스트 아이템 클래스이다
    */
    private class PatternAnalysisActivity_CartListItem {

        private Drawable icon;

        private String categoryName;
        private String productName;
        private String productPrice;

        private boolean selectable = true;

        public PatternAnalysisActivity_CartListItem(Drawable icon, String categoryName, String productName, String productPrice) {
            this.icon = icon;
            this.categoryName = categoryName;
            this.productName = productName;
            this.productPrice = productPrice;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getCategoryName() { return this.categoryName; }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

        public String getProductName() { return this.productName; }

        public void setProductName(String productName) { this.productName = productName; }

        public String getProductPrice() { return this.productPrice; }

        public void setProductPrice(String productPrice) { this.productPrice = productPrice; }
    }
    /*
       Class Name : PatternAnalysisActivity_CartListView
       Function   : 사야될 물건을 보여주는 카트 리스트뷰의 리스트 아이템들을 객체화시켜주는 리스트뷰 클래스이다
    */
    private class PatternAnalysisActivity_CartListView extends LinearLayout {

        private ImageView icon;
        private TextView categoryName;
        private TextView productName;
        private TextView productPrice;

        public PatternAnalysisActivity_CartListView(Context context, PatternAnalysisActivity_CartListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.pattern_analysis_cart_listitem, this, true);

            icon = (ImageView)findViewById(R.id.cartIcon);
            icon.setImageDrawable(aItem.getIcon());

            categoryName = (TextView)findViewById(R.id.cartCategoryName);
            categoryName.setText(aItem.getCategoryName());

            productName = (TextView)findViewById(R.id.cartProductName);
            productName.setText(aItem.getProductName());

            productPrice = (TextView)findViewById(R.id.cartProductPrice);
            productPrice.setText(aItem.getProductPrice());
        }

        public void setIcon(Drawable icon) {
            this.icon.setImageDrawable(icon);
        }

        public void setCategoryName(String categoryName) {
            this.categoryName.setText(categoryName);
        }

        public void setProductName(String productName) {
            this.productName.setText(productName);
        }

        public void setProductPrice(String productPrice) {
            this.productPrice.setText(productPrice);
        }
    }
    /*
	   Function Name : getListAdapter
       Function      : 소비주기패턴를 뿌려주기 위한 리스트 어댑터를 만든다
                       데이터베이스를 조회하여 리스트뷰 레이아웃에 맞게 ArrayList에 하나하나 추가하여
                       정렬 후 리스트어댑터에 set하여 리턴한다
	   Input         : Context
	   Output        : PatternAnalysisActivity_ListAdapter
    */
    private PatternAnalysisActivity_ListAdapter getListAdapter(Context context) {
        PatternAnalysisActivity_ListAdapter listAdapter = new PatternAnalysisActivity_ListAdapter(context);
        List<PatternAnalysisActivity_ListItem> currentListItems = new ArrayList<>();

        Drawable icon;
        String categoryName;
        String productTitle;
        String latestOrderDate;
        String leftPeriod;
        String period;

        int progressBar_max;
        int progressBar_progress;

        int leftPeriodToBeCompared;
        Boolean backgroundFlag;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {
            Cursor periodTableCursor = getConsumptionPeriodTableCursor();

            int recordCount = periodTableCursor.getCount();
            int periodTable_categoryName_column = periodTableCursor.getColumnIndex(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME);
            int periodTable_period_column = periodTableCursor.getColumnIndex(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_PERIOD);


            for (int i = 0; i < recordCount; i++) {
                backgroundFlag = false;

                periodTableCursor.moveToNext();

                categoryName = periodTableCursor.getString(periodTable_categoryName_column);

                icon = getIcon(categoryName);

                period = Integer.toString(periodTableCursor.getInt(periodTable_period_column));

                progressBar_max = parseInt(period);

                period = period + "-day Cycle";

                latestOrderDate = getLatestOrderDateByCategoryName(categoryName);

                progressBar_progress = calDate(latestOrderDate);

                leftPeriodToBeCompared = progressBar_max - progressBar_progress;

                if(Math.abs(leftPeriodToBeCompared) <= HIGHLIGHT_MIN_DAYS) {
                    backgroundFlag = true;
                }

                if(progressBar_progress < progressBar_max) {
                    if(progressBar_max - progressBar_progress >= 100) {
                        leftPeriod = "D- 99";
                    }else {
                        leftPeriod = "D- " + String.format("%02d", progressBar_max - progressBar_progress);
                    }
                }else if(progressBar_progress > progressBar_max) {
                    if(progressBar_progress - progressBar_max >= 100) {
                        leftPeriod = "D+ 99";
                    }else {
                        leftPeriod = "D+ " + String.format("%02d", progressBar_progress - progressBar_max);
                    }
                    progressBar_progress = progressBar_max;
                }else {
                    leftPeriod = "D Day!";
                }

                productTitle = categoryName;

                currentListItems.add(new PatternAnalysisActivity_ListItem(leftPeriodToBeCompared,icon,productTitle,period,leftPeriod,progressBar_max,progressBar_progress,backgroundFlag));
            }

            sortListItems(currentListItems);

            listItems = currentListItems;

            listAdapter.setListItems(currentListItems);

            periodTableCursor.close();
        }

        db.close();

        return listAdapter;
    }
    /*
	   Function Name : getCartListAdapter
       Function      : 사야될 물품을 리스트뷰로 뿌려주기 위해 사용되는 카트 리스트뷰 어댑터를 만든다.
                       기존의 소비주기패턴조회에서 사용된 리스트아이템을 그대로 가져와(Global Variable로 공유)
                       알맞게 정보를 뽑아내어 ArrayList에 담는다. 그 다음에 리스트어댑터에 set하여 리턴한다.
                       여기서 카테고리 이름에 대한 최저가를 데이터베이스로 뽑아내는 과정이 들어간다.
	   Input         : Context
	   Output        : PatternAnalysisActivity_CartListAdapter
    */
    private PatternAnalysisActivity_CartListAdapter getCartListAdapter(Context context) {
        PatternAnalysisActivity_CartListAdapter cartListAdapter = new PatternAnalysisActivity_CartListAdapter(context);
        List<PatternAnalysisActivity_CartListItem> cartListItems = new ArrayList<>();

        int listItemSize = listItems.size();
        int CART_MIN = 10;

        Drawable icon;
        String categoryName;
        String productName;
        String productPrice;
        String productID;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {

            PatternAnalysisActivity_ListItem currentListItem;

            for (int i = 0; i < listItemSize; i++) {
                currentListItem = listItems.get(i);

                if (Math.abs(currentListItem.getLeftPeriodToBeCompared()) <= CART_MIN) {
                    icon = currentListItem.getIcon();
                    categoryName = currentListItem.getProductTitle();

                    productID = getMostRecentlyProductID(categoryName);

                    productName = getProductName(productID);
                    productPrice = getLowestProductPrice(categoryName) + "원";

                    cartListItems.add(new PatternAnalysisActivity_CartListItem(icon, categoryName, productName, productPrice));
                }
            }

            cartListAdapter.setListItems(cartListItems);

        }

        db.close();

        return cartListAdapter;
    }
    /*
	   Function Name : sortListItems
       Function      : 소비주기패턴 리스트 어댑터를 만드는 과정에서
                       남아있는 기간을 기준으로 내림차순으로 정렬하기 위한 함수이다.
	   Input         : List<PatternAnalysisActivity_ListItem> (소비주기패턴의 리스트아이템 ArrayList)
	   Output        : None
    */
    private void sortListItems(List<PatternAnalysisActivity_ListItem> listItems) {
        ListComparator listComparator = new ListComparator();

        Collections.sort(listItems, listComparator);
    }
    /*
	   Function Name : openReadableDatabase
       Function      : 데이터베이스 헬퍼를 이용해 데이터베이스를 읽기전용으로 오픈한다.
	   Input         : None
	   Output        : 데이터베이스 핸들 생성 성공 여부
    */
    private boolean openReadableDatabase() {

        dbHelper = new PatternAnalysisActivity_DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        return true;
    }
    /*
	   Function Name : openWritableDatabase
       Function      : 데이터베이스 헬퍼를 이용해 데이터베이스를 읽고 쓸수있는 모드로 오픈한다.
	   Input         : None
	   Output        : 데이터베이스 핸들 생성 성공 여부
    */
    private boolean openWritableDatabase() {
        dbHelper = new PatternAnalysisActivity_DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        return true;
    }
    /*
	   Function Name : getConsumptionPeriodTableCursor
       Function      : 소비주기테이블 전체를 쿼리하여 결과로 커서를 리턴한다.
	   Input         : None
	   Output        : Cursor
    */
    private Cursor getConsumptionPeriodTableCursor() {
        String SQL = "select * from " + CONSUMPTION_CYCLE_TABLE_NAME;

        return db.rawQuery(SQL, null);
    }
    /*
	   Function Name : getOrderHistoryTableCursor
       Function      : 구매내역 테이블 전체를 쿼리하여 결과로 커서를 리턴한다.
	   Input         : None
	   Output        : Cursor
    */
    private Cursor getOrderHistoryTableCursor() {
        String SQL = "select * from " + ORDER_HISTORY_TABLE_NAME;

        return db.rawQuery(SQL, null);
    }

    /*
	   Function Name : getConsumptionPeriod
       Function      : 구매내역 정보를 이용하여 새로운 소비주기를 분석해 데이터베이스에 업데이트한다.
	   Input         : None
	   Output        : None
    */
    private void getConsumptionPeriod() {
        boolean isDatabaseOpen = openWritableDatabase();

        if(isDatabaseOpen) {
            Cursor orderHistoryTableCursor = getOrderHistoryTableCursor();

            int orderHistoryReadCount = orderHistoryTableCursor.getCount();
            int o_productID_column = orderHistoryTableCursor.getColumnIndex(ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID);

            String o_productID;
            String o_categoryName;
            String latestOrderDate;
            String secondLatestOrderDate;

            int newPeriod;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

            String today = sdf.format(new Date());

            for(int i=0;i<orderHistoryReadCount;i++) {
                orderHistoryTableCursor.moveToNext();

                o_productID = orderHistoryTableCursor.getString(o_productID_column);
                o_categoryName = getCategoryName(o_productID);

                secondLatestOrderDate = getSecondLatestOrderDateByCategoryName(o_categoryName);

                if(secondLatestOrderDate != null) {
                    if(!isThereCategoryNameInConsumptionCycleTable(o_categoryName)) {
                        latestOrderDate = getLatestOrderDateByCategoryName(o_categoryName);

                        newPeriod = diffDate(latestOrderDate, secondLatestOrderDate);

                        ContentValues values = new ContentValues();

                        values.put(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME,o_categoryName);
                        values.put(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CREATE_DATE,today);
                        values.put(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_PERIOD,newPeriod);

                        db.insert(CONSUMPTION_CYCLE_TABLE_NAME,null,values);
                    }
                }
            }

            orderHistoryTableCursor.close();
        }

        db.close();
    }
    /*
	   Function Name : updateConsumptionPeriod
       Function      : 기존의 소비주기패턴 정보에 새로운 구매내역이 있으면 새로운 소비주기를 업데이트한다.
	   Input         : None
	   Output        : None
    */
    private void updateConsumptionPeriod() {
        String latestPeriod;
        String latestOrderDate;
        String secondLatestOrderDate;
        String createDate;
        String categoryName;

        int compare;
        int newPeriod;

        int difference;

        Boolean isDatabaseOpen = openWritableDatabase();


        if(isDatabaseOpen) {
            Cursor periodTableCursor = getConsumptionPeriodTableCursor();

            int recordCount = periodTableCursor.getCount();
            int periodTable_categoryName_column = periodTableCursor.getColumnIndex(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME);
            int periodTable_period_column = periodTableCursor.getColumnIndex(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_PERIOD);
            int periodTable_createDate_column = periodTableCursor.getColumnIndex(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CREATE_DATE);

            for(int i=0;i<recordCount;i++) {
                periodTableCursor.moveToNext();

                categoryName = periodTableCursor.getString(periodTable_categoryName_column);
                createDate = periodTableCursor.getString(periodTable_createDate_column);
                latestPeriod = periodTableCursor.getString(periodTable_period_column);

                latestOrderDate = getLatestOrderDateByCategoryName(categoryName);

                compare = compareDate(createDate, latestOrderDate);

                if(compare == 2) {
                    secondLatestOrderDate = getSecondLatestOrderDateByCategoryName(categoryName);

                    difference = diffDate(latestOrderDate, secondLatestOrderDate);

                    newPeriod = (int)( ALPHA * Double.parseDouble(Integer.toString(difference)) + (1.0 - ALPHA) * Double.parseDouble(latestPeriod));

                    ContentValues values = new ContentValues();
                    values.put(CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_PERIOD,newPeriod);
                    String[] whereArgs = { categoryName };

                    db.update(CONSUMPTION_CYCLE_TABLE_NAME,values,"categoryName = ?", whereArgs);
                }
            }

            periodTableCursor.close();
        }

        db.close();
    }
    /*
	   Function Name : isThereCategoryNameInConsumptionCycleTable
       Function      : 소비주기패턴 테이블에 특정 카테고리 이름에 대한 주기가 존재하는지 알아내는 함수이다,
	   Input         : 카테고리 이름
	   Output        : 카테고리 이름에 대한 주기가 있는지 여부
    */
    private boolean isThereCategoryNameInConsumptionCycleTable(String categoryName) {
        Cursor cursor;

        String table = CONSUMPTION_CYCLE_TABLE_NAME;
        String[] columns = { CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME };
        String selection = CONSUMPTION_CYCLE_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() == 0) {
            cursor.close();
            return false;
        }else {
            cursor.close();
            return true;
        }
    }
    /*
	   Function Name : getCategoryName
       Function      : 특정 productID를 통해 카테고리 이름을 리턴한다.
	   Input         : ProductID
	   Output        : CategoryName
    */
    private String getCategoryName(String productID) {
        String categoryName;

        Cursor cursor;

        String table = PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        cursor.moveToNext();

        categoryName = cursor.getString(0);

        return categoryName;
    }
    /*
	   Function Name : getSuperCategoryName
       Function      : 특정 카테고리 이름에 대한 상위 카테고리 이름을 리턴한다.
	   Input         : CategoryName
	   Output        : SuperCategoryName
    */
    private String getSuperCategoryName(String categoryName) {
        String superCategoryName = null;

        Cursor cursor;

        String table = CATEGORY_TABLE_NAME;
        String[] columns = { CATEGORY_TABLE_ATTRIBUTE_NAME_SUPER_CATEGORY_NAME };
        String selection = CATEGORY_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(!cursor.isLast()) {

            cursor.moveToNext();

            superCategoryName = cursor.getString(0);

        }else {
            superCategoryName = null;
        }
        cursor.close();

        return superCategoryName;
    }
    /*
	   Function Name : getLatestOrderDateByCategoryName
       Function      : 특정 카테고리를 가지는 가장 최근 구매내역의 날짜를 리턴한다.
	   Input         : CategoryName
	   Output        : 가장 최근 구매 날짜
    */
    private String getLatestOrderDateByCategoryName(String categoryName) {
        String latestOrderDate;

        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME + " AS O, " + PRODUCT_TABLE_NAME +" AS P";
        String[] columns = { "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE };
        String selection = "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = " + "P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID
                + " AND P." + PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };
        String orderBy = "O." +ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        if(cursor.isLast()) {
            latestOrderDate = null;
        }else {
            cursor.moveToNext();

            latestOrderDate = cursor.getString(0);
        }

        cursor.close();

        return latestOrderDate;
    }
    /*
	   Function Name : getLatestOrderDateByProductID
       Function      : 특정 productID를 가지는 상품에 대해 가장 최근 구매내역의 날짜를 리턴한다.
	   Input         : ProductID
	   Output        : 가장 최근 구매 날짜
    */
    private String getLatestOrderDateByProductID(String productID) {
        String latestOrderDate;

        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME;
        String[] columns = { ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE };
        String selection = ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };
        String orderBy = ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        if(cursor.isLast()) {
            latestOrderDate = null;
        }else {
            cursor.moveToNext();

            latestOrderDate = cursor.getString(0);
        }

        cursor.close();

        return latestOrderDate;
    }
    /*
	   Function Name : getSecondLatestOrderDateByCategoryName
       Function      : 특정 카테고리에 대해 두번째로 최근인 구매내역의 날짜를 리턴한다.
	   Input         : CategoryName
	   Output        : 두번째로 최근인 구매 날짜
    */
    private String getSecondLatestOrderDateByCategoryName(String categoryName) {
        String secondLatestOrderDate;

        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME + " AS O, " + PRODUCT_TABLE_NAME +" AS P";
        String[] columns = { "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE };
        String selection = "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = " + "P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID
                + " AND P." + PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };
        String orderBy = "O." +ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        if(cursor.isLast()) {
            secondLatestOrderDate = null;
        }else {
            cursor.moveToNext();

            if(cursor.isLast()) {
                secondLatestOrderDate = null;
            }else {
                cursor.moveToNext();
                secondLatestOrderDate = cursor.getString(0);
            }
        }

        cursor.close();

        return secondLatestOrderDate;
    }
    /*
	   Function Name : getSecondLatestOrderDateByProductID
       Function      : 특정 productID를 가지는 상품에 대해 두번째로 최근인 구매내역의 날짜를 리턴한다.
	   Input         : ProductID
	   Output        : 두번째로 최근인 구매 날짜
    */
    private String getSecondLatestOrderDateByProductID(String productID) {
        String secondLatestOrderDate;

        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME;
        String[] columns = { ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE };
        String selection = ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };
        String orderBy = ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        cursor.moveToNext();

        if(cursor.isLast()) {
            secondLatestOrderDate = null;
        }else {
            cursor.moveToNext();
            secondLatestOrderDate = cursor.getString(0);
        }

        cursor.close();

        return secondLatestOrderDate;
    }
    /*
	   Function Name : getProductName
       Function      : 특정 productID를 가지는 상품의 이름을 리턴한다.
	   Input         : ProductID
	   Output        : ProductName
    */
    private String getProductName(String productID) {
        String productName;

        Cursor cursor;

        String table = PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        cursor.moveToNext();

        productName = cursor.getString(0);

        cursor.close();

        return productName;
    }
    /*
	   Function Name : getMostRencentlyProductID
       Function      : 특정 카테고리를 입력받아 카테고리 내에서 구매날짜가 가장 최신인 상품의 ProductID를 리턴한다.
	   Input         : CategoryName
	   Output        : ProductID
    */
    private String getMostRecentlyProductID(String categoryName) {
        String mostRecentlyProductID;

        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME + " AS O, " + PRODUCT_TABLE_NAME +" AS P";
        String[] columns = { "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID, "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE };
        String selection = "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = " + "P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID
                + " AND P." + PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };
        String orderBy = "O." +ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        cursor.moveToNext();

        mostRecentlyProductID = cursor.getString(0);

        cursor.close();

        return mostRecentlyProductID;
    }
    /*
	   Function Name : getLowestProductPrice
       Function      : 특정 카테고리를 입력받아 카테고리 내에서 가장 가격이 낮은 물품의 가격을 리턴한다.
	   Input         : CategoryName
	   Output        : Price(최저 가격)
    */
    private String getLowestProductPrice(String categoryName) {
        String lowestProductPrice;

        Cursor cursor;

        String table =  PRODUCT_TABLE_NAME;
        String[] columns = { "MIN(" + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_PRICE + ")" };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        cursor.moveToNext();

        lowestProductPrice = cursor.getString(0);

        cursor.close();

        return lowestProductPrice;
    }
    /*
	   Function Name : calDate
       Function      : 특정 날짜를 스트링으로 입력받아 현재 날짜와의 차이를 일단위로 계산해 리턴한다.
	   Input         : String date
	   Output        : Int days(날짜 차이)
    */
    private int calDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        String today = sdf.format(new Date());

        Date today_date = null;
        Date compare_date = null;

        long difference;
        int differenceInDays;

        try {
            today_date = sdf.parse(today);
            compare_date = sdf.parse(date);
        }catch(ParseException pe) {
            pe.printStackTrace();
        }

        difference = today_date.getTime() - compare_date.getTime();

        differenceInDays = (int) (difference / (24 * 60 * 60 * 1000));

        return (int) differenceInDays;
    }
    /*
	   Function Name : diffDate
       Function      : 두개의 날짜를 스트링으로 받아 두 날짜의 차이를 일단위로 계산해 리턴한다.
	   Input         : String firstDate, String secondDate
	   Output        : Int days(날짜 차이)
    */
    private int diffDate(String firstDate,String secondDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Date sdf_firstDate = null;
        Date sdf_secondDate = null;

        long difference;
        int differenceInDays;

        try {
            sdf_firstDate = sdf.parse(firstDate);
            sdf_secondDate = sdf.parse(secondDate);
        }catch(ParseException pe) {
            pe.printStackTrace();
        }

        difference = sdf_firstDate.getTime() - sdf_secondDate.getTime();

        differenceInDays = (int) (difference / (24 * 60 * 60 * 1000));

        return (int) differenceInDays;
    }
    /*
	   Function Name : compareDate
       Function      : 두개의 날짜를 스트링으로 받아 어느 날짜가 더 빠른지 알려준다.
	   Input         : String firstDate, String secondDate
	   Output        : 1(첫번째가 더 빠름), 2(두번쨰가 더 빠름)
    */
    private int compareDate(String firstDate, String secondDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Date sdf_first_date = null;
        Date sdf_second_date = null;

        long difference;

        try {
            sdf_first_date = sdf.parse(firstDate);
            sdf_second_date = sdf.parse(secondDate);
        }catch(ParseException pe) {
            pe.printStackTrace();
        }

        difference = sdf_first_date.getTime() - sdf_second_date.getTime();

        if(difference > 0 ) {
            return 1;
        }else if(difference < 0) {
            return 2;
        }else {
            return 0;
        }
    }
    /*
	   Function Name : getIcon
       Function      : 입력 받은 카테고리에 맞는 아이콘을 리턴한다.
	   Input         : CategoryName
	   Output        : Drawable(아이콘)
    */
    private Drawable getIcon(String categoryName) {

        Drawable icon;
        Resources res = getResources();
        String superCategoryName = getSuperCategoryName(categoryName);

        switch (superCategoryName) {
            case "종이":
                icon = res.getDrawable(R.drawable.category_icon_toilet_papers);
                break;

            case "음료":
                icon = res.getDrawable(R.drawable.category_icon_beverage);
                break;

            case "어류":
                icon = res.getDrawable(R.drawable.category_icon_fish);
                break;

            case "세면도구":
                icon = res.getDrawable(R.drawable.category_icon_shower);
                break;

            case "야채":
                icon = res.getDrawable(R.drawable.category_icon_vegetables);
                break;

            case "육류":
                icon = res.getDrawable(R.drawable.category_icon_meat);
                break;

            case "데스크":
                icon = res.getDrawable(R.drawable.category_icon_desk);
                break;

            case "의류":
                icon = res.getDrawable(R.drawable.category_icon_clothing);
                break;

            default:
                icon = res.getDrawable(R.drawable.category_icon_default);
                break;
        }

        return icon;
    }
    /*
	   Function Name : onCreateOptionsMenu
       Function      : 소비주기패턴조회 액티비티 생성시 메뉴를 생성한다.
	   Input         : Menu
	   Output        : Boolean
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pattern_main, menu);

        return true;
    }
    /*
	   Function Name : onOptionItemSelected
       Function      : 소비주기패턴조회 화면의 타이틀바에 위치한 메뉴의 클릭 이벤트를 담당한다.
	   Input         : Menu
	   Output        : Boolean
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int curId = item.getItemId();

        switch (curId) {
            case R.id.menu_cart:
                setContentView(R.layout.pattern_analysis_cart);

                cartView = (ListView)findViewById(R.id.cartListView01);

                cartListAdapter = getCartListAdapter(this);

                cartView.setAdapter(cartListAdapter);

                cartView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        PatternAnalysisActivity_CartListItem curItem = (PatternAnalysisActivity_CartListItem) cartListAdapter.getItem(position);

                    }
                });

                break;

            case R.id.menu_refresh:
                setContentView(R.layout.pattern_analysis_main);

                getConsumptionPeriod();

                updateConsumptionPeriod();

                listView1 = (ListView)findViewById(R.id.listView01);

                listAdapter = getListAdapter(this);

                listView1.setAdapter(listAdapter);

                listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        PatternAnalysisActivity_ListItem curItem = (PatternAnalysisActivity_ListItem) listAdapter.getItem(position);

                    }
                });

                break;
            case R.id.menu_home:
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    /*
	   Function Name : onClickCloseButton
       Function      : 사야될 물품 리스트 화면의 나가기 버튼을 눌렀을 때,
                       다시 소비주기패턴조회 리스트 화면으로 돌아오게 한다.
	   Input         : View(클릭된 뷰)
	   Output        : None
    */
    public void onClickCloseButton(View view) {
        setContentView(R.layout.pattern_analysis_main);

        getConsumptionPeriod();

        updateConsumptionPeriod();

        listView1 = (ListView)findViewById(R.id.listView01);

        listAdapter = getListAdapter(this);

        listView1.setAdapter(listAdapter);

        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PatternAnalysisActivity_ListItem curItem = (PatternAnalysisActivity_ListItem) listAdapter.getItem(position);

            }
        });
    }

}