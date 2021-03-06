package com.example.chatapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This is the Activity where the User can see all his chat-partners. 
 * There are options to add and delete Chat-Partners and (un-)subscribe 
 * them from Roster:
 * http://www.igniterealtime.org/builds/smack/docs/latest/javadoc/org/jivesoftware/smack/Roster.html
 */
public class MainChatActivity extends Activity {
	
	// Log Tags
	private static final String NEW_USER_TAG = "Add User Error";
	private static final String DELETE_USER_TAG = "Delete User Error";
	// Request Tag for changing to another Activity
	static final int PICK_ACCOUNT_REQUEST = 1;
	static final int CHAT_ROOM_REQUEST = 2;
	// GUI elements
	private ListView 	   listView;
	private Button 		   addButton;
	// Members for handling chatPartners
	private XMPPConnection connection;
	private Roster		   chatPartners;
	private ArrayList<ChatPartner> chats;
	private static Context context;
	private ArrayAdapter<ChatPartner> adapter;
	private String currentChatPartner;
	private NotificationManager notifyer;
	private int notificationID = 1;

	
	/**
	 * onCreate Android Lifecycle Method
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_chat);
		
		// Initialize Members
		connection = Login.getConnection();
		setTitle("Welcome "+ connection.getUser().split("/")[0]);
		chats = new ArrayList<ChatPartner>();
		chatPartners = this.connection.getRoster();
		addButton = (Button) findViewById(R.id.addChatPartner);
		listView = (ListView) findViewById(R.id.chatPartners);
		context = this;
		currentChatPartner = "";
		notifyer = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		// Get all ChatPartners and load them into Array
		Collection<RosterEntry> rosterEntries = chatPartners.getEntries();
		for(RosterEntry e : rosterEntries)
		{
			chats.add(new ChatPartner(e.getUser(),e.getName()));
		}
		
		// Set ListView - Adapter
		adapter = new ArrayAdapter<ChatPartner>(this, R.layout.chat_partner_view, R.id.chat_partner_item_view, chats);
		listView.setAdapter(adapter);
		registerForContextMenu(listView);
		initialiseListeners();
				
	}
	
	
	
	/**
	 * Android Method
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_chat, menu);
		return true;
	}
	
	
	
	/**
	 * Creates Context Menu for ListView
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) 
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Delete User");
	}
	
	
	
	
	/**
	 * Handles Context Menu Item clicks
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{
		 super.onContextItemSelected(item);
		 if (item.getTitle().equals("Delete User"))
		 {
			 AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			 removeUser(chats.get(info.position));
		 }
		 
		 return true;
	}
	
	
	
	/**
	 * Method for keeping track of the current Activity we are in. Is called by other Activites.
	 * 
	 * @param newContext is the Activity we are currently in. Is used to correctly display Toasts and Dialogs
	 *  				 even though the Packet and Message Listeners are initialised here, and not in ChatRoom- or ChooseAccountAcitivty
	 */
	public static void setContext(Context newContext)
	{
		context = newContext;
	}
	
	
	
	/**
	 * Sets up a notificationBuilder that is shown when there is an incoming message
	 */
	private NotificationCompat.Builder getNotificationBuilder(Context context, String userID)
	{
		return new NotificationCompat.Builder(context)
							.setSmallIcon(R.drawable.ic_launcher)
							.setContentTitle("New Message")
							.setContentText("New Message received from " + userID)
							.setNumber(notificationID);
	}
	
	
	
	/**
	 * If the User presses back, he will be logged out from Server
	 */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (connection != null)
		{
			connection.disconnect();
			connection = null;
		}
	}
	
	
	
	/**
	 * Sets all Listeners
	 */
	private void initialiseListeners() 
	{
		// Handle subscriptions
		connection.addPacketListener(new PacketListener() 
		{
			@Override
			public void processPacket(Packet packet) 
			{
				handlePresencePackets(packet);
			}
		}, new PacketTypeFilter(Presence.class));
		
		// Handle Messages that come outside of ChatRoom
		connection.addPacketListener(new PacketListener() 
		{
			@Override
			public void processPacket(Packet packet) 
			{
				handleMessagePackets(packet);
			}
		}, new MyPacketFilter());
		
		// Handle short ListView Clicks
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) 
			{
				// Get Clicked User, and start Chatroom
				ChatPartner clickedUser = (ChatPartner) parent.getItemAtPosition(position);
				handleShortListViewClick(view.getContext(), clickedUser);
			}
		});
		
		
		
		// Let User choose account from his phonebook
		addButton.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(v.getContext(), ChooseAccountActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
			}
		});
	}

	
	
	/**
	 * Goes into ChatRoom with selected User
	 * 
	 * @param viewContext is the Context, the ListView was clicked in
	 * @param clickedUser is the User we want to chat with
	 */
	private void handleShortListViewClick(Context viewContext, ChatPartner clickedUser) 
	{
		Intent intent = new Intent(viewContext, ChatRoomActivity.class);
		intent.putExtra("chosenChatPartnerEmail", clickedUser.getEmail());
		intent.putExtra("chosenChatPartnerName", clickedUser.getName());
		intent.putStringArrayListExtra("messagesOutsideChatroom", clickedUser.getMessages());
		
		// Remember that we are in a ChatRoom with somebody
		currentChatPartner = clickedUser.getEmail();
		
		startActivityForResult(intent, CHAT_ROOM_REQUEST);
	}


	
	/**
	 * Stores received Messages that come from Users, we currently are NOT in a ChatRoom with
	 * 
	 * @param packet is the Message Packet that was sent to us
	 */
	private void handleMessagePackets(Packet packet) {
		
		Log.i("MESSAGE HANDLER", "Message received");
		Message message = (Message) packet;
		Log.i("MESSAGE HANDLER", "Message from " + message.getFrom() + " with type: " + message.getType());
		// Split getFrom()-String or else "user@qip.ru/HOST" is returned, which isn�t found
		int index = getChatIndex(message.getFrom().split("/")[0]);
		
		if (index == -1)
		{
			// -1 means User isn�t in Roster yet
			
		}
		else
		{
			// Add Message to "offlineMessages" of the corresponding ChatPartner
			Log.i("MESSAGE HANDLER", "Added Message to " + chats.get(index).getEmail()+"�s ChatRoom");
			if (message.getBody() != null)
			{
				chats.get(index).addMessage(message.getBody());
				notifyer.notify(notificationID, getNotificationBuilder(context, message.getFrom().split("/")[0]).build());
			}
			else
			{
				Log.i("MESSAGE HANDLER", "Message Body was null");
			}
		}
	}


	
	/**
	 * Handles Subscription Requests from other Users. (Un-)Subscribes to other Users, depending on the User Input
	 * 
	 * @param packet is the Presence Packet that was sent to us
	 */
	private void handlePresencePackets(Packet packet) {
		Presence presence = (Presence)packet;
		final String sender = presence.getFrom();
		boolean inRoster = chatPartners.contains(presence.getFrom());
		Log.i("Presence Listener", "Received Presence Packet from " + presence.getFrom() + " with Type " + presence.getType() + 
									". Is this person already in Roster? " + Boolean.toString(inRoster));
		
		switch (presence.getType()) 
		{
			case subscribe:
				if (inRoster)
				{
					Presence response = new Presence(Presence.Type.subscribed);
					response.setTo(sender);
					connection.sendPacket(response);
				}
				else
				{
					showSubscriptionDialog(sender);
				}
				break;

			case unsubscribed:
				showToast("User "+ sender +" unsubscribed from you and is deleted from your List", false);
				removeUser(new ChatPartner(sender, chatPartners.getEntry(sender).getName()));
				break;
				
			// TODO Handle Other Presence PacketTypes
				
			default:
				Log.i("Presence Listener", "Not handled!");
		}
	}
	
	
	
	/**
	 * Displays a Toast on the UI Thread
	 * @param long 
	 * 
	 * @param string the Messages which is posted
	 */
	private void showToast(final String message, final boolean longToastLength) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				int toast_length = longToastLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
				Toast.makeText(context, message, toast_length).show();
			}
		});
	}


	
	/**
	 * Displays a Dialog to the User when a Subscription Request is incoming
	 * 
	 * @param sender who wanted to subscribe
	 */
	private void showSubscriptionDialog(final String sender) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(context)
				.setTitle("New Subscription Request")
				.setMessage(sender + " sent a subscription request. Do you want to start chatting with this User?")
				.setCancelable(false)
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Don�t accept subscription request
						Presence unSubscribe = new Presence(Presence.Type.unsubscribe);
						Presence unSubscribed = new Presence(Presence.Type.unsubscribed);
						unSubscribe.setTo(sender);
						unSubscribed.setTo(sender);
						connection.sendPacket(unSubscribe);
						connection.sendPacket(unSubscribed);
						dialog.cancel();
					}
				})
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Accept request and add User
						Presence subscribe = new Presence(Presence.Type.subscribe);
						Presence subscribed = new Presence(Presence.Type.subscribed);
						subscribe.setTo(sender);
						subscribed.setTo(sender);
						connection.sendPacket(subscribe);
						connection.sendPacket(subscribed);
						addUser(sender, sender.split("@")[0]);
						dialog.cancel();
					}
				}).show();
			}
		});
	}


	
	/**
	 *  Is Called when another Activity returns. Adds chosen User to Roster if he exists or updates Messages
	 *  ArrayList for the User we were just in the ChatRoom with
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    
	    // Keep Track of Context
	    context = this;
	    
		switch(requestCode) 
		{
			case PICK_ACCOUNT_REQUEST:
				if (resultCode == RESULT_OK)
				{
					String userEmail = data.getStringExtra("chosenContactEmail");
					String userName  = data.getStringExtra("chosenContactName");
					Log.i("Data given back", userEmail + " " + userName);
					if (userExists(userEmail))
					{
						addUser(userEmail, userName);
					}
				}
				else
				{
					Toast.makeText(this, "No User Selected", Toast.LENGTH_SHORT).show();
				}
				break;
			
			case CHAT_ROOM_REQUEST:
				ArrayList<String> messages = data.getStringArrayListExtra("Messages");
				int chatPartnerIndex = getChatIndex(currentChatPartner);
				chats.get(chatPartnerIndex).updateMessages(messages);
				break;
				
			default:
				Log.i("Context Tracker", "Returned to MainChatActivity");
		}
		
		// Keep track of chatPartner
		currentChatPartner = "";
	}


	
	/**
	 * Searches for the index a userID in our Array of ChatPartners
	 * 
	 * @param userID is the User we want to search
	 * @return the index of the userID we wanted to search, or -1 if userID was not found
	 */
	private int getChatIndex(String userID) {
		for(int i = 0; i < chats.size(); i++)
		{
			if (chats.get(i).getEmail().equals(userID))
				return i;
		}
		return -1;
	}

	

	
	/**
	 * Add new ChatPartner to Array and subscribe to Server
	 * 
	 * @param email is the userID of the new ChatPartner
	 * @param name is the name that the new ChatPartner is referred to
	 */
	private void addUser(final String email, final String name)
	{
		try 
		{
			if (! chats.contains(new ChatPartner(email, name)))
			{
				Presence response = new Presence(Presence.Type.subscribe);
				response.setTo(email);
				Log.i("PRESENCE_TAG", "Sending subscribe request to: "+response.getTo());
				connection.sendPacket(response);
				chatPartners.createEntry(email, name, null);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.add(new ChatPartner(email, name));
						adapter.notifyDataSetChanged();
					}
				});
			}
			else
			{
				Toast.makeText(this, "User already in Roster!", Toast.LENGTH_SHORT).show();
			}
		} 
		catch (XMPPException e) 
		{
			Log.e(NEW_USER_TAG, "Error when trying to add new User to Roster");
			Log.e(NEW_USER_TAG, e.toString());
			Toast.makeText(this, "User: "+ name + " could not be added! Try Again!", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	
	/**
	 * Remove user from Array and unsubscribe
	 *  
	 * @param user is the User we want to delete
	 * @return 
	 */
	private boolean removeUser(final ChatPartner user)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try 
				{
					chatPartners.removeEntry(chatPartners.getEntry(user.getEmail()));
					adapter.remove(user);
					adapter.notifyDataSetChanged();
				}
				catch (XMPPException e)
				{
					Log.e(DELETE_USER_TAG, "Error when removing User" + user.getName());
					Log.e(DELETE_USER_TAG, e.toString());
				}
			}
		});
		return true;
	}
	
	
	
	/**
	 * Check if User exists on server
	 * 
	 * @param userID is the User we want to search for on the Server
	 * @return true if User exists on Server
	 */
	private boolean userExists(String userID)
	{
		try 
		{
			UserSearchManager searchManager = new UserSearchManager(this.connection);
	
			String searchService = (String) searchManager.getSearchServices().toArray()[0];
			
			Form searchForm = searchManager.getSearchForm(searchService);
			Form answerForm = searchForm.createAnswerForm();
			
			// Split username@service.com into username*, or else search doesn�t work
			String usernameWithoutDomain = userID.split("@")[0] + "*";
			answerForm.setAnswer("user", usernameWithoutDomain);
			
			// Get Data from SearchService
			ReportedData resultData = searchManager.getSearchResults(answerForm, searchService);
			if (resultData.getRows() != null)
			{	
				Iterator<Row> iter = resultData.getRows();
				if (!iter.hasNext())
				{
					Toast.makeText(this, "User " + userID + " does not exist!", Toast.LENGTH_SHORT).show();
					return false;
				}
				else
				{
					Row row = iter.next();
                	Iterator iterator = row.getValues("jid");
                    if(iterator.hasNext() && iterator.next().toString().equals(userID) && !iter.hasNext())
                    {
        				Toast.makeText(this, "User " + userID + " exists and is added to your Chat Partners!", Toast.LENGTH_SHORT).show();
                    	return true;
                    }
                    else
                    {
                    	Toast.makeText(this, "User " + userID + "not unique! Please give full name", Toast.LENGTH_SHORT).show();
                    	return false;
                    }
				}
			}
			else
			{
				Toast.makeText(this, "User " + userID + " does not exist!", Toast.LENGTH_SHORT).show();
				return false;
			}
		
		}
		catch (XMPPException e)
		{
			Log.e(NEW_USER_TAG, e.toString());
			Log.e(NEW_USER_TAG, connection.getServiceName());
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			Log.e(NEW_USER_TAG, e.toString());
			e.printStackTrace();
			return false;
		}
	}

	
	
	
	/**
	 * Implements a Filter that accepts all Messages except for those from the User, we are 
	 * currently in a ChatRoom with
	 */
	private class MyPacketFilter implements PacketFilter 
	{
		@Override
		public boolean accept(Packet packet) 
		{
			if (packet.getClass() == Message.class)
			{
				String from = packet.getFrom().split("/")[0];
				return (! from.equals(currentChatPartner));
			}
			return false;
		}
	}
}