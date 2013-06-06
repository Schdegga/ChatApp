package com.example.chatapp;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * This Activity represents a ChatRoom where two Users can chat with eachother
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class ChatRoomActivity extends Activity {
	
	private String chatPartnerName;
	private String chatPartnerEmail;
	private XMPPConnection connection;
	private ChatManager chatManager;
	private Chat chat;
	private ArrayList<String> sentMessages; 
	private ArrayAdapter<String> adapter;
	private ListView listView;
	private Button sendButton;
	private EditText messageInput;
	
	private final static String CHAT_ROOM_MESSAGE_LISTENER_TAG = "Chat Room Message Listener";
	private final static String CHAT_ROOM_SEND_MESSAGE_TAG = "Chat Room Send Message";
	
	
	/**
	 * onCreate() Android Lifecycle Method
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_room);
		
		Intent intentThatStartedThisActivity = getIntent();
		chatPartnerEmail = intentThatStartedThisActivity.getStringExtra("chosenChatPartnerEmail");
		chatPartnerName  = intentThatStartedThisActivity.getStringExtra("chosenChatPartnerName");
		listView = (ListView) findViewById(R.id.chatRoomListView);
		sendButton = (Button) findViewById(R.id.chatRoomSendMessageButton);
		messageInput = (EditText) findViewById(R.id.chatRoomMessageText);
		
		// Keep Track of Context
		MainChatActivity.setContext(this);
		
		setTitle(chatPartnerName); 
		connection = Login.getConnection();
		chatManager = connection.getChatManager();
		Log.i("New Chat", "Creating chat with: " + chatPartnerEmail);
		chat = chatManager.createChat(chatPartnerEmail, new MessageListener() {
			@Override
			public void processMessage(Chat chat, final Message message) {
				handleIncomingMessage(chat, message);
			}
		});
		sentMessages = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, R.layout.chat_view, R.id.chatViewTextView, sentMessages);
		listView.setAdapter(adapter);
		getOfflineMessages(intentThatStartedThisActivity.getStringArrayListExtra("messagesOutsideChatroom"));
		
		initialiseListeners();
	}
	
	
	
	/**
	 * Android Method
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_room, menu);
		return true;
	}
	
	
	
	/**
	 * Adds incoming Message to the ListView
	 * 
	 * @param chat is the Chat where the message came in
	 * @param message is the Message Packet that was received
	 */
	protected void handleIncomingMessage(Chat chat, final Message message) 
	{
		Log.i(CHAT_ROOM_MESSAGE_LISTENER_TAG, "Message received from: " + message.getFrom() + " with Type: " + message.getType());
		if (message.getType() == Message.Type.chat)
		{
			Log.i(CHAT_ROOM_MESSAGE_LISTENER_TAG, "Trying to add Message to adapter");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i(CHAT_ROOM_MESSAGE_LISTENER_TAG, "Is message.getBody() == null" + Boolean.toString(message.getBody() == null));
					adapter.add(message.getBody());
					adapter.notifyDataSetChanged();
				}
			});
		}
	}


	
	/**
	 * Sets all Listeners
	 */
	private void initialiseListeners() 
	{
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
		
		//listen to softkeyboard done-button
		messageInput.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		            sendMessage();
		        }
		        return false;
		    }
		});
	}
	
	
	
	/**
	 * Retrieves Messages that came in, when we weren´t in the same ChatRoom with the User
	 * 
	 * @param stringArrayExtra are the stored Messages
	 */
	private void getOfflineMessages(ArrayList<String> stringArrayExtra) {
		Log.i("ChatRoom", "Trying to get offline messages");
		for(final String s : stringArrayExtra)
		{
			Log.i("ChatRoom", "Actual offline messages present");
			runOnUiThread(new Runnable()
			{
				@Override
				public void run() {
					adapter.add(s);
					adapter.notifyDataSetChanged();
				}
			});
		}
	}



	/**
	 * Sends content of the EditText messageInput to ChatPartner
	 */
	private void sendMessage()
	{
		final String messageBody = messageInput.getText().toString();
		
		if (! messageBody.isEmpty())
		{
			try
			{
				Log.i("SEND_MESSAGE_TAG", "Sending Message to: " + chat.getParticipant());
				Message message = new Message();
				message.setBody(messageBody);
				message.setType(Message.Type.chat);
				chat.sendMessage(message);
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						adapter.add(messageBody);
						adapter.notifyDataSetChanged();
					}
				});
				messageInput.setText("");
				InputMethodManager inputManager = 
				        (InputMethodManager) this.
				            getSystemService(Context.INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(
				        this.getCurrentFocus().getWindowToken(),
				        InputMethodManager.HIDE_NOT_ALWAYS);
			}
			catch (XMPPException e)
			{
				Log.e(CHAT_ROOM_SEND_MESSAGE_TAG, "Error when sending message to: " + chat.getParticipant());
				e.printStackTrace();
			}
			catch (IllegalStateException e)
			{
				Log.e(CHAT_ROOM_SEND_MESSAGE_TAG, "IllegalStateException");
				Log.e(CHAT_ROOM_SEND_MESSAGE_TAG, e.toString());
			}
			catch (Exception e)
			{
				Log.e(CHAT_ROOM_SEND_MESSAGE_TAG, e.toString());
			}
		}
	}
}
