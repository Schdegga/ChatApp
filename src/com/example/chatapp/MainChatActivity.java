package com.example.chatapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
	// GUI elements
	private ListView 	   listView;
	private Button 		   addButton;
	// Members for handling chatPartners
	private XMPPConnection connection;
	private Roster		   chatPartners;
	private ArrayList<ChatPartner> chats;
	private ChatManager    chatManager;


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
		
		
		// Handle subscriptions
		connection.addPacketListener(new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				handlePresencePackets(packet);
			}
		}, new PacketTypeFilter(Presence.class));
		
		
		/*
		 *
		// Handle Messages that come outside of ChatRoom
		connection.addPacketListener(new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				handleMessagePackets(packet);
			}
		}, new PacketTypeFilter(Message.class));
		*
		*
		*/
		
		
		// Get all ChatPartners and load them into Array
		Collection<RosterEntry> rosterEntries = chatPartners.getEntries();
		for(RosterEntry e : rosterEntries)
		{
			chats.add(new ChatPartner(e.getUser(),e.getName()));
		}
		
		// Set ListView - Adapter
		ArrayAdapter<ChatPartner> adapter = new ArrayAdapter<ChatPartner>(this, R.layout.chat_partner_view, R.id.chat_partner_item_view, chats);
		listView.setAdapter(adapter);
		
		// Handle ListItem Clicks
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				ChatPartner clickedUser = (ChatPartner) parent.getItemAtPosition(position);
				return removeUser(clickedUser);
			}
		});
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				
				// Get Clicked User, and start Chatroom
				ChatPartner clickedUser = (ChatPartner) parent.getItemAtPosition(position);
				Intent intent = new Intent(view.getContext(), ChatRoomActivity.class);
				intent.putExtra("chosenChatPartnerEmail", clickedUser.getEmail());
				intent.putExtra("chosenChatPartnerName", clickedUser.getName());
				intent.putStringArrayListExtra("messagesOutsideChatroom", clickedUser.getMessages());
				startActivity(intent);
			}
		});
		
		// Let User choose account from his phonebook
		addButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), ChooseAccountActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
			}
		});
	}
	
	
	private void handleMessagePackets(Packet packet) {
		
		Log.i("MESSAGE HANDLER", "Message received");
		Message message = (Message) packet;
		Log.i("MESSAGE HANDLER", "Message from " + message.getFrom());
		// Split getFrom()-String or else "user@qip.ru/HOST" is returned, which isn´t found
		int index = getChatIndex(message.getFrom().split("/")[0]);
		
		if (index == -1)
		{
			// -1 means User isn´t in Roster yet
			
		}
		else
		{
			// Add Message to "offlineMessages" of the corresponding ChatPartner
			Log.i("MESSAGE HANDLER", "Sent Message to " + chats.get(index).getEmail());
			chats.get(index).addMessage(message.getBody());
		}
		
	}


	// Find position of JabberID in ArrayList
	private int getChatIndex(String from) {
		for(int i = 0; i < chats.size(); i++)
		{
			if (chats.get(i).getEmail().equals(from))
				return i;
		}
		return -1;
	}


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
				showToast("User "+ sender +"unsubscribed from you and is deleted from your List");
				removeUser(new ChatPartner(sender, chatPartners.getEntry(sender).getName()));
				break;
				
			default:
				Log.i("Presence Listener", "Not handled!");
		}
		
		
		/*
		if (presence.getType() == Presence.Type.subscribe ||
			presence.getType() == Presence.Type.subscribed)
		{
			if (chatPartners.contains(presence.getFrom()))
			{
				Presence response = new Presence(Presence.Type.subscribed);
				response.setTo(presence.getFrom());
				connection.sendPacket(response);
			}
		}
		*/
	}
	
	
	
	/**
	 * Displays a Toast on the UI Thread
	 * @param string the Messages which is posted
	 */
	private void showToast(final String message) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(MainChatActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}


	
	/**
	 * Displays a Dialog to the User when a Subscription Request is incoming
	 * @param sender who wanted to subscribe
	 */
	private void showSubscriptionDialog(final String sender) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				new AlertDialog.Builder(MainChatActivity.this)
				.setTitle("New Subscription Request")
				.setMessage(sender + " sent a subscription request. Do you want to start chatting with this User?")
				.setCancelable(false)
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
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


	// Is Called when another Activity returns. Adds chosen User to Roster if he exists
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    
		switch(requestCode) {
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
		}
	}
	

	
	 // Add new ChatPartner to Array and subscribe to Server
	private void addUser(String email, String name)
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
				chats.add(new ChatPartner(email, name));
				((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
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
	
	
	
	// Remove user from Array and unsubscribe 
	private boolean removeUser(ChatPartner user)
	{
		try 
		{
			chatPartners.removeEntry(chatPartners.getEntry(user.getEmail()));
			chats.remove(user);
			((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
		}
		catch (XMPPException e)
		{
			Log.e(DELETE_USER_TAG, "Error when removing User" + user.getName());
			Log.e(DELETE_USER_TAG, e.toString());
		}
		return true;
	}
	
	
	
	// Check if User exists on google Talk server
	private boolean userExists(String userID)
	{
		try 
		{
			UserSearchManager searchManager = new UserSearchManager(this.connection);
	
			String searchService = (String) searchManager.getSearchServices().toArray()[0];
			
			Form searchForm = searchManager.getSearchForm(searchService);
			Form answerForm = searchForm.createAnswerForm();
			
			String usernameWithoutDomain = userID.split("@")[0] + "*";
			answerForm.setAnswer("user", usernameWithoutDomain);
			
			ReportedData resultData = searchManager.getSearchResults(answerForm, searchService);
			if (resultData.getRows() != null)
			{
				
				Iterator<Row> iter = resultData.getRows();
				if (!iter.hasNext())
				{
					Toast.makeText(this, "User "+userID+" does not exist!", Toast.LENGTH_SHORT).show();
					return false;
				}
				else
				{
					Row row = iter.next();
                	Iterator iterator = row.getValues("jid");
                    if(iterator.hasNext())
                    {
                        String value = iterator.next().toString();
                        Log.i("Iterator values......"," "+value);
                    }
                    
                    // If there is more than 1 column, there are more Users that match the search 
                    if (!iter.hasNext())
                    {
        				Toast.makeText(this, "User " + userID + " exists!", Toast.LENGTH_SHORT).show();
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
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_chat, menu);
		return true;
	}


}
