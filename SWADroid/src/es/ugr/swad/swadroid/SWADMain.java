/*
 *  This file is part of SWADroid.
 *
 *  Copyright (C) 2010 Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 *
 *  SWADroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SWADroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SWADroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.ugr.swad.swadroid;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import es.ugr.swad.swadroid.Global;
import es.ugr.swad.swadroid.model.Course;
import es.ugr.swad.swadroid.model.DataBaseHelper;
import es.ugr.swad.swadroid.model.Model;
import es.ugr.swad.swadroid.modules.Courses;
import es.ugr.swad.swadroid.modules.Groups;
import es.ugr.swad.swadroid.modules.Messages;
import es.ugr.swad.swadroid.modules.Module;
import es.ugr.swad.swadroid.modules.Notices;
import es.ugr.swad.swadroid.modules.attendance.Attendance;
import es.ugr.swad.swadroid.modules.downloads.DownloadsManager;
import es.ugr.swad.swadroid.modules.downloads.DirectoryTreeDownload;
import es.ugr.swad.swadroid.modules.notifications.Notifications;
import es.ugr.swad.swadroid.modules.tests.Tests;
import es.ugr.swad.swadroid.ssl.SecureConnection;
import es.ugr.swad.swadroid.sync.AccountAuthenticator;

/**
 * Main class of the application.
 * @author Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 * @author Antonio Aguilera Malagon <aguilerin@gmail.com>
 * @author Helena Rodríguez Gijon <hrgijon@gmail.com>
 */
public class SWADMain extends MenuExpandableListActivity {
	/**
	 * Array of strings for main ListView
	 */
	protected String[] functions;
	/**
	 * Function name field
	 */
	final String NAME = "listText";
	/**
	 * Function text field
	 */
	final String IMAGE = "listIcon";
	/**
	 * Code of selected course
	 * */
	long courseCode;
	/**
	 * Cursor for database access
	 */
	private Cursor dbCursor;
	/**
	 * User courses list
	 */
	private List<Model>listCourses;
	/**
	 * Tests tag name for Logcat
	 */
	public static final String TAG = Global.APP_TAG;
	/**
	 * Indicates if it is the first run
	 * */
	private boolean firstRun = false;

	/**
	 * Current role 2 - student 3 - teacher -1 - none role was chosen 
	 * */
	private int currentRole = -1;
	/**
	 * Group position inside the main menu for Messages group
	 * */
	private int MESSAGES_GROUP = 0;
	/**
	 * Group position inside the main menu for Evaluation group
	 * */
	private int EVALUATION_GROUP = 1;
	/**
	 * Group position inside the main menu for Course group
	 * */
	private int COURSE_GROUP = 2;
	/**
	 * Group position inside the main menu for User group
	 * */
	private int USERS_GROUP = 3;
	/**
	 * Child position inside the messages menu for Notification
	 * */
	private int NOTIFICATION_CHILD = 0;
	/**
	 * Child position inside the messages menu for Send message
	 * */
	private int SEND_MESSAGES_CHILD = 1;
	/**
	 * Child position inside the messages menu for Publish Note
	 * */
	private int PUBLISH_NOTE_CHILD = 2;
	/**
	 * Child position inside the evaluation menu for Tests
	 * */
	private int TESTS_CHILD = 0;
	/**
	 * Child position inside the course menu for Documents
	 * */
	private int DOCUMENTS_CHILD = 0;
	/**
	 * Child position inside the course menu for Shared area
	 * */
	private int SHARED_AREA_CHILD = 1;
	/**
	 * Child position inside the users menu for Rollcall
	 * */
	private int ROLLCALL_CHILD = 0;


	/**
	 * Gets the database helper
	 * @return the database helper
	 */
	public static DataBaseHelper getDbHelper() {
		return dbHelper;
	}

	/**
	 * Shows configuration dialog on first run.
	 */
	public void showConfigurationDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.initialDialogTitle)
		.setMessage(R.string.firstRunMsg)
		.setCancelable(false)
		.setPositiveButton(R.string.yesMsg, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				viewPreferences();
			}
		})
		.setNegativeButton(R.string.noMsg, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				createSpinnerAdapter();
			}
		}).show();
	}

	/**
	 * Shows initial dialog after application upgrade.
	 */
	public void showUpgradeDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.initialDialogTitle)
		.setMessage(R.string.upgradeMsg)
		.setCancelable(false)
		.setNeutralButton(R.string.close_dialog, null)
		.show();
	}

	/* (non-Javadoc)
	 * @see android.app.ExpandableListActivity#onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long)
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		// Get the item that was clicked
		Object o = this.getExpandableListAdapter().getChild(groupPosition, childPosition);
		@SuppressWarnings("unchecked")
		String keyword = (String) ((Map<String,Object>)o).get(NAME);

		Intent activity;
		if(keyword.equals(getString(R.string.notificationsModuleLabel))) {
			activity = new Intent(getBaseContext(), Notifications.class);
			startActivityForResult(activity, Global.NOTIFICATIONS_REQUEST_CODE);				
		} else if(keyword.equals(getString(R.string.testsModuleLabel))) {
			activity = new Intent(getBaseContext(), Tests.class);
			startActivityForResult(activity, Global.TESTS_REQUEST_CODE);
		} else if(keyword.equals(getString(R.string.messagesModuleLabel))) {
			activity = new Intent(getBaseContext(), Messages.class);
			activity.putExtra("notificationCode", new Long(0));
			startActivityForResult(activity, Global.MESSAGES_REQUEST_CODE);
		} else if(keyword.equals(getString(R.string.noticesModuleLabel))){
			activity = new Intent(getBaseContext(), Notices.class);
			startActivityForResult(activity, Global.NOTICES_REQUESET_CODE);
		} else if(keyword.equals(getString(R.string.attendanceModuleLabel))) {
			activity = new Intent(getBaseContext(), Attendance.class);
			startActivityForResult(activity, Global.ATTENDANCE_REQUEST_CODE);
		} else if(keyword.equals(getString(R.string.documentsDownloadModuleLabel))){
			activity = new Intent(getBaseContext(), DownloadsManager.class);
			activity.putExtra("downloadsCode", Global.DOCUMENTS_AREA_CODE);
			startActivityForResult(activity,Global.DOWNLOADSMANAGER_REQUEST_CODE);
			
		}else if(keyword.equals(getString(R.string.sharedsDownloadModuleLabel))){
			activity = new Intent(getBaseContext(), DownloadsManager.class);
			activity.putExtra("downloadsCode", Global.SHARE_AREA_CODE);
			startActivityForResult(activity,Global.DOWNLOADSMANAGER_REQUEST_CODE);
			/*activity = new Intent(getBaseContext(),DirectoryTreeDownload.class);
			activity.putExtra("treeCode",Global.SHARE_AREA_CODE);
			startActivityForResult(activity,Global.DIRECTORY_TREE_REQUEST_CODE);*/
		}

		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		prefs.getPreferences(getBaseContext());
		if(!Global.isPreferencesChanged()){
			createSpinnerAdapter();
			if(!firstRun){
				courseCode = Global.getSelectedCourseCode();
				createMenu();
			}
		}else{
			getCurrentCourses();
			Global.setPreferencesChanged(false);
		}
	}



	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate()
	 */
	@Override
	public void onCreate(Bundle icicle) {
		int lastVersion, currentVersion;
		ImageView image;
		TextView text;

		//Initialize screen
		super.onCreate(icicle);
		setContentView(R.layout.main);

		image = (ImageView)this.findViewById(R.id.moduleIcon);
		image.setBackgroundResource(R.drawable.ic_launcher_swadroid);

		text = (TextView)this.findViewById(R.id.moduleName);
		text.setText(R.string.app_name);

		try {            
			//Initialize database
			/*db = DataFramework.getInstance();
            db.open(this, this.getPackageName());
            dbHelper = new DataBaseHelper(db);*/

			//Initialize preferences
			prefs.getPreferences(getBaseContext()); 

			//Initialize HTTPS connections 
			SecureConnection.initSecureConnection();

			//Check if this is the first run after an install or upgrade
			lastVersion = prefs.getLastVersion();
			currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

			//If this is the first run, show configuration dialog
			if(lastVersion == 0) {
				showConfigurationDialog();
				dbHelper.initializeDB();
				//prefs.upgradeCredentials();
				
				//Configure automatic synchronization
				Intent activity = new Intent(getBaseContext(), AccountAuthenticator.class);
				startActivity(activity);

				prefs.setLastVersion(currentVersion);
				firstRun = true;
				Global.setSelectedCourseCode(-1);

				Global.setSelectedCourseShortName("");
				Global.setSelectedCourseFullName("");

				//If this is an upgrade, show upgrade dialog
			} else if(lastVersion < currentVersion) {
				//showUpgradeDialog();
				dbHelper.upgradeDB(this);
				//prefs.upgradeCredentials();

				//Configure automatic synchronization
				Intent activity = new Intent(getBaseContext(), AccountAuthenticator.class);
				startActivity(activity);

				prefs.setLastVersion(currentVersion);
			}
			listCourses = dbHelper.getAllRows(Global.DB_TABLE_COURSES,"","name");
			if(listCourses.size() >0){
				Course c =(Course) listCourses.get(prefs.getLastCourseSelected());
				Global.setSelectedCourseCode(c.getId());
				Global.setSelectedCourseShortName(c.getShortName());
				Global.setSelectedCourseFullName(c.getFullName());
				
			}else{
				Global.setSelectedCourseCode(-1);
				Global.setSelectedCourseShortName("");
				Global.setSelectedCourseFullName("");
				if(!firstRun && Module.connectionAvailable(this)) getCurrentCourses(); //at the first run, this will be launched after the preferences menu 
			}
			currentRole = -1;
		} catch (Exception ex) {
			error(ex.getMessage());
			ex.printStackTrace();
		} 
		/*if(!firstRun && Module.connectionAvailable(this)){
			getActualCourses();
		}*/

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == Activity.RESULT_OK){
			switch(requestCode){
			//After get the list of courses, a dialog is launched to choice the course
			case Global.COURSES_REQUEST_CODE:
				createSpinnerAdapter();
				createMenu();
				break;
			}
		}
	}

	private void createSpinnerAdapter(){
		Spinner spinner = (Spinner) this.findViewById(R.id.spinner);
		listCourses = dbHelper.getAllRows(Global.DB_TABLE_COURSES, null, "name");
		dbCursor =  dbHelper.getDb().getCursor(Global.DB_TABLE_COURSES, null, "name");
		startManagingCursor(dbCursor);
		if(listCourses.size() != 0){
			SimpleCursorAdapter adapter = new SimpleCursorAdapter (this,
					android.R.layout.simple_spinner_item, 
					dbCursor, 
					new String[]{"name"}, 
					new int[]{android.R.id.text1});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(new onItemSelectedListener());
			if(Global.getSelectedCourseCode()!=-1){
				boolean found = false;
				int i=0;
				while(!found && i <listCourses.size()){
					if(listCourses.get(i).getId() == Global.getSelectedCourseCode()){
						found=true;
					}else{
						++i;
					}
				}
				if(i<listCourses.size())
					spinner.setSelection(i);
			}
		} else {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{getString(R.string.clickToGetCourses)});
			spinner.setAdapter(adapter);
		}
		spinner.setOnTouchListener(Spinner_OnTouch);

	}

	private class onItemSelectedListener implements OnItemSelectedListener{

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position,
				long id) {
			prefs.setLastCourseSelected(position);
			Course courseSelected = (Course)listCourses.get(position);
			courseCode = courseSelected.getId();
			Global.setSelectedCourseCode(courseCode);
			Global.setSelectedCourseShortName(courseSelected.getShortName());
			Global.setSelectedCourseFullName(courseSelected.getFullName());
			createMenu();

		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub

		}

	}
	private View.OnTouchListener Spinner_OnTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {

				if(dbHelper.getAllRows(Global.DB_TABLE_COURSES).size()==0){
					if(Module.connectionAvailable(getBaseContext()))
						getCurrentCourses();
					//else

				}else{
					v.performClick();
				}

			}
			return true;
		}
	};

	private void getCurrentCourses(){
		Intent activity;
		activity = new Intent(getBaseContext(), Courses.class );
		Toast.makeText(getBaseContext(), R.string.coursesProgressDescription, Toast.LENGTH_LONG).show();
		startActivityForResult(activity,Global.COURSES_REQUEST_CODE);
	}

	private void createMenu(){
		if(listCourses.size() != 0){
			Course courseSelected;
			if(Global.getSelectedCourseCode()!=-1){
				String where = "id="+String.valueOf(Global.getSelectedCourseCode());
				courseSelected = (Course) dbHelper.getAllRows(Global.DB_TABLE_COURSES, where, "name").get(0);
			}else{
				courseSelected = (Course) listCourses.get(0);
				Global.setSelectedCourseCode(courseSelected.getId());
				Global.setSelectedCourseShortName(courseSelected.getShortName());
				Global.setSelectedCourseFullName(courseSelected.getFullName());
				prefs.setLastCourseSelected(0);
			}

			if(courseSelected != null){
				if(getExpandableListAdapter() == null)
					createBaseMenu();
				int userRole = courseSelected.getUserRole();
				if(userRole == Global.TEACHER_TYPE_CODE && currentRole != Global.TEACHER_TYPE_CODE) 
					changeToTeacherMenu();
				if(userRole == Global.STUDENT_TYPE_CODE && currentRole != Global.STUDENT_TYPE_CODE) 
					changeToStudentMenu();
			}
		}
	}

	/**
	 * Creates base menu. The menu base is common for students and teachers.
	 * Sets currentRole to student role
	 * */
	private void createBaseMenu(){
		if(getExpandableListAdapter() == null || currentRole==-1){
			//the menu base is equal to students menu. 
			currentRole = Global.STUDENT_TYPE_CODE; 
			//Construct Expandable List
			final ArrayList<HashMap<String, Object>> headerData = new ArrayList<HashMap<String, Object>>();

			final HashMap<String, Object> messages = new HashMap<String, Object>();
			messages.put(NAME, getString(R.string.messages));
			messages.put(IMAGE, getResources().getDrawable(R.drawable.msg));
			headerData.add( messages );

			final HashMap<String, Object> evaluation = new HashMap<String, Object>();
			evaluation.put(NAME, getString(R.string.evaluation));
			evaluation.put(IMAGE, getResources().getDrawable(R.drawable.grades));
			headerData.add( evaluation);
			
			//DISABLE until it will be functional
			final HashMap<String, Object> courses = new HashMap<String,Object>();
			courses.put(NAME, getString(R.string.course));
			courses.put(IMAGE, getResources().getDrawable(R.drawable.blackboard));
			headerData.add(courses);

			final ArrayList<ArrayList<HashMap<String, Object>>> childData = new ArrayList<ArrayList<HashMap<String, Object>>>();

			final ArrayList<HashMap<String, Object>> messagesData = new ArrayList<HashMap<String, Object>>();
			childData.add(messagesData);

			final ArrayList<HashMap<String, Object>> evaluationData = new ArrayList<HashMap<String, Object>>();
			childData.add(evaluationData);

			//DISABLE until it will be functional
			final ArrayList<HashMap<String,Object>> documentsData = new ArrayList<HashMap<String, Object>>();
			childData.add(documentsData);

			//Messages category
			HashMap<String, Object> map = new HashMap<String,Object>();
			map.put(NAME, getString(R.string.notificationsModuleLabel) );
			map.put(IMAGE, getResources().getDrawable(R.drawable.notif));
			messagesData.add(map); 

			map = new HashMap<String,Object>();        
			map.put(NAME, getString(R.string.messagesModuleLabel) );
			map.put(IMAGE, getResources().getDrawable(R.drawable.msg_write));
			messagesData.add(map);

			//Evaluation category
			map = new HashMap<String,Object>();
			map.put(NAME, getString(R.string.testsModuleLabel) );
			map.put(IMAGE, getResources().getDrawable(R.drawable.test));
			evaluationData.add(map);

			//DISABLE until it will be functional
			//Documents category
			map = new HashMap<String,Object>();
			map.put(NAME, getString(R.string.documentsDownloadModuleLabel));
			map.put(IMAGE,  getResources().getDrawable(R.drawable.folder));
			documentsData.add(map);
			//shared area category
			map = new HashMap<String,Object>();
			map.put(NAME, getString(R.string.sharedsDownloadModuleLabel));
			map.put(IMAGE,  getResources().getDrawable(R.drawable.folderusers));
			documentsData.add(map);
			setListAdapter( new ImageExpandableListAdapter(
					this,
					headerData,
					R.layout.image_list_item,
					new String[] { NAME },            // the name of the field data
					new int[] { R.id.listText }, // the text field to populate with the field data
					childData,
					0,
					null,
					new int[] {}
					));

			getExpandableListView().setOnChildClickListener(this);
		}
	}

	/**
	 * Adapts the current menu to students view. Removes options unique to teachers and adds options unique to students
	 */
	private void changeToStudentMenu()
	{
		if(currentRole == Global.TEACHER_TYPE_CODE){
			//Removes Publish Note from messages menu
			((ImageExpandableListAdapter) getExpandableListAdapter()).removeChild(MESSAGES_GROUP, PUBLISH_NOTE_CHILD);
			//Removes completely users menu 
			//DISABLE until it will be functional
			((ImageExpandableListAdapter) getExpandableListAdapter()).removeGroup(USERS_GROUP);

		}
		currentRole = Global.STUDENT_TYPE_CODE;
	}
	/**
	 * Adapts the current menu to teachers view. Removes options unique to students and adds options unique to teachers
	 */
	private void changeToTeacherMenu()
	{
		if(currentRole == Global.STUDENT_TYPE_CODE){
			HashMap<String, Object> map  = new HashMap<String,Object>();        
			map.put(NAME, getString(R.string.noticesModuleLabel) );
			map.put(IMAGE, getResources().getDrawable(R.drawable.note));
			((ImageExpandableListAdapter) getExpandableListAdapter()).addChild(MESSAGES_GROUP,PUBLISH_NOTE_CHILD, map);

			//DISABLE until it will be functional
			final HashMap<String, Object> users = new HashMap<String, Object>();
			users.put(NAME, getString(R.string.users));
			users.put(IMAGE, getResources().getDrawable(R.drawable.users));
			ArrayList<HashMap<String,Object>> child = new ArrayList<HashMap<String, Object>>();  
			map = new HashMap<String,Object>();
			map.put(NAME, getString(R.string.attendanceModuleLabel));
			map.put(IMAGE, getResources().getDrawable(R.drawable.rollcall));
			child.add(map);
			((ImageExpandableListAdapter) getExpandableListAdapter()).addGroup(USERS_GROUP, users, child);


		}
		currentRole = Global.TEACHER_TYPE_CODE;
	}

}