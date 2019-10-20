package wangdaye.com.geometricweather.main.ui.controller;

import android.animation.AnimatorSet;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.Size;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.cardview.widget.CardView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Objects;

import wangdaye.com.geometricweather.R;
import wangdaye.com.geometricweather.basic.model.location.Location;
import wangdaye.com.geometricweather.basic.model.weather.Daily;
import wangdaye.com.geometricweather.basic.model.weather.Weather;
import wangdaye.com.geometricweather.main.ui.MainColorPicker;
import wangdaye.com.geometricweather.resource.ResourceHelper;
import wangdaye.com.geometricweather.resource.provider.ResourceProvider;
import wangdaye.com.geometricweather.ui.widget.moon.MoonPhaseView;
import wangdaye.com.geometricweather.ui.widget.moon.SunMoonView;
import wangdaye.com.geometricweather.ui.widget.weatherView.WeatherView;

public class SunMoonController extends AbstractMainItemController {

    private CardView card;

    private TextView title;
    private TextView phaseText;
    private MoonPhaseView phaseView;
    private SunMoonView sunMoonView;

    private RelativeLayout sunContainer;
    private TextView sunTxt;
    private RelativeLayout moonContainer;
    private TextView moonTxt;

    @NonNull private WeatherView weatherView;
    @Nullable private Weather weather;

    @Size(2) private float[] startTimes;
    @Size(2) private float[] endTimes;
    @Size(2) private float[] currentTimes;
    @Size(2) private float[] animCurrentTimes;
    private int phaseAngle;

    private boolean executeEnterAnimation;
    @Size(3) private AnimatorSet[] attachAnimatorSets;

    public SunMoonController(@NonNull Activity activity, @NonNull WeatherView weatherView,
                             @NonNull ResourceProvider provider, @NonNull MainColorPicker picker,
                             @Px float cardMarginsVertical, @Px float cardMarginsHorizontal,
                             @Px float cardRadius) {
        super(activity, activity.findViewById(R.id.container_main_sun_moon), provider, picker,
                cardMarginsVertical, cardMarginsHorizontal, cardRadius);

        this.card = view.findViewById(R.id.container_main_sun_moon);
        this.title = view.findViewById(R.id.container_main_sun_moon_title);
        this.phaseText = view.findViewById(R.id.container_main_sun_moon_phaseText);
        this.phaseView = view.findViewById(R.id.container_main_sun_moon_phaseView);
        this.sunMoonView = view.findViewById(R.id.container_main_sun_moon_controlView);
        this.sunContainer = view.findViewById(R.id.container_main_sun_moon_sunContainer);
        this.sunTxt = view.findViewById(R.id.container_main_sun_moon_sunrise_sunset);
        this.moonContainer = view.findViewById(R.id.container_main_sun_moon_moonContainer);
        this.moonTxt = view.findViewById(R.id.container_main_sun_moon_moonrise_moonset);

        this.weatherView = weatherView;
        this.executeEnterAnimation = true;
        this.attachAnimatorSets = new AnimatorSet[] {null, null, null};
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindView(@NonNull Location location) {
        if (location.getWeather() != null && location.getWeather().getDailyForecast().size() != 0) {
            weather = location.getWeather();

            if (!weather.getDailyForecast().get(0).sun().isValid()) {
                return;
            }

            ensureTime(weather);
            ensurePhaseAngle(weather);

            card.setCardBackgroundColor(picker.getRootColor(context));

            title.setTextColor(weatherView.getThemeColors(picker.isLightTheme())[0]);

            if (!weather.getDailyForecast().get(0).getMoonPhase().isValid()) {
                phaseText.setVisibility(View.GONE);
                phaseView.setVisibility(View.GONE);
            } else {
                phaseText.setVisibility(View.VISIBLE);
                phaseText.setTextColor(picker.getTextContentColor(context));
                phaseText.setText(weather.getDailyForecast().get(0).getMoonPhase().getMoonPhase(context));
                phaseView.setVisibility(View.VISIBLE);
                phaseView.setColor(
                        ContextCompat.getColor(context, R.color.colorTextContent_dark),
                        ContextCompat.getColor(context, R.color.colorTextContent_light),
                        picker.getTextContentColor(context)
                );
            }

            sunMoonView.setSunDrawable(ResourceHelper.getSunDrawable(provider));
            sunMoonView.setMoonDrawable(ResourceHelper.getMoonDrawable(provider));

            if (executeEnterAnimation) {
                sunMoonView.setTime(startTimes, endTimes, startTimes);
                sunMoonView.setDayIndicatorRotation(0);
                sunMoonView.setNightIndicatorRotation(0);
                phaseView.setSurfaceAngle(0);
            } else {
                sunMoonView.setTime(startTimes, endTimes, currentTimes);
                sunMoonView.setDayIndicatorRotation(0);
                sunMoonView.setNightIndicatorRotation(0);
                phaseView.setSurfaceAngle(phaseAngle);
            }
            int[] themeColors = weatherView.getThemeColors(picker.isLightTheme());
            if (picker.isLightTheme()) {
                sunMoonView.setColors(
                        themeColors[1],
                        ColorUtils.setAlphaComponent(themeColors[1], (int) (0.66 * 255)),
                        ColorUtils.setAlphaComponent(themeColors[1], (int) (0.33 * 255)),
                        picker.getRootColor(context),
                        true
                );
            } else {
                sunMoonView.setColors(
                        themeColors[1],
                        ColorUtils.setAlphaComponent(themeColors[1], (int) (0.5 * 255)),
                        ColorUtils.setAlphaComponent(themeColors[1], (int) (0.2 * 255)),
                        picker.getRootColor(context),
                        false
                );
            }

            if (weather.getDailyForecast().get(0).sun().isValid()) {
                sunContainer.setVisibility(View.VISIBLE);
                sunTxt.setText(
                        weather.getDailyForecast().get(0).sun().getRiseTime(context) + "↑"
                                + " / "
                                + weather.getDailyForecast().get(0).sun().getSetTime(context) + "↓"
                );
            } else {
                sunContainer.setVisibility(View.GONE);
            }
            if (weather.getDailyForecast().get(0).moon().isValid()) {
                moonContainer.setVisibility(View.VISIBLE);
                moonTxt.setText(
                        weather.getDailyForecast().get(0).moon().getRiseTime(context) + "↑"
                                + " / "
                                + weather.getDailyForecast().get(0).moon().getSetTime(context) + "↓"
                );
            } else {
                moonContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onEnterScreen() {
        if (executeEnterAnimation && weather != null) {
            executeEnterAnimation = false;

            ValueAnimator timeDay = ValueAnimator.ofObject(new FloatEvaluator(), startTimes[0], currentTimes[0]);
            timeDay.addUpdateListener(animation -> {
                animCurrentTimes[0] = (Float) animation.getAnimatedValue();
                sunMoonView.setTime(startTimes, endTimes, animCurrentTimes);
            });

            double totalRotationDay = 360.0 * 7 * (currentTimes[0] - startTimes[0]) / (endTimes[0] - startTimes[0]);
            ValueAnimator rotateDay = ValueAnimator.ofObject(
                    new FloatEvaluator(), 0, (int) (totalRotationDay - totalRotationDay % 360)
            );
            rotateDay.addUpdateListener(animation ->
                    sunMoonView.setDayIndicatorRotation((Float) animation.getAnimatedValue())
            );

            attachAnimatorSets[0] = new AnimatorSet();
            attachAnimatorSets[0].playTogether(timeDay, rotateDay);
            attachAnimatorSets[0].setInterpolator(new OvershootInterpolator(1f));
            attachAnimatorSets[0].setDuration(getPathAnimatorDuration(0));
            attachAnimatorSets[0].start();

            ValueAnimator timeNight = ValueAnimator.ofObject(new FloatEvaluator(), startTimes[1], currentTimes[1]);
            timeNight.addUpdateListener(animation -> {
                animCurrentTimes[1] = (Float) animation.getAnimatedValue();
                sunMoonView.setTime(startTimes, endTimes, animCurrentTimes);
            });

            double totalRotationNight = 360.0 * 4 * (currentTimes[1] - startTimes[1]) / (endTimes[1] - startTimes[1]);
            ValueAnimator rotateNight = ValueAnimator.ofObject(
                    new FloatEvaluator(), 0, (int) (totalRotationNight - totalRotationNight % 360)
            );
            rotateNight.addUpdateListener(animation ->
                    sunMoonView.setNightIndicatorRotation(-1 * (Float) animation.getAnimatedValue())
            );

            attachAnimatorSets[1] = new AnimatorSet();
            attachAnimatorSets[1].playTogether(timeNight, rotateNight);
            attachAnimatorSets[1].setInterpolator(new OvershootInterpolator(1f));
            attachAnimatorSets[1].setDuration(getPathAnimatorDuration(1));
            attachAnimatorSets[1].start();

            if (phaseAngle > 0) {
                ValueAnimator moonAngle = ValueAnimator.ofObject(new FloatEvaluator(), 0, phaseAngle);
                moonAngle.addUpdateListener(animation ->
                        phaseView.setSurfaceAngle((Float) animation.getAnimatedValue())
                );

                attachAnimatorSets[2] = new AnimatorSet();
                attachAnimatorSets[2].playTogether(moonAngle);
                attachAnimatorSets[2].setInterpolator(new DecelerateInterpolator());
                attachAnimatorSets[2].setDuration(getPhaseAnimatorDuration());
                attachAnimatorSets[2].start();
            }
        }
    }

    @Override
    public void onDestroy() {
        for (int i = 0; i < attachAnimatorSets.length; i ++) {
            if (attachAnimatorSets[i] != null && attachAnimatorSets[i].isRunning()) {
                attachAnimatorSets[i].cancel();
            }
            attachAnimatorSets[i] = null;
        }
    }

    private void ensureTime(@NonNull Weather weather) {
        Daily today = weather.getDailyForecast().get(0);
        Daily tomorrow = weather.getDailyForecast().get(1);

        Calendar calendar = Calendar.getInstance();
        int currentTime = SunMoonView.decodeTime(calendar);

        calendar.setTime(Objects.requireNonNull(today.sun().getRiseDate()));
        int sunriseTime = SunMoonView.decodeTime(calendar);
        
        calendar.setTime(Objects.requireNonNull(today.sun().getSetDate()));
        int sunsetTime = SunMoonView.decodeTime(calendar);

        startTimes = new float[2];
        endTimes = new float[2];
        currentTimes = new float[] {currentTime, currentTime};

        // sun.
        startTimes[0] = sunriseTime;
        endTimes[0] = sunsetTime;
        
        // moon.
        if (!today.moon().isValid() || !tomorrow.moon().isValid()) {
            // do not have moonrise and moonset data.
            if (currentTime < sunriseTime) {
                // predawn. --> moon move from [sunset of yesterday] to [sunrise of today].
                calendar.setTime(Objects.requireNonNull(
                        today.sun().getSetDate()
                ));
                startTimes[1] = SunMoonView.decodeTime(calendar) - 24 * 60;
                endTimes[1] = sunriseTime;
            } else {
                // moon move from [sunset of today] to [sunrise of tomorrow]
                calendar.setTime(Objects.requireNonNull(
                        today.sun().getSetDate()
                ));
                startTimes[1] = SunMoonView.decodeTime(calendar);

                calendar.setTime(Objects.requireNonNull(
                        tomorrow.sun().getRiseDate()
                ));
                endTimes[1] = SunMoonView.decodeTime(calendar) + 24 * 60;
            }
        } else {
            // have moonrise and moonset data.
            if (currentTime < sunriseTime) {
                // predawn. --> moon move from [moonrise of yesterday] to [moonset of today].
                calendar.setTime(Objects.requireNonNull(
                        today.moon().getRiseDate()
                ));
                startTimes[1] = SunMoonView.decodeTime(calendar) - 24 * 60;

                calendar.setTime(Objects.requireNonNull(
                        today.moon().getSetDate()
                ));
                endTimes[1] = SunMoonView.decodeTime(calendar);
                if (endTimes[1] < startTimes[1]) {
                    endTimes[1] += 24 * 60;
                }
            } else {
                // moon move from [moonrise of today] to [moonset of tomorrow].
                calendar.setTime(Objects.requireNonNull(
                        today.moon().getRiseDate()
                ));
                startTimes[1] = SunMoonView.decodeTime(calendar);

                calendar.setTime(Objects.requireNonNull(
                        tomorrow.moon().getSetDate()
                ));
                endTimes[1] = SunMoonView.decodeTime(calendar);
                if (endTimes[1] < startTimes[1]) {
                    endTimes[1] += 24 * 60;
                }
            }
        }

        animCurrentTimes = new float[] {currentTimes[0], currentTimes[1]};
    }

    private void ensurePhaseAngle(@NonNull Weather weather) {
        Integer angle = weather.getDailyForecast().get(0).getMoonPhase().getAngle();
        phaseAngle = angle == null ? 0 : angle;
    }

    private long getPathAnimatorDuration(int index) {
        long duration = (long) Math.max(
                1000 + 3000.0
                        * (currentTimes[index] - startTimes[index])
                        / (endTimes[index] - startTimes[index]),
                0
        );
        return Math.min(duration, 4000);
    }

    private long getPhaseAnimatorDuration() {
        long duration = (long) Math.max(0, phaseAngle / 360.0 * 1000 + 1000);
        return Math.min(duration, 2000);
    }
}
