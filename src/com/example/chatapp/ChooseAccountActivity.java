package com.example.chatapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * This Activity lets the User choose an Account with a gmail account. The Result is given back to the MainChatActivity
 * Example that was used: http://developer.android.com/training/contacts-provider/index.html
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ChooseAccountActivity extends FragmentActivity implements LoaderCallbacks<Cursor>
{
	// Stores the query Data for ListView Adapter
	private final static String[] FROM_COLUMNS = 
	{
		Email.DISPLAY_NAME_PRIMARY,
		Email.ADDRESS
	};
	// Tells Adapter where to put the query Data
	private final static int[] TO_IDS = 
	{
		R.id.usernameTextView,
		R.id.emailTextView
	};

	// Query Projection: this is the data we get back from the cursor
	private final static String[] PROJECTION = 
	{
		Contacts._ID,
		Contacts.DISPLAY_NAME,
		Email.DATA
	};
	
	// The Cursor Column Indexes in order to tell the Cursor, which data we want
	private static final int CONTACT_ID_INDEX = 0;
	private static final int CONTACT_DISPLAY_NAME_INDEX = 1;
	private static final int CONTACT_EMAIL_ADDRESS_INDEX = 2;
	
	// Restricts Query: Only give back not-empty Email-Addresses
	private final static String SELECTION = "(" + Email.DATA + " NOT LIKE '')";
	
	// ListView and corresponding Adapter
	private ListView  accountView;
    private SimpleCursorAdapter cursorAdapter;	
    
    // GUI Elements
    private EditText emailInput, nameInput;
    private Button   addButton;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_account);
		
		// Initializes loading the Contact-Data
		getSupportLoaderManager().initLoader(0, null, this);
		
		// Setup GUI Members
		emailInput = (EditText) findViewById(R.id.newUserEmailInput);
		nameInput  = (EditText) findViewById(R.id.newUserNameInput);
		addButton  = (Button)   findViewById(R.id.addNewUserButton);
		
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String contactName  = nameInput.getText().toString();
				String contactEmail = emailInput.getText().toString();
				
				// Put the extra information into the Result Intent and leave
				changeActivityWithResult(contactName, contactEmail);
			}
		});
		
		// Setup ListView and Adapter
		accountView = (ListView) findViewById(R.id.accountList);
		cursorAdapter = new SimpleCursorAdapter(this, R.layout.account_view, null, FROM_COLUMNS, TO_IDS, 0);
		accountView.setAdapter(cursorAdapter);
		
		accountView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// When an Item is clicked, name and email are queried and extracted and given back to MainChatActivity
				Cursor cursor = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
				cursor.moveToPosition(position);
				
				String contactName = cursor.getString(CONTACT_DISPLAY_NAME_INDEX);
				String contactEmail  = cursor.getString(CONTACT_EMAIL_ADDRESS_INDEX);
				
				// Put the extra information into the Result Intent and leave
				changeActivityWithResult(contactName, contactEmail);
			}
		});
		
		
		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_account, menu);
		return true;
	}
	
	
	
	// Store extra information in the Intent and go back to MainChatActivity
	private void changeActivityWithResult(String extraNameString, String extraEmailString)
	{
		Log.i("CHANGE ACTIVITY", "Changing acitivty now");
		Intent extraData = new Intent();
		extraData.putExtra("chosenContactName", extraNameString);
		extraData.putExtra("chosenContactEmail", extraEmailString);
		setResult(RESULT_OK, extraData);
		ChooseAccountActivity.this.finish();
	}
	
	

	/**
	 * These next functions are required to implement LoaderCallbacks<Cursor> Interface: 
	 * They load, update, and clean the cursor
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// Return cursor to query
		return new CursorLoader(ChooseAccountActivity.this, Email.CONTENT_URI, PROJECTION, SELECTION, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// Update cursor
		cursorAdapter.swapCursor(cursor);		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// Delete references to previous cursor
		cursorAdapter.swapCursor(null);
	}
}
