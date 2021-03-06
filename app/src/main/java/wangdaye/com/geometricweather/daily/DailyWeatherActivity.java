package wangdaye.com.geometricweather.daily;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.basic.GeoActivity;
import wangdaye.com.geometricweather.basic.models.Location;
import wangdaye.com.geometricweather.basic.models.weather.Daily;
import wangdaye.com.geometricweather.basic.models.weather.Weather;
import wangdaye.com.geometricweather.daily.adapter.DailyWeatherAdapter;
import wangdaye.com.geometricweather.db.DatabaseHelper;
import wangdaye.com.geometricweather.settings.SettingsOptionManager;
import wangdaye.com.geometricweather.ui.widgets.insets.FitBottomSystemBarRecyclerView;
import wangdaye.com.geometricweather.ui.widgets.insets.FitBottomSystemBarViewPager;
import wangdaye.com.geometricweather.utils.DisplayUtils;
import wangdaye.com.geometricweather.utils.helpters.AsyncHelper;
import wangdaye.com.geometricweather.utils.managers.ThemeManager;

/**
 * Daily weather activity.
 * */

public class DailyWeatherActivity extends GeoActivity {

    private CoordinatorLayout mContainer;
    private Toolbar mToolbar;
    private TextView mTitle;
    private TextView mSubtitle;
    private TextView mIndicator;

    private String mFormattedId;
    private int mPosition;

    public static final String KEY_FORMATTED_LOCATION_ID = "FORMATTED_LOCATION_ID";
    public static final String KEY_CURRENT_DAILY_INDEX = "CURRENT_DAILY_INDEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_daily);
        initData();
        initWidget();
    }

    @Override
    public View getSnackbarContainer() {
        return mContainer;
    }

    private void initData() {
        mFormattedId = getIntent().getStringExtra(KEY_FORMATTED_LOCATION_ID);
        mPosition = getIntent().getIntExtra(KEY_CURRENT_DAILY_INDEX, 0);
    }

    private void initWidget() {
        mContainer = findViewById(R.id.activity_weather_daily_container);

        mToolbar = findViewById(R.id.activity_weather_daily_toolbar);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mTitle = findViewById(R.id.activity_weather_daily_title);
        mSubtitle = findViewById(R.id.activity_weather_daily_subtitle);
        mIndicator = findViewById(R.id.activity_weather_daily_indicator);
        if (!SettingsOptionManager.getInstance(this).getLanguage().isChinese()){
            mSubtitle.setVisibility(View.GONE);
        }

        String formattedId = mFormattedId;
        AsyncHelper.runOnIO(emitter -> {
            Location location = null;

            if (!TextUtils.isEmpty(formattedId)) {
                location = DatabaseHelper.getInstance(this).readLocation(formattedId);
            }
            if (location == null) {
                location = DatabaseHelper.getInstance(this).readLocationList().get(0);
            }

            location.setWeather(DatabaseHelper.getInstance(this).readWeather(location));
            emitter.send(location);
        }, (AsyncHelper.Callback<Location>) location -> {
            if (location == null) {
                finish();
                return;
            }

            Weather weather = location.getWeather();
            if (weather == null) {
                finish();
                return;
            }

            selectPage(
                    weather.getDailyForecast().get(mPosition),
                    location.getTimeZone(),
                    mPosition,
                    weather.getDailyForecast().size()
            );

            List<View> viewList = new ArrayList<>(weather.getDailyForecast().size());
            List<String> titleList = new ArrayList<>(weather.getDailyForecast().size());

            for (int i = 0; i < weather.getDailyForecast().size(); i ++) {
                Daily d = weather.getDailyForecast().get(i);

                FitBottomSystemBarRecyclerView recyclerView = new FitBottomSystemBarRecyclerView(this);
                recyclerView.setClipToPadding(false);
                DailyWeatherAdapter dailyWeatherAdapter = new DailyWeatherAdapter(this, d, 3);
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
                gridLayoutManager.setSpanSizeLookup(dailyWeatherAdapter.spanSizeLookup);
                recyclerView.setAdapter(dailyWeatherAdapter);
                recyclerView.setLayoutManager(gridLayoutManager);

                viewList.add(recyclerView);
                titleList.add(String.valueOf(i + 1));
            }

            FitBottomSystemBarViewPager pager = findViewById(R.id.activity_weather_daily_pager);
            pager.setAdapter(new FitBottomSystemBarViewPager.FitBottomSystemBarPagerAdapter(pager, viewList, titleList));
            pager.setPageMargin((int) DisplayUtils.dpToPx(this, 1));
            pager.setPageMarginDrawable(new ColorDrawable(ThemeManager.getInstance(this).getLineColor(this)));
            pager.setCurrentItem(mPosition);
            pager.clearOnPageChangeListeners();
            pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // do nothing.
                }

                @Override
                public void onPageSelected(int position) {
                    selectPage(
                            weather.getDailyForecast().get(position),
                            location.getTimeZone(),
                            position,
                            weather.getDailyForecast().size()
                    );
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // do nothing.
                }
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void selectPage(Daily daily, TimeZone timeZone, int position, int size) {
        mTitle.setText(daily.getDate(getString(R.string.date_format_widget_long)));
        mSubtitle.setText(daily.getLunar());

        mToolbar.setContentDescription(mTitle.getText() + ", " + mSubtitle.getText());

        if (timeZone != null && daily.isToday(timeZone)) {
            mIndicator.setText(getString(R.string.today));
        } else {
            mIndicator.setText((position + 1) + "/" + size);
        }
    }
}
