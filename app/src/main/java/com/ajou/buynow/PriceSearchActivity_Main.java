// PriceSearchActivity_Main.java
/*
	File Name : PriceSearchActivity_Main.java
    Function  : BUYNOW 어플리케이션에서 최저가 조회 액티비티의 클래스이다.
                큰 틀에서 기능은 다음과 같다
                    1) 카테고리, 상품명, 상품아이디를 통한 검색
                    2) 검색 시 Like 연산자를 이용한 스트링 매칭
                    3) 여러 마트 중에 가장 싼 가격의 상품과 그 가격, 그리고 마트이름과 위치를 리스트뷰로 뿌려준다.
	Author    : 김지환
	Date      : 2016/12/01
*/
package com.ajou.buynow;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PriceSearchActivity_Main extends AppCompatActivity{

    ListView listView1; // 최저가조회 액티비티의 메인 리스트뷰
    PriceSearchActivity_ListAdapter listAdapter; // 메인 리스트뷰의 어댑터

    public static final String TAG = "PriceSearchActivity"; // Log를 위한 TAG
    private static final String PACKAGE_NAME = "com.ajou.buynow"; // 데이터베이스를 Assets폴더에서 기기로 가져올때 디렉토리의 패키지 이름

    private SQLiteDatabase db; // 데이터베이스 객체
    private PriceSearchActivity_DatabaseHelper dbHelper; // 데이터베이스 헬퍼 객채
    private int DATABASE_VERSION = 1; // 데이터베이스 헬퍼 객체가 사용할 데이터베이스 버전

    private static String DATABASE_NAME = "BUYNOW.db"; // 데이터베이스 이름

    // 데이터베이스 스키마의 테이블 이름과 속성이 바뀌는걸 감안해 스트링 상수로 지정하였다
    private static String PRODUCT_TABLE_NAME = "PRODUCT";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID = "productID";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME = "productName";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_PRICE = "price";
    private static String PRODUCT_TABLE_ATTRIBUTE_NAME_MART_ID = "martID";

    private static String CATEGORY_TABLE_NAME = "CATEGORY";
    private static String CATEGORY_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME = "categoryName";
    private static String CATEGORY_TABLE_ATTRIBUTE_NAME_SUPER_CATEGORY_NAME = "superCategoryName";

    private static String MART_TABLE_NAME = "MART";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_ID = "martID";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_NAME = "martName";
    private static String MART_TABLE_ATTRIBUTE_NAME_MART_LOCATION = "martLocation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.price_search_main);

        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.price_search_parent_layout);

        ListView listView = (ListView)findViewById(R.id.listView01);

        final EditText editText = (EditText)findViewById(R.id.search_edit_text);

        RadioButton radioButton01 = (RadioButton)findViewById(R.id.radio_button_category);

        radioButton01.setChecked(true);

        loadDBfile(); // Assets 폴더에서 데이터베이스 파일을 기기로 옮긴다

        editText.clearFocus();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editText.clearFocus();
            }
        });
        final RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radio_group);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (radioGroup.getCheckedRadioButtonId()) {
                    case R.id.radio_button_category:
                        editText.setHint("카테고리를 입력하세요");
                        break;

                    case R.id.radio_button_product_name:
                        editText.setHint("상품명을 입력하세요");
                        break;

                    case R.id.radio_button_product_id:
                        editText.setHint("상품 아이디를 입력하세요");
                        break;

                    default:
                        break;
                }
            }
        });

        parentLayout.setOnContextClickListener(new View.OnContextClickListener() {
            @Override
            public boolean onContextClick(View v) {
                if(v.getId() != R.id.search_edit_text) {
                    editText.clearFocus();
                }

                return false;
            }
        });

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if(!editText.hasFocus()) {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }else {
                    InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputMethodManager.showSoftInput(editText, 0);
                }
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                listView1 = (ListView) findViewById(R.id.listView01); // 리스트뷰 찾기

                String inputData;

                EditText editText;
                RadioGroup radioGroup;

                editText = (EditText)findViewById(R.id.search_edit_text);
                radioGroup = (RadioGroup)findViewById(R.id.radio_group);

                inputData = editText.getText().toString();

                if(inputData.length() != 0 && !inputData.equals(" ")) {

                    switch (radioGroup.getCheckedRadioButtonId()) {
                        case R.id.radio_button_category:
                            listAdapter = getListAdapterByCategory(getApplicationContext(), inputData); // 리스트뷰 어댑터를 만든다
                            break;

                        case R.id.radio_button_product_name:
                            listAdapter = getListAdapterByProductName(getApplicationContext(), inputData);
                            break;

                        case R.id.radio_button_product_id:
                            listAdapter = getListAdapterByProductID(getApplicationContext(), inputData);
                            break;

                        default:
                            break;
                    }

                    if(listAdapter.getCount() == 0) {

                         Toast.makeText(getApplicationContext(), "검색 결과가 없습니다.\n키워드를 다시 입력해보세요.", Toast.LENGTH_SHORT).show();
                         listView1.setAdapter(null);

                    }else {

                        listView1.setAdapter(listAdapter); // 리스트뷰 어댑터를 셋팅

                        // 리스트뷰 아이템 클릭시 발생할 이벤트
                        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                EditText editText = (EditText)findViewById(R.id.search_edit_text);

                                editText.clearFocus();
                            }
                        });

                    }

                }else {
                    Toast.makeText(getApplicationContext(), "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });
    }
    /*
       Class Name  : PriceSearchActivity_DatabaseHelper
       Function    : SQLiteOpenHelper를 상속해 데이터베이스와 관련된 연산을 돕는다
    */
    private class PriceSearchActivity_DatabaseHelper extends SQLiteOpenHelper {

        public PriceSearchActivity_DatabaseHelper(Context context) {
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
       Class Name : PriceSearchActivity_ListAdapter
       Function   : 최저가 조회시 리스트로 뿌려줄때 사용되는 리스트뷰 어댑터 클래스이다
    */
    private class PriceSearchActivity_ListAdapter extends BaseAdapter {

        private Context context;
        private List<PriceSearchActivity_ListItem> items = new ArrayList<>();

        public PriceSearchActivity_ListAdapter(Context context) {
            this.context = context;
        }

        public void addItem(PriceSearchActivity_ListItem item) {
            this.items.add(item);
        }

        public void setListItems(List<PriceSearchActivity_ListItem> listItems) {
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

            PriceSearchActivity_ListView listView;

            if(convertView == null) {
                listView = new PriceSearchActivity_ListView(this.context, this.items.get(position));
            }else {
                listView = (PriceSearchActivity_ListView) convertView;

                listView.setIcon(this.items.get(position).getIcon());
                listView.setProductName(this.items.get(position).getProductName());
                listView.setMartName(this.items.get(position).getMartName());
                listView.setPrice(this.items.get(position).getPrice());

            }

            return listView;
        }
    }
    /*
       Class Name : PriceSearchActivity_ListItem
       Function   : 최저가 조회에서 리스트로 담기는 내용이 되는 리스트 아이템 클래스이다
    */
    private class PriceSearchActivity_ListItem {

        private int leftPeriodToBeCompared;

        private Drawable icon;

        private String productName;
        private String martName;
        private String price;

        private boolean selectable = true;

        public PriceSearchActivity_ListItem(Drawable icon, String productName, String martName, String price) {
            this.icon = icon;
            this.productName = productName;
            this.martName = martName;
            this.price = price;
        }

        public boolean isSelectable() {
            return selectable;
        }

        public void setSelectable(boolean selectable) {
            this.selectable = selectable;
        }

        public String getProductName() { return this.productName; }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getMartName() { return this.martName; }

        public void setMartName(String martName) {
            this.martName = martName;
        }

        public String getPrice() { return this.price; }

        public void setPrice(String price) {
            this.price = price;
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public void setIcon(Drawable icon) {
            this.icon = icon;
        }

    }
    /*
       Class Name : PriceSearchActivity_ListView
       Function   : 최저가 조회 시 리스트로 뿌려줄때 리스트 아이템을 객체화 시켜주는 리스트뷰 클래스이다
    */
    private class PriceSearchActivity_ListView extends LinearLayout {

        private ImageView icon;
        private TextView productName;
        private TextView martName;
        private TextView price;

        public PriceSearchActivity_ListView(Context context, PriceSearchActivity_ListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.price_search_listitem, this, true);

            icon = (ImageView)findViewById(R.id.icon);
            icon.setImageDrawable(aItem.getIcon());

            productName = (TextView)findViewById(R.id.productName_text_view);
            productName.setText(aItem.getProductName());

            martName = (TextView)findViewById(R.id.martName_text_view);
            martName.setText(aItem.getMartName());

            price = (TextView) findViewById(R.id.price_text_view);
            price.setText(aItem.getPrice());

        }

        public void setProductName(String data) {
            productName.setText(data);
        }

        public void setMartName(String data) { martName.setText(data); }

        public void setPrice(String data) { price.setText(data); }

        public void setIcon(Drawable icon) {
            this.icon.setImageDrawable(icon);
        }

    }
    /*
	   Function Name : openReadableDatabase
       Function      : 데이터베이스 헬퍼를 이용해 데이터베이스를 읽기전용으로 오픈한다.
	   Input         : None
	   Output        : 데이터베이스 핸들 생성 성공 여부
    */
    private boolean openReadableDatabase() {

        dbHelper = new PriceSearchActivity_DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        return true;
    }
    /*
	   Function Name : onClickSearchButton
       Function      : 최저가 조회 메인 화면에서 사용자가 검색어를 입력하고 돋보기 버튼을 클릭시
                       이벤트 생성을 받아 검색어에 해당하는 쿼리를 발생시켜 결과를 띄워주는 역할을 담당한다.
	   Input         : View
	   Output        : None
    */
    public void onClickSearchButton(View v) {

        listView1 = (ListView) findViewById(R.id.listView01); // 리스트뷰 찾기

        String inputData;

        EditText editText;
        RadioGroup radioGroup;

        editText = (EditText)findViewById(R.id.search_edit_text);
        radioGroup = (RadioGroup)findViewById(R.id.radio_group);

        InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        inputData = editText.getText().toString();

        editText.clearFocus();

        if(inputData.length() != 0 && !inputData.equals(" ")) {

            switch (radioGroup.getCheckedRadioButtonId()) {
                case R.id.radio_button_category:
                    listAdapter = getListAdapterByCategory(this, inputData); // 리스트뷰 어댑터를 만든다
                    break;

                case R.id.radio_button_product_name:
                    listAdapter = getListAdapterByProductName(this, inputData);
                    break;

                case R.id.radio_button_product_id:
                    listAdapter = getListAdapterByProductID(this, inputData);
                    break;

                default:
                    break;
            }

            if(listAdapter.getCount() == 0) {

                Toast.makeText(getApplicationContext(), "검색 결과가 없습니다.\n키워드를 다시 입력해보세요.", Toast.LENGTH_SHORT).show();
                listView1.setAdapter(null);

            }else {
                listView1.setAdapter(listAdapter); // 리스트뷰 어댑터를 셋팅

                // 리스트뷰 아이템 클릭시 발생할 이벤트
                listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        EditText editText = (EditText)findViewById(R.id.search_edit_text);

                        editText.clearFocus();

                    }
                });
            }

        }else {
            Toast.makeText(getApplicationContext(), "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
        }
    }
    /*
       Function Name : getListAdapterByCategory
       Function      : 사용자가 카테고리로 검색 할 때, 결과를 뿌려줄 리스트 어댑터를 알맞게 만들어 리턴한다.
       Input         : Context context, String categoryName
       Output        : PriceSearchActivity_ListAdapter
    */
    private PriceSearchActivity_ListAdapter getListAdapterByCategory(Context context, String categoryName) {

        PriceSearchActivity_ListAdapter listAdapter = new PriceSearchActivity_ListAdapter(context);
        List<PriceSearchActivity_ListItem> listItems = new ArrayList<>();

        Drawable icon;
        String productName;
        String martName;
        String martID;
        String martLocation;
        String lowestPrice;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {
            Cursor productTableCursor = getProductTableCursorByCategoryName(categoryName);

            int recordCount = productTableCursor.getCount();
            int product_table_product_name_column = productTableCursor.getColumnIndex(PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME);

            for (int i = 0; i < recordCount; i++) {
                productTableCursor.moveToNext();

                productName = productTableCursor.getString(product_table_product_name_column);

                categoryName = getCategoryNameByProductName(productName);

                icon = getIcon(categoryName);

                lowestPrice = getLowestProductPrice(productName)+"원";

                martID = getLowestPriceMartID(productName);

                martName = getMartName(martID);

                martLocation = getMartLocation(martID);

                martName = martName + "\n" + martLocation;

                listItems.add(new PriceSearchActivity_ListItem(icon,productName,martName,lowestPrice));
            }

            listAdapter.setListItems(listItems);

            productTableCursor.close();
        }

        db.close();

        return listAdapter;
    }
    /*
       Function Name : getListAdapterByProductName
       Function      : 사용자가 상품명으로 검색 할 때, 결과를 뿌려줄 리스트 어댑터를 알맞게 만들어 리턴한다.
       Input         : Context context, String productName
       Output        : PriceSearchActivity_ListAdapter
    */
    private PriceSearchActivity_ListAdapter getListAdapterByProductName(Context context, String productName) {

        PriceSearchActivity_ListAdapter listAdapter = new PriceSearchActivity_ListAdapter(context);
        List<PriceSearchActivity_ListItem> listItems = new ArrayList<>();

        Drawable icon;
        String categoryName;
        String martName;
        String martID;
        String martLocation;
        String lowestPrice;

        int readCount;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {
            Cursor cursor = getProductTableCursorByProductName(productName);

            readCount = cursor.getCount();

            for(int i=0;i<readCount;i++) {

                cursor.moveToNext();

                productName = cursor.getString(0);

                categoryName = getCategoryNameByProductName(productName);

                icon = getIcon(categoryName);

                lowestPrice = getLowestProductPrice(productName) + "원";

                martID = getLowestPriceMartID(productName);

                martName = getMartName(martID);

                martLocation = getMartLocation(martID);

                martName = martName + "\n" + martLocation;

                listItems.add(new PriceSearchActivity_ListItem(icon, productName, martName, lowestPrice));

            }

            cursor.close();
        }

        listAdapter.setListItems(listItems);

        db.close();

        return listAdapter;
    }
    /*
       Function Name : getListAdapterByProductID
       Function      : 사용자가 상품 아이디로 검색 할 때, 결과를 뿌려줄 리스트 어댑터를 알맞게 만들어 리턴한다.
       Input         : Context context, String productID
       Output        : PriceSearchActivity_ListAdapter
    */
    private PriceSearchActivity_ListAdapter getListAdapterByProductID(Context context, String productID) {
        PriceSearchActivity_ListAdapter listAdapter = new PriceSearchActivity_ListAdapter(context);
        List<PriceSearchActivity_ListItem> listItems = new ArrayList<>();

        Drawable icon;
        String productName;
        String categoryName;
        String martName;
        String martID;
        String martLocation;
        String price;

        Boolean isDatabaseOpen = openReadableDatabase();

        if (isDatabaseOpen) {

            productName = getProductName(productID);

            if(productName != null) {

                categoryName = getCategoryName(productID);

                icon = getIcon(categoryName);

                price = getLowestProductPrice(productName) + "원";

                martID = getLowestPriceMartID(productName);

                martName = getMartName(martID);

                martLocation = getMartLocation(martID);

                martName = martName + "\n" + martLocation;

                listItems.add(new PriceSearchActivity_ListItem(icon, productName, martName, price));

            }
        }

        listAdapter.setListItems(listItems);

        db.close();

        return listAdapter;
    }
    /*
       Function Name : getProductTableCursorByCategoryName
       Function      : 카테고리 이름을 입력받아 카테고리 이름을 포함하는(Like 연산자 쿼리) Product들의 이름을 뽑는다.
                       테이블에는 Product 이름만 들어있고, 이름들은 Distinct하게 뽑히도록 한다.
       Input         : String categoryName
       Output        : Cursor
    */
    private Cursor getProductTableCursorByCategoryName(String categoryName) {
        String SQL = "select DISTINCT " + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " from " + PRODUCT_TABLE_NAME + " where " + PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " like '%" + categoryName+ "%'";

        return db.rawQuery(SQL, null);
    }
    /*
       Function Name : getProductTableCursorByProductName
       Function      : 상품 이름을 입력받아 입력받은 상품 이름을 포함하는(Like 연산자 쿼리) Product들의 이름을 뽑는다.
                       테이블에는 Product 이름만 들어있고, 이름들은 Distinct하게 뽑히도록 한다.
       Input         : String productName
       Output        : Cursor
    */
    private Cursor getProductTableCursorByProductName(String productName) {
        String SQL = "select DISTINCT " + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " from " + PRODUCT_TABLE_NAME + " where " + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " like '%" + productName + "%'";

        return db.rawQuery(SQL, null);
    }
    /*
	   Function Name : getLowestPriceMartID
       Function      : 특정 상품 이름을 입력받아 여러 마트 중에 같은 상품 이름을 비교해 가장 가격이 낮은 물품을 가진
                       마트의 아이디를 리턴한다.
	   Input         : productName
	   Output        : String martID(최저가 상품을 가진 마트의 아이디)
    */
    private String getLowestPriceMartID(String productName) {
        String lowestPriceMartID;

        Cursor cursor;

        String table =  PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_MART_ID };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " = ?";
        String[] selectionArgs = { productName };
        String orderBy = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_PRICE + " ASC";

        cursor = db.query(table,columns,selection,selectionArgs,null,null,orderBy);

        if(cursor.getCount() != 0) {
            cursor.moveToNext();

            lowestPriceMartID = cursor.getString(0);
        }else {
            lowestPriceMartID = null;
        }

        cursor.close();

        return lowestPriceMartID;
    }
    /*
	   Function Name : getLowestProductPrice
       Function      : 특정 상품 이름을 입력받아 여러 마트 중에 같은 상품 이름을 비교해 가장 가격이 낮은 물품의 가격을 리턴한다.
	   Input         : productName
	   Output        : Price(최저 가격)
    */
    private String getLowestProductPrice(String productName) {
        String lowestProductPrice;

        Cursor cursor;

        String table =  PRODUCT_TABLE_NAME;
        String[] columns = { "MIN(" + PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_PRICE + ")" };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " = ?";
        String[] selectionArgs = { productName };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            lowestProductPrice = cursor.getString(0);

        }else {
            lowestProductPrice = null;
        }

        cursor.close();

        return lowestProductPrice;
    }
    /*
	   Function Name : getMartName
       Function      : 마트 아이디를 입력받아 마트 아이디에 해당하는 마트 이름을 리턴한다.
	   Input         : MartID
	   Output        : MartName
    */
    private String getMartName(String martID) {
        String martName;

        Cursor cursor;

        String table = MART_TABLE_NAME;
        String[] columns = { MART_TABLE_ATTRIBUTE_NAME_MART_NAME };
        String selection = MART_TABLE_ATTRIBUTE_NAME_MART_ID + " = ?";
        String[] selectionArgs = { martID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            martName = cursor.getString(0);

        }else {
            martName = null;
        }

        cursor.close();

        return martName;
    }
    /*
	   Function Name : getMartLocation
       Function      : 마트 아이디를 입력받아 마트 아이디에 해당하는 마트의 위치를 리턴한다.
	   Input         : MartID
	   Output        : MartLocation
    */
    private String getMartLocation(String martID) {
        String martLocation;

        Cursor cursor;

        String table = MART_TABLE_NAME;
        String[] columns = { MART_TABLE_ATTRIBUTE_NAME_MART_LOCATION };
        String selection = MART_TABLE_ATTRIBUTE_NAME_MART_ID + " = ?";
        String[] selectionArgs = { martID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            martLocation = cursor.getString(0);

        }else {
            martLocation = null;
        }

        cursor.close();

        return martLocation;
    }
    /*
       Function Name : getProductName
       Function      : 상품 아이디를 입력받아 상품의 상품명을 리턴한다.
       Input         : productID
       Output        : productName
    */
    private String getProductName(String productID) {
        String productName;

        Cursor cursor;

        String table = PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            productName = cursor.getString(0);

        } else {
            productName = null;
        }
        cursor.close();

        return productName;
    }
    /*
       Function Name : getCategoryName
       Function      : 상품 아이디를 입력받아 상품의 카테고리 명을 리턴한다.
       Input         : productID
       Output        : categoryName
    */
    private String getCategoryName(String productID) {
        String categoryName;

        Cursor cursor;

        String table = PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_ID + " = ?";
        String[] selectionArgs = { productID };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            categoryName = cursor.getString(0);

        }else {
            categoryName = null;
        }

        cursor.close();

        return categoryName;
    }
    /*
	   Function Name : getCategoryNameByProductName
       Function      : 상품명을 입력받아 상품의 카테고리 명을 리턴한다.
	   Input         : productName
	   Output        : categoryName
    */
    private String getCategoryNameByProductName(String productName) {
        String categoryName;

        Cursor cursor;

        String table = PRODUCT_TABLE_NAME;
        String[] columns = { PRODUCT_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME };
        String selection = PRODUCT_TABLE_ATTRIBUTE_NAME_PRODUCT_NAME + " like '%" + productName + "%'";

        cursor = db.query(table,columns,selection,null,null,null,null);

        if(cursor.getCount() != 0) {
            cursor.moveToNext();

            categoryName = cursor.getString(0);
        }else {
            categoryName = null;
        }

        cursor.close();

        return categoryName;
    }
    /*
	   Function Name : getSuperCategoryName
       Function      : 특정 카테고리 이름에 대한 상위 카테고리 이름을 리턴한다.
	   Input         : CategoryName
	   Output        : SuperCategoryName
    */
    private String getSuperCategoryName(String categoryName) {
        String superCategoryName;

        Cursor cursor;

        String table = CATEGORY_TABLE_NAME;
        String[] columns = { CATEGORY_TABLE_ATTRIBUTE_NAME_SUPER_CATEGORY_NAME };
        String selection = CATEGORY_TABLE_ATTRIBUTE_NAME_CATEGORY_NAME + " = ?";
        String[] selectionArgs = { categoryName };

        cursor = db.query(table,columns,selection,selectionArgs,null,null,null);

        if(cursor.getCount() != 0) {

            cursor.moveToNext();

            superCategoryName = cursor.getString(0);

        }else {
            superCategoryName = null;
        }

        cursor.close();

        return superCategoryName;
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
       Function      : 최저가 조회 액티비티 생성시 메뉴를 생성한다.
	   Input         : Menu
	   Output        : Boolean
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_price_main, menu);
        return true;
    }
    /*
	   Function Name : onOptionItemSelected
       Function      : 최저가 조회 화면의 타이틀바에 위치한 메뉴의 클릭 이벤트를 담당한다.
	   Input         : Menu
	   Output        : Boolean
    */
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