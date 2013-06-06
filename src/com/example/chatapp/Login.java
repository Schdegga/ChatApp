package com.example.chatapp;

import org.jivesoftware.smack.AndroidConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * This Activity represents a Login formula.
 * It retrieves a Google Account via two TextEdits and then 
 * connects and logs in to talk.google.com (Google Talk XMPP Server)
 */
public class Login extends Activity {

	 // Server Connection Data.
	 // See https://developers.google.com/talk/open_communications#developer 
	public static final String HOST    = "webim.qip.ru";
	public static final int    PORT    = 5222;
	//public static final String SERVICE = "gmail.com";
	
	// GUI Elements
	private EditText usernameInput, passwordInput;
	private Button   login;
	
	// Tags for Logging
	public static final String SERVER_CONNECTION_TAG = "Server Connection";
	public static final String USER_LOGIN_TAG  = "User Login";
	
	// Server connection
	private static XMPPConnection connection;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		usernameInput = (EditText) findViewById(R.id.usernameInput);
		passwordInput = (EditText) findViewById(R.id.passwordInput);
		login = (Button) findViewById(R.id.loginButton);
		connection = null;
		
		login.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String username = usernameInput.getText().toString();
				String password = passwordInput.getText().toString();
				// Login User
				login(username, password);
			}
		});
		
		
	}

	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
		if (connection != null)
		{
			Log.i("Login (On Destroy)", "Conenction is destroyed");
			connection.disconnect();
			connection = null;
		}
	}
	
	
	
	public static XMPPConnection getConnection()
	{
		return connection;
	}
	
	
	
	/**
	 * Is called if Login succeeds, switches to next Activity and sets connection
	 * @param connection
	 */
	private void startChatting(XMPPConnection connection)
	{
		if (connection == null)
		{
			// Dialogs and Toasts can only be displayed in UI-Thread, this functions comes from another Thread
			// (connectionThread), so runOnUIThread is required
			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					new AlertDialog.Builder(Login.this)
												.setTitle("No Connection established")
												.setMessage("Please try Logging in again, or check Internet connection")
												.setCancelable(false)
												.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
													
													@Override
													public void onClick(DialogInterface dialog, int which) {
														dialog.cancel();
														usernameInput.setText("");
														passwordInput.setText("");
													}
												})
												.show();
				}
			});
		}
		else
		{
			Login.connection = connection;
			// Set Availability to Online
			Presence presence = new Presence(Presence.Type.available);
			connection.sendPacket(presence);
			// Switch Activity
			Intent intent = new Intent(this, MainChatActivity.class);
			startActivity(intent);
		}
	}
	
	
	
	/**
	 * Creates connection to Server and Logs in User 
	 * @param username
	 * @param password
	 */
	private void login(final String username, final String password){
		
		// Show User that Work is done
		final ProgressDialog dialog = ProgressDialog.show(this, "Connecting to Server", "Please Wait", false);
		
		// Create Background-Thread that connects to Server
		Thread connectThread = new Thread(new Runnable() {
			@Override
			public void run() {
	
				// Connect to Server
				SmackAndroid.init(Login.this);
				AndroidConnectionConfiguration config = new AndroidConnectionConfiguration(HOST, PORT, "qip.ru");
				XMPPConnection newConnection = new XMPPConnection(config);
				try 
				{
					newConnection.connect();
					Log.i(SERVER_CONNECTION_TAG, "Successfully connected to Server at " + newConnection.getHost());
					SASLAuthentication.supportSASLMechanism("PLAIN", 0);
				} 
				catch (XMPPException e) 
				{
					Log.e(SERVER_CONNECTION_TAG, e.toString());
					startChatting(null);
				}
				
				// Login User
				try 
				{
					//TODO: Use real Username and Password
					// If your username is "username@service", just login with username and leave out @service
					newConnection.login("dampfhans", "1234567");
					Log.i(USER_LOGIN_TAG, "Logged in as: " + newConnection.getUser());
					dialog.dismiss();
					startChatting(connection);
				}
				catch (XMPPException e)
				{
					Log.e(USER_LOGIN_TAG, e.toString());
					e.printStackTrace();
					startChatting(null);
				}
				catch (Exception e)
				{
					Log.e(USER_LOGIN_TAG, e.toString());
					e.printStackTrace();
					startChatting(null);
				}
			}
			
		});
		
		// Show Dialog and start connecting
		dialog.show();
		connectThread.start();
	}
}
