package com.tene.products.uasr.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tene.platform.audio.AppAudioPlayer;
import com.tene.platform.client.services.interfaces.TeneHttpServiceException;
import com.tene.platform.common.compression.CompressThread;
import com.tene.platform.language.AppProperties;
import com.tene.platform.location.LocationProvider;
import com.tene.platform.location.LocationUtils;
import com.tene.platform.utils.AppConstants;
import com.tene.platform.utils.CommonUtils;
import com.tene.platform.utils.DateTimeUtil;
import com.tene.platform.utils.ImageUtilities;
import com.tene.platform.utils.eSAPCompressionResponseHandler;
import com.tene.products.cms.pojos.DeviceConfig;
import com.tene.products.database.DatabaseAccess;
import com.tene.products.esap.service.params.DistrictMaster;
import com.tene.products.esap.service.params.FarmerRegisterBean;
import com.tene.products.esap.service.params.ScreenMaster;
import com.tene.products.esap.service.params.TalukMaster;
import com.tene.products.esap.service.params.VillageMaster;
import com.tene.products.uasr.model.DistrictMasterManager;
import com.tene.products.uasr.model.ScreenMasterManager;
import com.tene.products.uasr.model.TalukMasterManager;
import com.tene.products.uasr.model.VillageMasterManager;
import com.tene.products.uasr.serviceaccess.UasrHttpServiceInvoke;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Addfarmer extends Activity implements TextWatcher {

	private static final String TAG = Addfarmer.class.getName();
	private static final String _SCREEN_ID = "ADD_FARMER";
	private File myFile = null;
	private ScreenMaster scrnMaster = null;
	private static boolean cameraInteraction = false;
	private UasrHttpServiceInvoke conn;
	private Spinner district, taluk, village;
	private EditText farmerName, farmerPhno;
	private FarmerRegisterBean.Request farmerRegisterBeanRequest = null;
	private int villId = 0;
	private ProgressDialog progressDialog;

	@Override
	public final void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.addfarmer);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		try {
			if(DeviceConfig.getInstance(getApplicationContext()) != null){
				if(DeviceConfig.getInstance(getApplicationContext()).getClientName() != null){
					((TextView)findViewById(R.id.FooterText)).setText(DeviceConfig.getInstance(getApplicationContext()).getClientName());
				}else{
					((TextView)findViewById(R.id.FooterText)).setText(AppProperties._footertext);
				}
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
			((TextView)findViewById(R.id.FooterText)).setText(AppProperties._footertext);
		}

		((TextView) findViewById(R.id.farmerphno)).setText(AppProperties._farmerPhone);
		((TextView) findViewById(R.id.farmername)).setText(AppProperties._farmername);
		((TextView) findViewById(R.id.district)).setText(AppProperties._district);
		((TextView) findViewById(R.id.taluk)).setText(AppProperties._subdistrict);
		//((TextView) findViewById(R.id.panchayat)).setText(AppProperties._panchayat);
		((TextView) findViewById(R.id.village)).setText(AppProperties._village);
		((TextView) findViewById(R.id.supportland)).setText(AppProperties._landscape);
		((TextView) findViewById(R.id.farmerphoto)).setText(AppProperties._farmerphoto);
		((ImageView) findViewById(R.id.footer_image)).setImageBitmap(BitmapFactory.decodeFile(CommonUtils.getEnv("/eSAP/logo.jpg")));

		findViewById(R.id.playbtn).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String audioFilePath = CommonUtils.getAudioFilePath("AddFarmer.wav");
				if (scrnMaster.getScreenAudioExplainFile() != null) {
					audioFilePath = CommonUtils.getAudioFilePath(scrnMaster.getScreenAudioExplainFile());

				}
				AppAudioPlayer.getInstance(getApplicationContext()).startPlaying(audioFilePath);
				if (AppAudioPlayer.getInstance(getApplicationContext()).getAudioStatus()) {
					findViewById(R.id.playbtn).setVisibility(android.view.View.VISIBLE);
				} else {
					findViewById(R.id.playbtn).setVisibility(android.view.View.INVISIBLE);
				}

			}
		});;

		findViewById(R.id.photo).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				takePhoto();
			}
		});

		findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				if (validateFormData(i)) {
					new Registering(Addfarmer.this).execute();


				}

			}
		});

		findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});


		findViewById(R.id.homebutton).setVisibility(android.view.View.GONE);
		scrnMaster = (new ScreenMasterManager(this)).getScreenMasterScreenId(_SCREEN_ID);
		((TextView) findViewById(R.id.textView1)).setText(AppProperties._addfarmer);
		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isValidPhoto()) {
					CommonUtils.deleteFile(myFile.getAbsolutePath());
				}
				finish();
			}
		});

		farmerName = (EditText) findViewById(R.id.farmernameedit);
		farmerPhno = (EditText) findViewById(R.id.farmerphnoedit);
		farmerPhno.setRawInputType(Configuration.KEYBOARD_12KEY);
		farmerPhno.addTextChangedListener(this);
		//	panchayat = (Spinner) findViewById(R.id.panchayatlist);
		taluk = (Spinner) findViewById(R.id.taluklist);
		village = (Spinner) findViewById(R.id.villagelist);

		final ArrayList<DistrictMaster> distlist = (new DistrictMasterManager(
				this)).getAllDistrictMaster();
		int len = distlist.size();

		String array_spinner[] = new String[len + 1];
		array_spinner[0] = "Select";
		for (int i = 1; i <= len; i++) {
			DistrictMaster x = distlist.get(i - 1);
			array_spinner[i] = x.getDistrictName();
		}

		district = (Spinner) findViewById(R.id.districtlist);
		ArrayAdapter<String> districtlist_array1 = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, array_spinner);
		districtlist_array1
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		district.setAdapter(districtlist_array1);
		district.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
									   View selectedItemView, int position, long id) {
				if (position == 0) {
					return; // he has selected "Select" Option
				}
				position--;
				DistrictMaster districtMaster = distlist.get(position);
				Log.i(TAG, "Selected District Name ==="
						+ distlist.get(position).getDistrictName());
				int districtId = districtMaster.getDistrictid();
				if (districtId > 0) {
					OnDistrictChangeById(districtId);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// Display Toast like select something
			}
		});
	}

	@Override
	protected void onResume() {
		getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,
				LayoutParams.FLAG_FULLSCREEN);
		// getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		int maxLength = 10;
		InputFilter[] FilterArray = new InputFilter[1];
		FilterArray[0] = new InputFilter.LengthFilter(maxLength);
		farmerPhno.setFilters(FilterArray);
		initService();
		String audioFilePath = CommonUtils.getAudioFilePath(scrnMaster
				.getScreenAudioExplainFile());
		if (scrnMaster.getScreenAudioNameFile() != null) {
			audioFilePath = CommonUtils.getAudioFilePath(scrnMaster
					.getScreenAudioNameFile());
		}
		AppAudioPlayer.getInstance(this).startPlaying(audioFilePath);
		if (AppAudioPlayer.getInstance(this).getAudioStatus()) {
			findViewById(R.id.playbtn).setVisibility(android.view.View.VISIBLE);
		} else {
			findViewById(R.id.playbtn).setVisibility(android.view.View.INVISIBLE);
		}
		if (cameraInteraction == true) {
			Log.i(TAG, "Coming from the Camera");
		} else {
			Log.i(TAG, "Start getting Farmer Details");
			// currentFarmer = null;
			village.setClickable(false);
			taluk.setClickable(false);
			//	panchayat.setClickable(false);
			farmerRegisterBeanRequest = new FarmerRegisterBean.Request();
			// currentFarmer = new FarmerMaster();
			farmerRegisterBeanRequest
					.setIncidentTime(DateTimeUtil._standardDateFormat
							.format(new Date()));
			// currentFarmer.setCreateTimeStamp(DateTimeUtil.getUasrTimestamp());
		}
		cameraInteraction = false;
		LocationUtils.forceGPSEnble(this);
		super.onResume();
	}

	private void registerFarmer() {
		try {
			String mystring = farmerPhno.getText().toString();
			long lav = Long.parseLong(mystring.trim());
			// currentFarmer.setFarmerId(lav);
			farmerRegisterBeanRequest.setFarmerIssuedId(lav);
		} catch (NumberFormatException nfe) {
			Log.e(TAG, "NumberFormatException ===" + nfe.getMessage());
		}

		// currentFarmer.setFarmerName(farmerName.getText().toString());
		// currentFarmer.setPhoneNumber(farmerPhno.getText().toString());
		// // currentFarmer.setFarmerAddress("");
		// currentFarmer.setLat(LocationProvider.getCurrentLatitude().toString());
		// currentFarmer.setLongitude(LocationProvider.getCurrentLongitude().toString());
		// currentFarmer.setFarmerPhoto(myFile.getName());

		farmerRegisterBeanRequest.setFarmerName(farmerName.getText().toString()
				.trim());
		farmerRegisterBeanRequest.setPhoneNumber(Long.valueOf(farmerPhno
				.getText().toString()));
		// farmerRegisterBeanRequest.setAddress(makeAddress(farmerRegisterBeanRequest.getVillageId()));
		farmerRegisterBeanRequest.setLatitude(LocationProvider
				.getCurrentLatitude().toString());
		farmerRegisterBeanRequest.setLongitude(LocationProvider
				.getCurrentLongitude().toString());
		farmerRegisterBeanRequest.setFarmerPhoto(myFile.getName());

		// Log.i(TAG, "currentFarmer = "+currentFarmer);
		Log.i(TAG, "farmerRegisterBeanRequest =" + farmerRegisterBeanRequest);
		// FarmerMasterManager fm = new FarmerMasterManager(this);
		// fm.getFarmer(String.valueOf(currentFarmer.getFarmerId()));
		// fm.getFarmer(String.valueOf(farmerRegisterBeanRequest.getFarmerIssuedId()));
		// currentFarmer.setUpload("n");
		File compressedFile = new File(myFile.getAbsoluteFile().toString()
				.substring(0, myFile.getAbsoluteFile().toString().indexOf("."))
				+ "-c.webp");

		farmerRegisterBeanRequest.setFarmerPhoto(compressedFile.getName());

		final String serviceId = "fr@"
				+ DateTimeUtil.serviceIdTime.format(new Date());
		//currentFarmer.setServiceId(serviceId);
		Date date = null;
		try {
			date = DateTimeUtil._standardDateFormat
					.parse(farmerRegisterBeanRequest.getIncidentTime());
		} catch (Exception e) {
			Log.d(TAG, "unparseable", e);
		}
		if (farmerRegisterBeanRequest.getVillageId() == 0) {
			farmerRegisterBeanRequest.setVillageId(villId);
		} else {
			Log.i(TAG, "farmerRegisterBeanRequest.getVillageId()="
					+ farmerRegisterBeanRequest.getVillageId());
		}

		DatabaseAccess.addFarmerMaster(farmerRegisterBeanRequest.getFarmerIssuedId(),
				farmerRegisterBeanRequest.getVillageId(),
				String.valueOf(farmerRegisterBeanRequest.getPhoneNumber()),
				farmerRegisterBeanRequest.getFarmerName(), "",
				farmerRegisterBeanRequest.getLatitude(),
				farmerRegisterBeanRequest.getLongitude(),
				farmerRegisterBeanRequest.getFarmerPhoto(),
				DateTimeUtil._sqliteDateTimeFormat.format(date), "n", serviceId);

		Map<String, String> kvs = new HashMap<String, String>();
		Gson gson = new Gson();
		kvs.put("deviceId", CommonUtils.fetchImei(getApplicationContext()));
		kvs.put(FarmerRegisterBean.Request.key, gson.toJson(
				farmerRegisterBeanRequest, FarmerRegisterBean.Request.class));

		// kvs.put("farmerIssuedID",
		// String.valueOf(currentFarmer.getFarmerId()));
		// kvs.put("villageID", String.valueOf( currentFarmer.getVillageId()));
		// kvs.put("farmerName", currentFarmer.getFarmerName());
		// kvs.put("farmerAddress", currentFarmer.getFarmerAddress());
		// kvs.put("latitude", currentFarmer.getLat());
		// kvs.put("longitude", currentFarmer.getLongitude());
		// kvs.put("phoneNumber", currentFarmer.getPhoneNumber());
		// kvs.put("eventTime", currentFarmer.getCreateTimeStamp());
		// kvs.put("deviceID", CommonUtils.fetchImei(getApplicationContext()));

		final String uploadServiceId = "fp@" + compressedFile.getAbsolutePath();
		// kvs.put("farmerPhotoName",compressedFile.getName());
		final Map<String, String> kvsupload = new HashMap<String, String>();
		kvsupload.put("deviceId",
				CommonUtils.fetchImei(getApplicationContext()));
		kvsupload.put("eventTime",
				DateTimeUtil._standardDateFormat.format(new Date()));
		kvsupload.put("photoType", "f");
		kvsupload.put("filePath", compressedFile.toString());
		kvsupload.put("fileName", compressedFile.getName());
		boolean result = false;
		Log.i(TAG, "Sending the Farmer Details to the service ===" + kvs);
		try {
			result = postData(AppConstants.HTTP_SERVICE_LISTENER_STRING,
					new URL(AppConstants.IP+ AppConstants.CLIENT_URL+"/tree/"
							+ AppConstants.FARMER_REGISTER), serviceId, null,
					kvs);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			Handler handler=new Handler(getApplicationContext().getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getBaseContext(), "Please install the TeneService", Toast.LENGTH_SHORT).show();
				}
			});

			return;
		}
		float compressionPercentage  = AppConstants.farmerPhotoCompression;
		(new CompressThread(myFile, compressedFile,
				compressionPercentage,
				new eSAPCompressionResponseHandler(compressedFile, true,
						uploadServiceId, serviceId, kvsupload))).start();
		Log.i(TAG, "Submitting Farmer Details to Service result ===" + result);
	}

	private void takePhoto() {

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// String imgName = currentFarmer.getCreateTimeStamp() + ".jpg";
		// currentFarmer.setFarmerPhoto(imgName);
		Date date = null;
		try {
			date = DateTimeUtil._standardDateFormat
					.parse(farmerRegisterBeanRequest.getIncidentTime());
		} catch (Exception e) {
			Log.d(TAG, "unparseable", e);
		}
		String imgName = DateTimeUtil.serviceIdTime.format(date) + ".webp";
		farmerRegisterBeanRequest.setFarmerPhoto(imgName);
		myFile = new File(CommonUtils.getFarmerPhotoFilePath(imgName));
		cameraInteraction = true;
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(myFile));
		startActivityForResult(intent, 0);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.i(TAG, "onActivityResult() : requestCode ===" + requestCode
				+ " & resultCode ===" + resultCode + " & RESULT_OK ==="
				+ RESULT_OK);
		if (resultCode != RESULT_OK && requestCode == 0) {
			if (isValidPhoto()) {
				CommonUtils.deleteFile(myFile.getAbsolutePath());
			}
		} else if (resultCode == RESULT_OK && requestCode == 0) {
			if (ImageUtilities.setImage(myFile.getAbsolutePath(), 180, 180,(ImageButton)findViewById(R.id.photo)) == false) {
				Toast.makeText(this,
						"Please capture the farmer photo in landscape mode",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	public void OnDistrictChangeById(int districtId) {
		taluk.setClickable(true);
		TalukMasterManager tm = new TalukMasterManager(this);
		final ArrayList<TalukMaster> taluklist = tm
				.getTalukBydistrictid(districtId);
		List<String> list = new ArrayList<String>();
		list.add("Select");// taluklist.add(0, "Select");
		for (int i = 0; i < taluklist.size(); i++) {
			list.add(taluklist.get(i).getTalukName());
		}
		ArrayAdapter<String> talukList_array2 = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				list.toArray(new String[0]));
		talukList_array2
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		taluk.setAdapter(talukList_array2);
		taluk.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
									   View selectedItemView, int position, long id) {
				if (position == 0) {
					return;
				}
				TalukMaster talukMaster = taluklist.get(position - 1);
				Log.i(TAG, "Selected Taluk Id ===" + talukMaster.getTalukid()
						+ " and Taluk Name ===" + talukMaster.getTalukName());
				if (talukMaster.getTalukid() > 0) {
					OnPanchayatChangeByID(talukMaster.getTalukid());
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// Display Toast like select something
			}
		});
	}

//	public void OnTalukChangeById(int talukid) {
//		panchayat.setClickable(true);
//		PanchayatMasterManager pm = new PanchayatMasterManager(this);
//		final ArrayList<PanchayatMaster> panchayatList = pm
//				.getPanchayatBytalukid(talukid);
//		List<String> list = new ArrayList<String>();
//		list.add("Select");
//		for (int i = 0; i < panchayatList.size(); i++) {
//			list.add(panchayatList.get(i).getPanchayatName());
//		}
//		ArrayAdapter<String> panchayatList_array2 = new ArrayAdapter<String>(
//				this, android.R.layout.simple_spinner_item,
//				list.toArray(new String[0]));
//		panchayatList_array2
//				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//		panchayat.setAdapter(panchayatList_array2);
//		panchayat.setOnItemSelectedListener(new OnItemSelectedListener() {
//			@Override
//			public void onItemSelected(AdapterView<?> parentView,
//					View selectedItemView, int position, long id) {
//				if (position == 0) {
//					return;
//				}
//				PanchayatMaster panchayatMaster = panchayatList
//						.get(position - 1);
//				int panchayatID = panchayatMaster.getPanchayatid();
//				if (panchayatID > 0) {
//					OnPanchayatChangeByID(panchayatID);
//				}
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> parentView) {
//				// Display Toast like select something
//			}
//		});
//	}

	public void OnPanchayatChangeByID(int talukid) {
		village.setClickable(true);
		VillageMasterManager vm = new VillageMasterManager(this);
		final ArrayList<VillageMaster> villageList = vm
				.getVillageBypanchayatID(talukid);
		List<String> list = new ArrayList<String>();
		list.add("Select");
		for (int i = 0; i < villageList.size(); i++) {
			list.add(villageList.get(i).getVillagename());
		}
		ArrayAdapter<String> panchayatList_array2 = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item,
				list.toArray(new String[0]));
		panchayatList_array2
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		village.setAdapter(panchayatList_array2);
		village.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
									   View selectedItemView, int position, long id) {
				if (position == 0) {
					return;
				}
				VillageMaster villageMaster = villageList.get(position - 1);
				Log.i(TAG,
						"Selected Village Id ==="
								+ villageMaster.getVillageid()
								+ " and Village Name ==="
								+ villageMaster.getVillagename());
				farmerRegisterBeanRequest.setVillageId(villageMaster
						.getVillageid());
				// currentFarmer.setVillageId(villageMaster.getVillageid());
				villId = farmerRegisterBeanRequest.getVillageId();

			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
				// Display Toast like select something
			}
		});
	}

	private boolean validateFormData(Intent intent) {
		String strfarmerPhoneNumber = farmerPhno.getText().toString().trim();
		if ((strfarmerPhoneNumber.length() == 0)) {
			Toast.makeText(this, "Enter the farmer Phone number ",
					Toast.LENGTH_SHORT).show();
			return false;
		} else {
			String regex = "^[0-9]{10,12}$";
			String farmerph = farmerPhno.getText().toString();
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(farmerph);
			if (!matcher.matches()) {
				Toast.makeText(this, "Enter a valid phone no ",
						Toast.LENGTH_SHORT).show();
				return false;
			}
		}
		String strfarmerName = farmerName.getText().toString().trim();

		if (strfarmerName.length() <= 2) {
			Toast.makeText(this, "Enter the Farmer Name with minimum 3 chars",
					Toast.LENGTH_SHORT).show();
			return false;
		} else {
			String rege = "^[a-zA-Z \t]{3,64}$";
			String farmerna = farmerName.getText().toString().trim();
			Pattern pattern = Pattern.compile(rege);
			Matcher matcher = pattern.matcher(farmerna);
			if (!matcher.matches()) {
				Toast.makeText(this, "Enter a valid farmername  ",
						Toast.LENGTH_SHORT).show();
				return false;
			}
		}

		if (district.getSelectedItem() == null
				|| district.getSelectedItem().toString() == null) {

			Toast.makeText(this, "select  the Farmer District ",
					Toast.LENGTH_SHORT).show();
			return false;
		}

		if ((district.getSelectedItem().toString().equalsIgnoreCase("Select"))) {
			Toast.makeText(this, "Select  the Farmer District ",
					Toast.LENGTH_SHORT).show();
			return false;
		}

		if ((taluk.getSelectedItem().toString().equalsIgnoreCase("Select"))) {
			Toast.makeText(this, "Select  the Farmer Sub-district ",
					Toast.LENGTH_SHORT).show();
			return false;
		}

//		if ((panchayat.getSelectedItem().toString().equalsIgnoreCase("Select"))) {
//			Toast.makeText(this, "Select  the Farmer panchayat ",
//					Toast.LENGTH_SHORT).show();
//			return false;
//		}

		if ((village.getSelectedItem().toString().equalsIgnoreCase("Select"))) {
			Toast.makeText(this, "Select  the Farmer village ",
					Toast.LENGTH_SHORT).show();
			return false;
		}

//		if(DeviceConfig.getInstance(getApplicationContext()).getExecutionModel().getFarmer().isFarmerPhotoRequired()){
		if (!isValidPhoto()) {
			Toast.makeText(this, "Please capture the photo", Toast.LENGTH_SHORT)
					.show();
			return false;
		}
//		}

		return true;
	}

	private boolean isValidPhoto() {
		if (myFile != null && myFile.isFile()) {
			return true;
		}
		return false;
	}

	/**
	 * To initialize the service
	 */
	public void initService() {
		conn = new UasrHttpServiceInvoke();
		Log.i(TAG, "----initService()----");
		Intent i = new Intent();
		i.setClassName("com.tene.platform.client",
				"com.tene.platform.client.services.TeneHttpServices");
		i.putExtra("bcs", AppConstants.HTTP_SERVICE_LISTENER_STRING);
		Log.i(TAG, "binding the serive with the values bcs ="
				+ AppConstants.HTTP_SERVICE_LISTENER_STRING);
		bindService(i, conn, Context.BIND_AUTO_CREATE);
	}

	/**
	 *
	 * @param broadcastListenerString
	 * @param url
	 *            IP Address of the server
	 * @param serviceId
	 *            serviceId for the request
	 * @param dependencyServiceId
	 * @param kvs
	 *            DataPoint to upload
	 * @return
	 */
	public boolean postData(String broadcastListenerString, URL url,
							String serviceId, String dependencyServiceId,
							Map<String, String> kvs) {
		UasrHttpServiceInvoke psi = new UasrHttpServiceInvoke();
		try {
			return psi.postTeneHttpService(broadcastListenerString, url,
					serviceId, dependencyServiceId, kvs);
		} catch (TeneHttpServiceException e) {
			Log.e(TAG,
					"Exception while submitting the Farmer Details to the Service are ==="
							+ e, e);
			return false;
		}
	}

	/**
	 * To release Service
	 */
	public void releaseService() {
		Log.i(TAG, "----releaseService()----");
		unbindService(conn);
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseService();
	}

	@Override
	public void afterTextChanged(Editable s) {

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
								  int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		((Button) findViewById(R.id.charcount)).setText(String.valueOf(farmerPhno.getText().toString()
				.length()));
	}
	class Registering extends AsyncTask<Void,Void,Void> {

		Context context;

		public Registering(Context context) {
			this.context = context;
		}


		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(Addfarmer.this);
			progressDialog.setTitle("Submitting ....");
			progressDialog.setMessage("Data submitting to server please wait .....");
			progressDialog.show();
			if (BuildConfig.DEBUG){
				Log.i(TAG,"created progressDialog and showing here");
			}
		}

		@Override
		protected void onPostExecute(Void aBoolean) {
			super.onPostExecute(aBoolean);
			if (BuildConfig.DEBUG){
				Log.i(TAG,"post execute with the result value="+aBoolean);
			}
			if (progressDialog != null && progressDialog.isShowing()){

				if (BuildConfig.DEBUG){
					Log.i(TAG,"dismissing progressDialog");
				}
				progressDialog.dismiss();
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}


		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected Void doInBackground(Void... params) {
			//boolean result = true;
			try{
				//	Thread.sleep(4000);if (BuildConfig.DEBUG){
				Log.i(TAG,"doing in background started");
				registerFarmer();
				Handler handler =  new Handler(context.getMainLooper());
				handler.post( new Runnable(){
					public void run(){
						Toast.makeText(getApplicationContext(), "Farmer registered successfully.",
								Toast.LENGTH_LONG).show();
						Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}
				});
//					farmerRegisterBeanRequest = new FarmerRegisterBean.Request();
			}catch (Exception e){
				Log.e(TAG, "doInBackground: exeception is ="+
						e.getMessage() );
//				result = false;
			}
			return null;
		}
	}
}
