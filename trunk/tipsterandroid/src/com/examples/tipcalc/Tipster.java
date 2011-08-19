package com.examples.tipcalc;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class Tipster extends Activity {
	private static final String TAG = "MyActivity";
	
	// Widgets in the application
	private EditText txtAmount;
	private EditText txtPeople;
	private EditText txtTipOther;
	private RadioGroup rdoGroupTips;
	private Button btnCalculate;
	private Button btnReset;

	private TextView txtTipAmount;
	private TextView txtTotalToPay;
	private TextView txtTipPerPerson;
	private TextView txtLatLong;
	private TextView txtAddress;
	private TextView txtAccuracy;
	private TextView txtComment;

	private Location curLocation;
	private LocationManager myLocationManager;
	private LocationListener gpsListener;

	// For the id of radio button selected
	private int radioCheckedId = -1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("on create...");
		setContentView(R.layout.main);

		// Access the various widgets by their id in R.java
		txtAmount = (EditText) findViewById(R.id.txtAmount);
		// On app load, the cursor should be in the Amount field
		txtAmount.requestFocus();

		txtPeople = (EditText) findViewById(R.id.txtPeople);
		txtTipOther = (EditText) findViewById(R.id.txtTipOther);

		rdoGroupTips = (RadioGroup) findViewById(R.id.RadioGroupTips);

		btnCalculate = (Button) findViewById(R.id.btnCalculate);
		// On app load, the Calculate button is disabled
		btnCalculate.setEnabled(false);

		btnReset = (Button) findViewById(R.id.btnReset);

		txtTipAmount = (TextView) findViewById(R.id.txtTipAmount);
		txtTotalToPay = (TextView) findViewById(R.id.txtTotalToPay);
		txtTipPerPerson = (TextView) findViewById(R.id.txtTipPerPerson);

		// On app load, disable the 'Other tip' percentage text field
		txtTipOther.setEnabled(false);

		txtLatLong = (TextView) findViewById(R.id.txtLatLong);
		txtAddress = (TextView) findViewById(R.id.txtAddress);
		txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
		txtComment = (TextView) findViewById(R.id.txtComment);

		/*
		 * Attach a OnCheckedChangeListener to the radio group to monitor radio
		 * buttons selected by user
		 */
		rdoGroupTips.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// Enable/disable Other Percentage tip field
				if (checkedId == R.id.radioFifteen
						|| checkedId == R.id.radioTwenty) {
					txtTipOther.setEnabled(false);
					/*
					 * Enable the calculate button if Total Amount and No. of
					 * People fields have valid values.
					 */
					btnCalculate.setEnabled(enableCalculateButton());
				}
				if (checkedId == R.id.radioOther) {
					// enable the Other Percentage tip field
					txtTipOther.setEnabled(true);
					// set the focus to this field
					txtTipOther.requestFocus();
					/*
					 * Enable the calculate button if Total Amount and No. of
					 * People fields have valid values. Also ensure that user
					 * has entered a Other Tip Percentage value before enabling
					 * the Calculate button.
					 */
					btnCalculate.setEnabled(enableCalculateButton());
				}
				// To determine the tip percentage choice made by user
				radioCheckedId = checkedId;
			}
		});
		
		/*
		 * Attach a KeyListener to the Tip Amount, No. of People and Other Tip
		 * Percentage text fields
		 */
/*		txtAmount.setOnKeyListener(mKeyListener);
		txtPeople.setOnKeyListener(mKeyListener);
		txtTipOther.setOnKeyListener(mKeyListener);
*/
		
		/*
		 * Instead of using KeyListener, use TextChangedListener This will work on real phones
		 */
		txtAmount.addTextChangedListener(inputTextWatcher);
		txtPeople.addTextChangedListener(inputTextWatcher);
		txtTipOther.addTextChangedListener(inputTextWatcher);

		/* Attach listener to the Calculate and Reset buttons */
		btnCalculate.setOnClickListener(mClickListener);
		btnReset.setOnClickListener(mClickListener);

		// Location stuff
		txtAddress.setText("Locating...");
		myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		gpsListener = new MyLocationListener();
		startGpsListener();

		curLocation = myLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (curLocation != null) {
			System.out.println("lat long: " + curLocation.getLatitude() + ", "
					+ curLocation.getLongitude());
			setLocationText(curLocation);
		} else {
			txtAddress.setText("Can't retrieve current location");
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		stopGpsListener();
		finish();
		System.exit(0);
	}



	private void setLocationText(Location location) {
		double lat = location.getLatitude();
		double lng = location.getLongitude();

		txtLatLong.setText(lat + ", " + lng);

		Geocoder gc = new Geocoder(this, Locale.getDefault());
		try {
			List<Address> addresses = gc.getFromLocation(lat, lng, 1);

			StringBuilder sb = new StringBuilder();
			if (addresses.size() > 0) {
				Address address = addresses.get(0);
				String suburb = "";
				for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
					String addressLine = address.getAddressLine(i);
					sb.append(addressLine).append("\n");
					if (i == 1) {
						suburb = addressLine;
					}
				}
				sb.append(address.getCountryName());
				
				if (suburb.toUpperCase().contains("BONDI")) {
					txtComment.setText("You're in Bondi, tip more to look cool!");
				}
				txtAddress.setText(sb.toString());
			}

		} catch (IOException e) {
			txtAddress.setText("error: " + e.getMessage());
			e.printStackTrace();
		}
		
		if (curLocation != null && curLocation.hasAccuracy()) {
			if (curLocation.getAccuracy() < 50) {
//				stopGpsListener();
				txtAccuracy.setText(String.valueOf(curLocation.getAccuracy()));
			}
		}
	}

	/**
	 * Enable calculate button logic
	 * 
	 * @return
	 */
	private boolean enableCalculateButton() {
		boolean enable = false;
		if (txtTipOther.isEnabled()) {
			enable = txtAmount.getText().length() > 0
					&& txtPeople.getText().length() > 0
					&& txtTipOther.getText().length() > 0;
		} else {
			enable = txtAmount.getText().length() > 0
					&& txtPeople.getText().length() > 0;
		}
		return enable;
	}

	/*
	 * KeyListener for the Total Amount, No of People and Other Tip Percentage
	 * fields. We need to apply this key listener to check for following
	 * conditions:
	 * 
	 * 1) If user selects Other tip percentage, then the other tip text field
	 * should have a valid tip percentage entered by the user. Enable the
	 * Calculate button only when user enters a valid value.
	 * 
	 * 2) If user does not enter values in the Total Amount and No of People, we
	 * cannot perform the calculations. Hence enable the Calculate button only
	 * when user enters a valid values.
	 * 
	 * This does not work on phone
	 * see: http://stackoverflow.com/questions/4282214/onkeylistener-not-working-on-virtual-keyboard
	 */
	private OnKeyListener mKeyListener = new OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			btnCalculate.setEnabled(enableCalculateButton());
			return false;
		}

	};
	
	private TextWatcher inputTextWatcher = new TextWatcher() {
	    public void afterTextChanged(Editable s) {}
	    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	    public void onTextChanged(CharSequence s, int start, int before, int count) {
			btnCalculate.setEnabled(enableCalculateButton());
			Log.d(TAG, "s = " + s);          
	    }
	};

	/**
	 * ClickListener for the Calculate and Reset buttons. Depending on the
	 * button clicked, the corresponding method is called.
	 */
	private OnClickListener mClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.btnCalculate) {
				calculate();
			} else {
				reset();
			}
		}
	};

	/**
	 * Resets the results text views at the bottom of the screen as well as
	 * resets the text fields and radio buttons.
	 */
	private void reset() {
		txtTipAmount.setText("");
		txtTotalToPay.setText("");
		txtTipPerPerson.setText("");
		txtAmount.setText("");
		txtPeople.setText("");
		txtTipOther.setText("");
		rdoGroupTips.clearCheck();
		rdoGroupTips.check(R.id.radioFifteen);
		// set focus on the first field
		txtAmount.requestFocus();
	}

	/**
	 * Calculate the tip as per data entered by the user.
	 */
	private void calculate() {
		Double billAmount = Double.parseDouble(txtAmount.getText().toString());
		Double totalPeople = Double.parseDouble(txtPeople.getText().toString());
		Double percentage = null;
		boolean isError = false;
		if (billAmount < 1.0) {
			showErrorAlert("Enter a valid Total Amount.", txtAmount.getId());
			isError = true;
		}

		if (totalPeople < 1.0) {
			showErrorAlert("Enter a valid value for No. of people.", txtPeople
					.getId());
			isError = true;
		}

		/*
		 * If user never changes radio selection, then it means the default
		 * selection of 15% is in effect. But its safer to verify
		 */
		if (radioCheckedId == -1) {
			radioCheckedId = rdoGroupTips.getCheckedRadioButtonId();
		}
		if (radioCheckedId == R.id.radioFifteen) {
			percentage = 15.00;
		} else if (radioCheckedId == R.id.radioTwenty) {
			percentage = 20.00;
		} else if (radioCheckedId == R.id.radioOther) {
			percentage = Double.parseDouble(txtTipOther.getText().toString());
			if (percentage < 1.0) {
				showErrorAlert("Enter a valid Tip percentage", txtTipOther
						.getId());
				isError = true;
			}
		}
		/*
		 * If all fields are populated with valid values, then proceed to
		 * calculate the tips
		 */
		if (!isError) {
			Double tipAmount = ((billAmount * percentage) / 100);
			Double totalToPay = billAmount + tipAmount;
			Double perPersonPays = totalToPay / totalPeople;

			txtTipAmount.setText(tipAmount.toString());
			txtTotalToPay.setText(totalToPay.toString());
			txtTipPerPerson.setText(perPersonPays.toString());
		}
	}

	/**
	 * Shows the error message in an alert dialog
	 * 
	 * @param errorMessage
	 *            String the error message to show
	 * @param fieldId
	 *            the Id of the field which caused the error. This is required
	 *            so that the focus can be set on that field once the dialog is
	 *            dismissed.
	 */
	private void showErrorAlert(String errorMessage, final int fieldId) {
		new AlertDialog.Builder(this).setTitle("Error")
				.setMessage(errorMessage).setNeutralButton("Close",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								findViewById(fieldId).requestFocus();
							}
						}).show();
	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location argLocation) {
			System.out.println("on location changed");
			curLocation = argLocation;

			// check if locations has accuracy data
			if (curLocation != null && curLocation.hasAccuracy()) {
				// Accuracy is in rage of 50 meters, stop listening we have a fix
				if (curLocation.getAccuracy() < 50) {
					stopGpsListener();
					txtAccuracy.setText(String.valueOf(curLocation.getAccuracy()));
				}
			}

			setLocationText(argLocation);
		}

		public void onProviderDisabled(String provider) {
			System.out.println("on provider disabled");
			txtAddress.setText("GPS disabled");
		}

		public void onProviderEnabled(String provider) {
			System.out.println("on provider enabled");
			txtAddress.setText("GPS enabled");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				stopGpsListener();
				//txtAddress.setText("GPS Temporarily Unavailable");
			}
			else if (status == LocationProvider.OUT_OF_SERVICE) {
				stopGpsListener();
				txtAddress.setText("GPS Out of Service");
			}
			else if (status == LocationProvider.AVAILABLE) {
				startGpsListener();
				txtAddress.setText("Locating...");
			}
		}
	};

	private void startGpsListener() {
		if (myLocationManager != null) {
			myLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
		}
	}

	private void stopGpsListener() {
		if (myLocationManager != null) {
			myLocationManager.removeUpdates(gpsListener);
		}
	}
}
