package com.icmonroe.calendarview;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Ian Monroe on 11/6/14.
 */
public class CalendarView extends ViewPager {

    Calendar today = Calendar.getInstance();
    Calendar selectedDay = today;
    CalendarViewAdapter calendarViewAdapter;
    boolean showMonthTitle = true;
    boolean showDaysOfWeekRow = true;
    boolean includeYearInTitle = true;
    int numberOfMonthsBefore = 10;
    int numberOfMonthsAfter = 10;
    int rowHeight;
    int intrinsicHeight;
    int height;
    int primaryColor = 0xFF1E88E5;

    public CalendarView(Context context) {
        super(context);
        sharedConstructor();
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor();
    }

    private void sharedConstructor(){
        rowHeight = (int) getContext().getResources().getDimension(R.dimen.calendar_day_height);
        height = intrinsicHeight = (rowHeight*8);
        setOnPageChangeListener(new ChangeListener());
        setAdapter(calendarViewAdapter = new CalendarViewAdapter());
        setDaySelected(today);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setPrimaryColor(int color){
        primaryColor = color;
    }

    public void setNumberOfMonthsBefore(int i){
        numberOfMonthsBefore = i;
        getAdapter().notifyDataSetChanged();
        setDaySelected(today);
    }

    public void setNumberOfMonthsAfter(int i){
        numberOfMonthsAfter = i;
        getAdapter().notifyDataSetChanged();
        setDaySelected(today);
    }

    public void setShowMonthTitle(boolean show){ showMonthTitle = show; }

    public void setShowDaysOfWeekRow(boolean show){ showDaysOfWeekRow = show; }

    public void setIncludeYearInTitle(boolean include){ includeYearInTitle = include; }

    private CalendarViewListener calendarViewListener;

    public void setCalendarViewListener(CalendarViewListener listener){
        calendarViewListener = listener;
    }

    public void setMonth(int year,int month){
        Calendar calendar = Calendar.getInstance();
        calendar.set(year,month,1);
        setMonth(calendar);
    }

    public void setMonth(Calendar calendar){
        int monthsApart = (today.get(Calendar.YEAR) - calendar.get(Calendar.YEAR)) * 12;
        monthsApart +=  (today.get(Calendar.MONTH) - calendar.get(Calendar.MONTH));
        setCurrentItem(monthsApart+numberOfMonthsBefore,false);
    }

    public void setDaySelected(Calendar calendar){
        setMonth(calendar);
        calendarViewAdapter.setDaySelected(calendar);
    }

    public Calendar getSelectedDay(){
        return selectedDay;
    }

    private Calendar getCurrentCalendarMonth(int position){
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                1
        );
        currentCalendar.add(Calendar.MONTH,position-numberOfMonthsBefore);
        return currentCalendar;
    }

    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    public static interface CalendarViewListener{
        public void onDaySelected(Calendar calendar);
        public void onMonthSelected(Calendar calendar);
    }

    private class CalendarViewAdapter extends PagerAdapter{

        CalendarMonthView calendarMonthView;

        @Override
        public int getCount() {
            return numberOfMonthsBefore+1+numberOfMonthsAfter;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        int lastPosition = 0;
        Calendar defaultDay;

        @Override
        public Object instantiateItem(ViewGroup container, int position){

            calendarMonthView = new CalendarMonthView(getContext());
            lastPosition = position;
            calendarMonthView.setCalendarDate(getCurrentCalendarMonth(position));
            if(defaultDay!=null && defaultDay.get(Calendar.MONTH)==position){
                calendarMonthView.setDaySelected(defaultDay);
                defaultDay = null; // after we found it make it no longer usable in future
            }

            container.addView(calendarMonthView);
            return calendarMonthView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object){
            container.removeView((View) object);
        }

        public void setDaySelected(Calendar calendar) {
            defaultDay = calendar;
            notifyDataSetChanged();
        }
    }

    private class ChangeListener implements OnPageChangeListener{

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        int lastPage = 0;

        @Override
        public void onPageSelected(int position) {
            Calendar monthSelected = getCurrentCalendarMonth(position);
            intrinsicHeight = getRowsForMonth(monthSelected,showMonthTitle,showDaysOfWeekRow) * rowHeight;
            if(calendarViewListener!=null){
                calendarViewListener.onMonthSelected(monthSelected);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public class CalendarMonthView extends LinearLayout{

        TextView monthTitleRow;
        View daysOfWeekRow;
        GridView gridView;
        Calendar calendarOfMonth;
        CalendarDayView lastSelectedDayView;
        int daysBeforeFirstDay;

        public CalendarMonthView(Context context) {
            super(context);
            LayoutInflater.from(context).inflate(R.layout.view_calendar_month, this, true);
            gridView = (GridView) findViewById(R.id.calendar_month_gridview);
            monthTitleRow = (TextView) findViewById(R.id.month_title_row);
            daysOfWeekRow = findViewById(R.id.days_of_week_row);
        }

        public void setCalendarDate(Calendar calendar){
            calendarOfMonth = calendar;
            gridView.setAdapter(new CalendarMonthAdapter());
            daysBeforeFirstDay = getDaysBeforeFirstDay(calendarOfMonth);
            monthTitleRow.setText(getTitleForMonth(calendarOfMonth,includeYearInTitle));
            monthTitleRow.setVisibility(showMonthTitle ? VISIBLE : GONE);
            daysOfWeekRow.setVisibility(showDaysOfWeekRow ? VISIBLE : GONE);
        }

        public void setDaySelected(Calendar calendar) {
            CalendarMonthAdapter calendarMonthAdapter = (CalendarMonthAdapter) gridView.getAdapter();
            calendarMonthAdapter.setDaySelected(calendar);
        }

        class CalendarMonthAdapter extends BaseAdapter{

            @Override
            public int getCount() {
                return daysBeforeFirstDay + calendarOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            int defaultDay = 1;

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                CalendarDayView calendarDayView;
                if(view!=null){
                    calendarDayView = (CalendarDayView) view;
                }else{
                    calendarDayView = new CalendarDayView(getContext());
                }

                int dayOfMonth = i+1-daysBeforeFirstDay;

                calendarDayView.setDay(dayOfMonth);
                if(dayOfMonth==defaultDay){
                    if(lastSelectedDayView!=null) lastSelectedDayView.setDeselected();
                    lastSelectedDayView = calendarDayView;
                    calendarDayView.setSelected();
                }else calendarDayView.setDeselected();

                return calendarDayView;
            }

            public void setDaySelected(Calendar calendar) {
                defaultDay = calendar!=null ? calendar.get(Calendar.DAY_OF_MONTH) : 1;
                notifyDataSetChanged();
            }

        }

        class CalendarDayView extends FrameLayout implements OnClickListener{

            TextView dayText;
            AdaptableCircleView dayCircle;
            int dayOfMonth = -1;

            public CalendarDayView(Context context) {
                super(context);
                LayoutInflater.from(context).inflate(R.layout.view_calendar_day, this, true);
                dayText = (TextView) findViewById(R.id.calendar_day_text);
                dayCircle = (AdaptableCircleView) findViewById(R.id.calendar_day_circle);
                setDeselected();
                setOnClickListener(this);
            }

            public void setDay(int day){
                if(day>0) {
                    dayOfMonth = day;
                    dayText.setText(dayOfMonth + "");
                    setClickable(true);
                }else{
                    setVisibility(INVISIBLE);
                    dayText.setText("");
                    setClickable(false);
                }
            }

            public Calendar getDay(){
                Calendar calendar = Calendar.getInstance();
                calendar.set(
                        calendarOfMonth.get(Calendar.YEAR),
                        calendarOfMonth.get(Calendar.MONTH),
                        dayOfMonth,0,0,0
                );
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }


            public void setAsToday(){
                dayText.setTextColor(Color.WHITE);
                dayCircle.setBackgroundColor(primaryColor);
                dayCircle.setPercentage(0.0f);
            }

            public void setAsHavingEvent() {
                dayText.setTextColor(Color.BLACK);
                dayCircle.setBackgroundColor(Color.LTGRAY);
                dayCircle.setInsetPadding(1);
                dayCircle.setForegroundColor(Color.WHITE);
                dayCircle.setPercentage(1.0f);
            }

            public void setAsNotHavingEvent(){
                dayText.setTextColor(Color.BLACK);
                dayCircle.setBackgroundColor(Color.WHITE);
            }

            public void setSelected(){
                if(sameDay(getDay(), today)) setAsToday();
                else {
                    dayText.setTextColor(Color.BLACK);
                    dayCircle.setBackgroundColor(Color.LTGRAY);
                    dayCircle.setPercentage(0.0f);
                }
            }

            public void setDeselected(){
                Calendar day = getDay();
                // If this day is today, mark it as so
                if(sameDay(day, today)) setAsToday();
                    // If this day has event, mark it as so
                else if(hasEvent(day)) setAsHavingEvent();
                    // If this day has event, mark it as so
                else setAsNotHavingEvent();
            }

            @Override
            public void onClick(View view) {
                if(lastSelectedDayView!=null) lastSelectedDayView.setDeselected();
                lastSelectedDayView = this;
                setSelected();

                if(calendarViewListener!=null){
                    calendarViewListener.onDaySelected(selectedDay=getDay());
                }
            }

        }

    }

    EventIndicator eventIndicator;

    public void setEventIndicator(EventIndicator indicator){
        eventIndicator = indicator;
    }

    public static interface EventIndicator{
        public boolean hasEvent(Calendar day);
    }

    private boolean hasEvent(Calendar day){
        return eventIndicator!=null ? eventIndicator.hasEvent(day) : false;
    }

    public static boolean sameDay(Calendar first,Calendar second){
        return first.get(Calendar.DAY_OF_YEAR)==second.get(Calendar.DAY_OF_YEAR)
                && first.get(Calendar.YEAR)==second.get(Calendar.YEAR);
    }

    public static int getRowsForMonth(Calendar month,boolean hasTitle,boolean hasDaysOfWeek){
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int additionalRows = (hasTitle ? 1 : 0) + (hasDaysOfWeek ? 1 : 0);
        return (int) Math.ceil( ((daysInMonth+getDaysBeforeFirstDay(month)) / 7.0) + additionalRows);
    }

    private static int getDaysBeforeFirstDay(Calendar month){
        Calendar cal=Calendar.getInstance();
        cal.set(
                month.get(Calendar.YEAR),
                month.get(Calendar.MONTH),
                1
        );
        return cal.get(Calendar.DAY_OF_WEEK) - 1;
    }

    private static String getTitleForMonth(Calendar calendar,boolean includeYear){
        SimpleDateFormat formatMonth = new SimpleDateFormat("MMMM"+(includeYear ? " yyyy" : ""), Locale.ENGLISH);
        return formatMonth.format(calendar.getTime());
    }

}
