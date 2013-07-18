/**
 * Fahrgemeinschaft / Ridesharing App
 * Copyright (c) 2013 by it's authors.
 * Some rights reserved. See LICENSE..
 *
 */

package de.fahrgemeinschaft;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.teleportr.Ride;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;

import de.fahrgemeinschaft.util.ButtonImageButton;
import de.fahrgemeinschaft.util.EditTextImageButton;
import de.fahrgemeinschaft.util.ReoccuringWeekDaysView;

public class EditRideFragment2 extends SherlockFragment 
        implements OnDateSetListener, OnTimeSetListener {

    private Ride ride; 
    private ButtonImageButton date;
    private ButtonImageButton time;
    private ReoccuringWeekDaysView reoccur;
    private EditTextImageButton price;

    @Override
    public View onCreateView(final LayoutInflater lI, ViewGroup p, Bundle b) {
        return lI.inflate(R.layout.fragment_ride_edit2, p, false);
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        
        date = (ButtonImageButton) v.findViewById(R.id.date);
        date.btn.setOnClickListener(pickDate);
        date.icn.setOnClickListener(pickDate);
        time = (ButtonImageButton) v.findViewById(R.id.time);
        time.btn.setOnClickListener(pickTime);
        time.icn.setOnClickListener(pickTime);
        price = (EditTextImageButton) v.findViewById(R.id.price);
        price.text.setOnFocusChangeListener(onPriceChange);
        reoccur = (ReoccuringWeekDaysView) v.findViewById(R.id.reoccur);
    }

    public void setRide(Ride ride) {
        this.ride = ride;
        System.out.println("fragment 2 set ride");
        setPrice(ride.getPrice());
        setDeparture(ride.getDep());
        reoccur.setDays(ride.getDetails());
    }

    protected void setPrice(int p) {
        if (p != 0) {
            price.text.setText((p / 100) + " €");
        } else {
            price.text.setText("");
        }
        ride.price(p);
    }

    OnFocusChangeListener onPriceChange = new OnFocusChangeListener() {
        
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                price.text.setText("");
            } else {
                String p = price.text.getText().toString();
                if (p.matches("\\d+\\.?\\d*"))
                    setPrice((int) Double.valueOf(p).doubleValue() * 100);
                else
                    setPrice(ride.getPrice());
            }
        }
    };

    private void setDeparture(long timestamp) {
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_YEAR);
        cal.setTimeInMillis(timestamp);
        if (cal.get(Calendar.DAY_OF_YEAR) == today)
            date.btn.setText(getString(R.string.today));
        else if (cal.get(Calendar.DAY_OF_YEAR) == today + 1)
            date.btn.setText(getString(R.string.tomorrow));
        else if (cal.get(Calendar.DAY_OF_YEAR) == today + 2)
            date.btn.setText(getString(R.string.after_tomorrow));
        else
            date.btn.setText(new SimpleDateFormat("dd. MMM yyyy",
                    Locale.GERMANY).format(timestamp));
        time.btn.setText(new SimpleDateFormat("HH:mm",
                Locale.GERMANY).format(timestamp));
        ride.dep(timestamp);
    }

    OnClickListener pickDate = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog d = new DatePickerDialog(getActivity(),
                    EditRideFragment2.this, year, month, day);
            d.show();
        }
    };

    OnClickListener pickTime = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            final Calendar c = Calendar.getInstance();
            int min = c.get(Calendar.MINUTE);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            TimePickerDialog t = new TimePickerDialog(getActivity(),
                    EditRideFragment2.this, hour, min, true);
            t.show();
        }
    };

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ride.getDep());
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        setDeparture(cal.getTime().getTime());
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ride.getDep());
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        setDeparture(cal.getTime().getTime());
    }
}
