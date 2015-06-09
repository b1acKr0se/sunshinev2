package io.wyrmise.sunshine;


import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class HeaderFragment extends Fragment {

    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static HeaderFragment create(int pageNumber) {
        HeaderFragment fragment = new HeaderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        fragment.setArguments(args);
        return fragment;
    }


    public HeaderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_header, container, false);

        ImageView imageView = (ImageView) rootView.findViewById(R.id.header_pic);

        TextView tempTxtView = (TextView) rootView.findViewById(R.id.temp);

        TextView cityTxtView = (TextView) rootView.findViewById(R.id.city);

        TextView descTxtView = (TextView) rootView.findViewById(R.id.desc);

        TextView humidityTxtView = (TextView) rootView.findViewById(R.id.humidity);

        TextView pressureTxtView = (TextView) rootView.findViewById(R.id.pressure);

        TextView windTxtView = (TextView) rootView.findViewById(R.id.wind);

        ArrayList<Weather> weathers = MainActivity.weatherList;

        Weather weather = weathers.get(mPageNumber);

        Resources res = getActivity().getResources();

        int resourceId = res.getIdentifier(getImageName(weather.getIcon()), "drawable", getActivity().getPackageName() );

        if(weather!=null) {
            tempTxtView.setText(weather.getTemp() + (char) 0x00B0);
            cityTxtView.setText(weather.getLocation());
            descTxtView.setText(weather.getDescription());
            humidityTxtView.setText("Humidity: " + weather.getHumidity() + "%");
            pressureTxtView.setText("Pressure: " + weather.getPressure()  + " HPA");
            windTxtView.setText("Wind Speed: " + weather.getWind() + " KMH");
            imageView.setImageResource(resourceId);
        }
        return rootView;
    }

    private String getImageName(String name) {
        String image = null;

        switch (name){
            case "01d":
                image = "sunny_header";
                break;
            case "01n":
                image = "sunny_header";
                break;
            case "02d":
                image = "sunny_header";
                break;
            case "02n":
                image = "sunny_header";
                break;
            case "03d":
                image = "scattered_cloudy_header";
                break;
            case "03n":
                image = "scattered_cloudy_header";
                break;
            case "04d":
                image = "scattered_cloudy_header";
                break;
            case "04n":
                image = "scattered_cloudy_header";
                break;
            case "09d":
                image = "shower_header";
                break;
            case "09n":
                image = "shower_header";
                break;
            case "10d":
                image = "rain_header";
                break;
            case "10n":
                image = "rain_header";
                break;
            case "11d":
                image = "thunderstorm_header";
                break;
            case "11n":
                image = "thunderstorm_header";
                break;
            case "13d":
                image = "snow_header";
                break;
            case "13n":
                image = "snow_header";
                break;
            case "50d":
                image = "p2";
                break;
            case "50n":
                image = "p3";
                break;
        }
        return image;
    }

}
