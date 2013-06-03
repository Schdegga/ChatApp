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
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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
		
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
		
		setTitle(chatPartnerName); 
		connection = Login.getConnection();
		chatManager = connection.getChatManager();
		chat = chatManager.createChat(chatPartnerEmail, new MessageListener() {
			@Override
			public void processMessage(Chat chat, final Message message) {
				if (message.getType() == Message.Type.chat)
				{
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							adapter.add(message.getBody());
							adapter.notifyDataSetChanged();
						}
					});
					
				}
			}
		});
		sentMessages = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, R.layout.chat_view, R.id.chatViewTextView, sentMessages);
		listView.setAdapter(adapter);
		getOfflineMessages(intentThatStartedThisActivity.getStringArrayListExtra("messagesOutsideChatroom"));

	}
	
	
	
	private void getOfflineMessages(ArrayList<String> stringArrayExtra) {
		Log.i("ChatRoom", "Trying to get offline messages");
		for(String s : stringArrayExtra)
		{
			adapter.add(s);
		}
		//((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
	}



	private void sendMessage()
	{
		String messageBody = messageInput.getText().toString();
		
		if (! messageBody.isEmpty())
		{
			try
			{
				Log.i("SEND_MESSAGE_TAG", "Sending Message to: " + chat.getParticipant());
				Message message = new Message();
				message.setBody(messageBody);
				message.setType(Message.Type.chat);
				chat.sendMessage(message);
				adapter.add(messageBody);
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
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
				Log.e("MESSAGE SEND TAG", "Error when sending message to: " + chat.getParticipant());
				e.printStackTrace();
			}
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_room, menu);
		return true;
	}

}
