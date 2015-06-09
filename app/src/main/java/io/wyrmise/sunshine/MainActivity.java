package io.wyrmise.sunshine;

import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.viewpagerindicator.CirclePageIndicator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends FragmentActivity implements AdapterView.OnItemClickListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int NUM_PAGES = 7;

    private ViewPager mPager;

    private PagerAdapter mPagerAdapter;

    private ListView listView;

    public static ArrayList<Weather> weatherList;

    private RelativeLayout container;

    private ProgressBar progressBar;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private GoogleApiClient mGoogleApiClient;

    private String lat, lon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkPlayService()) {
            buildGoogleApiClient();
        }

        listView = (ListView) findViewById(R.id.forecast_listview);

        listView.setOnItemClickListener(this);

        container = (RelativeLayout) findViewById(R.id.container);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        mPager = (ViewPager) findViewById(R.id.viewpager);

        GetWeatherData weatherTask = new GetWeatherData();

        weatherTask.execute("21", "105.8");

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Google api callback
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        System.out.println("Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    private boolean checkPlayService() {
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(result)) {
                GooglePlayServicesUtil.getErrorDialog(result, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported by Google Play Service.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private void displayLocation() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            lat = String.valueOf(latitude);
            lon = String.valueOf(longitude);

            String[] geo = {lat, lon};


        } else {
            Toast.makeText(this, "Cannot retrieve your location!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return HeaderFragment.create(position);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }


    private class GetWeatherData extends AsyncTask<String, Void, Weather[]> {

        private final String LOG_TAG = GetWeatherData.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            listView.setVisibility(ListView.GONE);
            container.setVisibility(RelativeLayout.GONE);
        }

        @Override
        protected Weather[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String LAT_PARAM = "lat";
                final String LON_PARAM = "lon";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(LAT_PARAM, params[0])
                        .appendQueryParameter(LON_PARAM, params[1])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return WeatherParser(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        public void onPostExecute(Weather[] result) {

            progressBar.setVisibility(ProgressBar.GONE);
            listView.setVisibility(ListView.VISIBLE);
            container.setVisibility(RelativeLayout.VISIBLE);

            if (result != null) {

                weatherList = new ArrayList<Weather>(Arrays.asList(result));

                WeatherAdapter adapter = new WeatherAdapter(getApplicationContext(), R.layout.list_item, weatherList);

                listView.setAdapter(adapter);

                mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());

                mPager.setAdapter(mPagerAdapter);

                CirclePageIndicator mIndicator = (CirclePageIndicator) findViewById(R.id.indicator);

                mIndicator.setViewPager(mPager);
            } else
                Toast.makeText(getApplicationContext(), "Network error!", Toast.LENGTH_SHORT).show();

        }


        private String getReadableDate(long time) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd");
            return dateFormat.format(time);
        }

        private Weather[] WeatherParser(String JsonString, int numOfDays) throws JSONException {
            final String LIST = "list";
            final String WEATHER = "weather";
            final String CITY = "city";
            final String LOCATION = "name";
            final String TEMP = "temp";
            final String MIN = "min";
            final String MAX = "max";
            final String DESC = "description";
            final String ICON = "icon";
            final String HUMIDITY = "humidity";
            final String PRESSURE = "pressure";
            final String WIND = "speed";

            JSONObject weatherForecast = new JSONObject(JsonString);

            JSONObject cityObj = weatherForecast.getJSONObject(CITY);

            String cityName = cityObj.getString(LOCATION);

            JSONArray weatherArray = weatherForecast.getJSONArray(LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int startDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            dayTime = new Time();

            Weather[] result = new Weather[numOfDays];

            for (int i = 0; i < weatherArray.length(); i++) {

                Weather weatherObject = new Weather();

                weatherObject.setLocation(cityName);

                JSONObject dayForecast = weatherArray.getJSONObject(i);

                long dateTime;

                dateTime = dayTime.setJulianDay(startDay + i);

                weatherObject.setDate(getReadableDate(dateTime));

                JSONObject weather = dayForecast.getJSONArray(WEATHER).getJSONObject(0);

                weatherObject.setDescription(weather.getString(DESC));

                weatherObject.setIcon(weather.getString(ICON));

                JSONObject temperature = dayForecast.getJSONObject(TEMP);
                double min = temperature.getDouble(MIN);
                double max = temperature.getDouble(MAX);

                weatherObject.setTemp(String.valueOf(Math.round((min+max)/2)));

                weatherObject.setHumidity(String.valueOf(dayForecast.getInt(HUMIDITY)));

                weatherObject.setPressure(String.valueOf(dayForecast.getDouble(PRESSURE)));

                weatherObject.setWind(String.valueOf(dayForecast.getDouble(WIND)));

                weatherObject.setMinTemp(String.valueOf(Math.round(min)));
                weatherObject.setMaxTemp(String.valueOf(Math.round(max)));

                result[i] = weatherObject;
            }
            return result;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mPager.setCurrentItem(position, true);
    }

}
