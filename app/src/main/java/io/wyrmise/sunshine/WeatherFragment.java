package io.wyrmise.sunshine;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

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

/**
 * A placeholder fragment containing a simple view.
 */
public class WeatherFragment extends Fragment implements AdapterView.OnItemClickListener {

    ListView listView;

    public static ArrayList<Weather> weatherList;

    public WeatherFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) rootView.findViewById(R.id.forecast_listview);

        listView.setOnItemClickListener(this);

        GetWeatherData weatherTask = new GetWeatherData();

        weatherTask.execute("20", "105");

        return rootView;
    }


    private class GetWeatherData extends AsyncTask<String, Void, Weather[]> {

        private final String LOG_TAG = GetWeatherData.class.getSimpleName();

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

            ArrayList<Weather> array = new ArrayList<Weather>(Arrays.asList(result));

            WeatherAdapter adapter = new WeatherAdapter(getActivity().getApplicationContext(), R.layout.list_item, array);

            listView.setAdapter(adapter);

        }


        private String getReadableDate(long time) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd");
            return dateFormat.format(time);
        }

        private Weather[] WeatherParser(String JsonString, int numOfDays) throws JSONException {
            final String LIST = "list";
            final String WEATHER = "weather";
            final String TEMP = "temp";
            final String MIN = "min";
            final String MAX = "max";
            final String DESC = "description";
            final String ICON = "icon";
            final String HUMIDITY = "humidity";
            final String PRESSURE = "pressure";
            final String WIND = "speed";

            JSONObject weatherForecast = new JSONObject(JsonString);
            JSONArray weatherArray = weatherForecast.getJSONArray(LIST);

            Time dayTime = new Time();
            dayTime.setToNow();

            int startDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff) + 1;

            dayTime = new Time();

            Weather[] result = new Weather[numOfDays];

            for (int i = 0; i < weatherArray.length(); i++) {

                Weather weatherObject = new Weather();

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
    }



}