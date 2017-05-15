// PatternAnalysisActivity_Main.java
/*
	File Name : InquiryMangerAcitivity.java
    Function  : BUYNOW 어플리케이션에서 구매내역조회 액티비티의 클래스이다.
                큰 틀에서 기능은 다음과 같다
                    1) 구매내역을 조회한다.
                    2) 날짜, 카테고리, 상품명, 마트명을 통해 조회를 할 수 있다.
	Author    : 박수인, 김지환
	Date      : 2016/12/04
*/
package com.ajou.buynow;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class InquiryMangerActivity_Main extends AppCompatActivity {

    ListView listView1; // 구매내역을 보여주는 리스트
    InquiryManager_ListAdapter listAdapter; // 구매내역과 연결된 어댑터

    private static final String PACKAGE_NAME = "com.example.jhwan.buynow"; // 데이터베이스를 Assets폴더에서 기기로 가져올때 디렉토리의 패키지 이름
    private static final String DATABASE_NAME = "BUYNOW.db";
    private SQLiteDatabase db; // 데이터베이스 객체
    private InquiryManagerActivity_DBHelper dbHelper; // 데이터베이스 헬퍼 객채
    private int DATABASE_VERSION = 1; // 데이터베이스 헬퍼 객체가 사용할 데이터베이스 버전

    private static String PRODUCT_TABLE_NAME = "PRODUCT";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID = "productID";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME = "productName";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_MART_ID = "martID";

    private static String ORDER_HISTORY_TABLE_NAME = "ORDER_HISTORY";
    private static String ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE = "orderDate";
    private static String ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID = "productID";

    private static String MART_TABLE_NAME = "MART";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_ID = "martID";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_NAME = "martName";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_LOCATION = "martLocation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.inquiry_main);

        LinearLayout parent_layout = (LinearLayout)findViewById(R.id.inquiry_parent_layout);

        final CheckBox orderDateCheckBox = (CheckBox)findViewById(R.id.orderdate_checkbox);
        final CheckBox categoryCheckBox = (CheckBox)findViewById(R.id.category_checkbox);
        CheckBox productCheckBox = (CheckBox)findViewById(R.id.product_checkbox);
        CheckBox martCheckBox = (CheckBox)findViewById(R.id.mart_checkbox);

        final EditText orderDateStartEditText = (EditText)findViewById(R.id.start_orderdate_edit_text);
        final EditText orderDateEndEditText = (EditText)findViewById(R.id.end_orderdate_edit_text);
        final EditText categoryEditText = (EditText)findViewById(R.id.category_edit_text);
        final EditText productEditText = (EditText)findViewById(R.id.product_name_edit_text);
        final EditText martEditText = (EditText)findViewById(R.id.mart_name_edit_text);

        orderDateStartEditText.setNextFocusForwardId(R.id.end_orderdate_edit_text);

        parent_layout.setOnContextClickListener(new View.OnContextClickListener() {
            @Override
            public boolean onContextClick(View view) {
                orderDateEndEditText.clearFocus();
                orderDateStartEditText.clearFocus();
                categoryEditText.clearFocus();
                productEditText.clearFocus();
                martEditText.clearFocus();

                return false;
            }
        });

        orderDateCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                orderDateStartEditText.setText(null);
                orderDateEndEditText.setText(null);

                if(buttonView.isChecked()) {
                    orderDateStartEditText.setHint("시작날짜(yymmdd)");
                    orderDateEndEditText.setHint("종료날짜(yymmdd)");
                }else {
                    orderDateStartEditText.setHint(null);
                    orderDateEndEditText.setHint(null);
                    orderDateStartEditText.clearFocus();
                    orderDateEndEditText.clearFocus();
                }
            }
        });

        categoryCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                categoryEditText.setText(null);

                if(buttonView.isChecked()) {
                    categoryEditText.setHint("카테고리를 입력하세요");
                }else {
                    categoryEditText.setHint(null);
                    categoryEditText.clearFocus();
                }
            }
        });

        productCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                productEditText.setText(null);

                if(buttonView.isChecked()) {
                    productEditText.setHint("상품명을 입력하세요");
                }else {
                    productEditText.setHint(null);
                    productEditText.clearFocus();
                }

            }
        });

        martCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                martEditText.setText(null);

                if(buttonView.isChecked()) {
                    martEditText.setHint("마트 이름을 입력하세요");
                }else {
                    martEditText.setHint(null);
                    martEditText.clearFocus();
                }
            }
        });

        orderDateStartEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(view.isFocused()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(view, 0);
                }
            }
        });

        orderDateEndEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(view.isFocused()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(view, 0);
                }
            }
        });

        productEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(view.isFocused()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(view, 0);
                }
            }
        });

        categoryEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(view.isFocused()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(view, 0);
                }
            }
        });

        martEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(view.isFocused()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(view, 0);
                }
            }
        });
    }
    /*
    구매내역의 데이터와 리스트뷰를 연결해주는 리스트어댑터 클래스
    */
    private class InquiryManager_ListAdapter extends BaseAdapter {

        private Context context;
        private List<InquiryManagerActivity_ListItem> items = new ArrayList<>();

        public InquiryManager_ListAdapter(Context context) {
            this.context = context;
        }

        public void addItem(InquiryManagerActivity_ListItem item) {
            this.items.add(item);
        }

        public void setListItems(List<InquiryManagerActivity_ListItem> listItems) {
            this.items = listItems;
        }

        public int getCount() {
            return this.items.size();
        }

        public Object getItem(int position) {
            return this.items.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            InquiryManagerActivity_ListView listView;

            if (convertView == null) {
                listView = new InquiryManagerActivity_ListView(this.context, this.items.get(position));
            } else {
                listView = (InquiryManagerActivity_ListView) convertView;
                listView.setMartName(this.items.get(position).getMartName());
                listView.setOrderDate(this.items.get(position).getOrderDate());
                listView.setProductName(this.items.get(position).getProductName());
            }
            return listView;
        }
    }
    /*
    구매내역의 정보가 담기는 리스트의 아이템 클래스
    */
    private class InquiryManagerActivity_ListItem {

        private String orderDate;
        private String productName;
        private String martName;

        private boolean selectable = true;

        public InquiryManagerActivity_ListItem(String orderDate, String productName, String martName) {

            this.orderDate = orderDate;
            this.martName = martName;
            this.productName = productName;

        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getMartName() {
            return martName;
        }

        public void setMartName(String martName) {
            this.martName = martName;
        }

        public String getProductName() { return this.productName; }

        public void setProductName(String productName) { this.productName = productName; }

        public String getOrderDate() {
            return orderDate;
        }
        public void setOrderDate(String orderDate) {
            this.orderDate = orderDate;
        }
    }
    /*
    구매내역을 리스트로 출력할 때 리스트를 객체화 해주는 클래스
    */
    private class InquiryManagerActivity_ListView extends LinearLayout {

        private TextView martName;
        private TextView orderDate;
        private TextView productName;

        public InquiryManagerActivity_ListView(Context context, InquiryManagerActivity_ListItem buyItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.inquiry_listitem, this, true);

            productName = (TextView)findViewById(R.id.product_name);
            productName.setText(buyItem.getProductName());

            martName = (TextView) findViewById(R.id.mart_name);
            martName.setText(buyItem.getMartName());

            orderDate = (TextView) findViewById(R.id.orderdate);
            orderDate.setText(buyItem.getOrderDate());

        }

        public void setProductName(String data) { productName.setText(data); }

        public void setMartName(String data) {
            martName.setText(data);
        }

        public void setOrderDate(String data) {
            orderDate.setText(data);
        }

    }

    public void onClickInquiry(View v) {

        String startDate = null;
        String endDate = null;
        String categoryName = "";
        String productName = "";
        String martName = "";


        final CheckBox orderDateCheckBox = (CheckBox)findViewById(R.id.orderdate_checkbox);
        CheckBox categoryCheckBox = (CheckBox)findViewById(R.id.category_checkbox);
        CheckBox productCheckBox = (CheckBox)findViewById(R.id.product_checkbox);
        CheckBox martCheckBox = (CheckBox)findViewById(R.id.mart_checkbox);

        final EditText orderDateStartEditText = (EditText)findViewById(R.id.start_orderdate_edit_text);
        EditText orderDateEndEditText = (EditText)findViewById(R.id.end_orderdate_edit_text);
        EditText categoryEditText = (EditText)findViewById(R.id.category_edit_text);
        EditText productEditText = (EditText)findViewById(R.id.product_name_edit_text);
        EditText martEditText = (EditText)findViewById(R.id.mart_name_edit_text);

        InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(orderDateStartEditText.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(orderDateEndEditText.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(categoryEditText.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(productEditText.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(martEditText.getWindowToken(), 0);

        if(orderDateCheckBox.isChecked()) {
            startDate = orderDateStartEditText.getText().toString();
            endDate = orderDateEndEditText.getText().toString();

            if(startDate.length() == 0 || endDate.length() == 0) {
                Toast.makeText(getApplicationContext(),"날짜를 입력하세요.",Toast.LENGTH_SHORT).show();

                startDate = null;
                endDate = null;
            }
        }

        if(categoryCheckBox.isChecked()) {
            categoryName = categoryEditText.getText().toString();

            if(categoryName.length() == 0 || categoryName.equals(" ")) {
                Toast.makeText(getApplicationContext(),"카테고리를 입력하세요.",Toast.LENGTH_SHORT).show();
                categoryName = null;
            }
        }

        if(productCheckBox.isChecked()) {
            productName = productEditText.getText().toString();

            if(productName.length() == 0 || productName.equals(" ")) {
                Toast.makeText(getApplicationContext(),"상품명을 입력하세요.",Toast.LENGTH_SHORT).show();
                productName = null;
            }
        }

        if(martCheckBox.isChecked()) {
            martName = martEditText.getText().toString();

            if(martName.length() == 0 || martName.equals(" ")) {
                Toast.makeText(getApplicationContext(),"마트 이름을 입력하세요.",Toast.LENGTH_SHORT).show();
                martName = null;
            }
        }

        listView1 = (ListView)findViewById(R.id.listView01);

        listAdapter = getListAdapter(getApplicationContext(), startDate, endDate, categoryName, productName, martName);

        if(listAdapter.getCount() == 0) {

            if(startDate != null || endDate != null || categoryName != null || productName != null || martName != null){
                if((categoryName != null && !categoryName.equals("") && !categoryName.equals(" ")) || (productName != null &&!productName.equals("") && !productName.equals(" ")) || (martName != null && !martName.equals("") && !martName.equals(" "))) {
                    Toast.makeText(getApplicationContext(), "검색 결과가 없습니다.\n키워드를 다시 엽력해보세요.", Toast.LENGTH_SHORT).show();
                }

                listView1.setAdapter(null);

            }
        }else {

            listView1.setAdapter(listAdapter);

        }

    }
    /*
    SQLiteOpenHelper를 상속하여 데이터베이스를 제어하는 클래스
    */
    private class InquiryManagerActivity_DBHelper extends SQLiteOpenHelper {

        public InquiryManagerActivity_DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onOpen(SQLiteDatabase db) {

        }
    }
    /*
    구매내역을 리스트뷰에 출력하는 메소드
    리스트뷰의 레이아웃에 맞춰 출력한다
    */
    private InquiryManager_ListAdapter getListAdapter(Context context, String startOrderDate, String endOrderDate, String categoryName, String productName, String martName) {
        InquiryManager_ListAdapter listAdapter = new InquiryManager_ListAdapter(context);
        List<InquiryManagerActivity_ListItem> listItem = new ArrayList<>();

        int readCount;
        String mart_name_and_location;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {
            if(startOrderDate != null) {
                Cursor cursor = getTableByOrderDate(startOrderDate, endOrderDate,categoryName,productName,martName);

                readCount = cursor.getCount();

                for(int i=0;i<readCount;i++) {
                    cursor.moveToNext();

                    mart_name_and_location = cursor.getString(2) + "\n" + cursor.getString(3);

                    listItem.add(new InquiryManagerActivity_ListItem(cursor.getString(0), cursor.getString(1), mart_name_and_location));
                }

                cursor.close();
            }else {

                Cursor cursor = getTableByNonOrderDate(categoryName, productName, martName);

                readCount = cursor.getCount();

                for(int i=0;i<readCount;i++) {
                    cursor.moveToNext();

                    mart_name_and_location = cursor.getString(2) + "\n" + cursor.getString(3);

                    listItem.add(new InquiryManagerActivity_ListItem(cursor.getString(0), cursor.getString(1), mart_name_and_location));
                }

                cursor.close();
            }

            listAdapter.setListItems(listItem);

        }

        db.close();

        return listAdapter;
    }
    /*
    DBHelper를 이용해 읽기전용으로 데이터베이스를 오픈
    */
    private boolean openReadableDatabase() {

        dbHelper = new InquiryManagerActivity_DBHelper(this);
        db = dbHelper.getReadableDatabase();

        return true;
    }

    private Cursor getTableByOrderDate(String start, String end, String categoryName, String productName, String martName) {
        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME + " as O, " + PRODUCT_TABLE_NAME + " as P, " + MART_TABLE_NAME + " as M";
        String[] columns = { "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE, "P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME ,"M." + MART_TABLE_ATTRIBUTE_NAME_MART_NAME ,"M." +  MART_TABLE_ATTRIBUTE_NAME_MART_LOCATION};
        String selection = "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID  + " and P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " LIKE '%"+productName+"%' and P." +
                PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " LIKE '%"+categoryName+"%' and P." + PRODUCT_TABLE_ATTRIBUTE_NAME_MART_ID + " = M." + MART_TABLE_ATTRIBUTE_NAME_MART_ID + " and M." + MART_TABLE_ATTRIBUTE_NAME_MART_NAME + " LIKE '%"+martName+"%' and" + " O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " between ? and ?";
        String[] selectionArgs = { start, end };
        String orderBy = "O." +ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        return cursor;
    }

    private Cursor getTableByNonOrderDate(String categoryName, String productName, String martName) {
        Cursor cursor;

        String table = ORDER_HISTORY_TABLE_NAME + " as O, " + PRODUCT_TABLE_NAME + " as P, " + MART_TABLE_NAME + " as M";
        String[] columns = { "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE, "P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME ,"M." + MART_TABLE_ATTRIBUTE_NAME_MART_NAME ,"M." +  MART_TABLE_ATTRIBUTE_NAME_MART_LOCATION};
        String selection = "O." + ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID  + " and P." + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " LIKE '%"+ productName+ "%' and P." +
                PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " LIKE '%"  + categoryName + "%' and P." + PRODUCT_TABLE_ATTRIBUTE_NAME_MART_ID + " = M." + MART_TABLE_ATTRIBUTE_NAME_MART_ID + " and M." + MART_TABLE_ATTRIBUTE_NAME_MART_NAME + " LIKE '%"+martName+ "%'";
        String orderBy = "O." +ORDER_HISTORY_TABLE_ATTRIBUTE_NAME_ORDER_DATE + " DESC";

        cursor = db.query(table,columns,selection,null,null,null,orderBy);

        return cursor;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inquiry_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int curId = item.getItemId();

        switch (curId) {
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
}