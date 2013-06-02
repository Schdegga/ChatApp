package com.example.chatapp;

import java.util.ArrayList;

import android.util.Log;

public class ChatPartner {

	private String email, name;
	private ArrayList<String> messages;
	
	public ChatPartner(String email, String name)
	{
		this.email = email;
		this.name = name;
		this.messages = new ArrayList<String>();
	}
	
	public String getName()
	{
		return name;
	}

	public String getEmail()
	{
		return email;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	public void addMessage(String messageBody)
	{
		messages.add(messageBody);
		Log.i("MESSAGE ADDED", "Message was added to " + email);
	}
	
	public ArrayList<String> getMessages()
	{
		return messages;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatPartner other = (ChatPartner) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
