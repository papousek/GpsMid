package de.ueller.gpsmid.ui;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

//#if polish.api.wmapi


import javax.microedition.lcdui.*;
import javax.wireless.messaging.*;
import javax.microedition.io.*;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiSendMessage extends Form implements CommandListener {
		
	// commands
	private static final Command CMD_SEND = new Command(Locale.get("guisendmessage.Send")/*Send*/, Command.OK, 1);
	private static final Command CMD_BACK = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 2);
	
    // text fields
	private TextField tfMessage;
	private TextField tfRecipientPhoneNumber;

	// choice group
	private ChoiceGroup cgRemember;
	
	// other
	private Trace parent;

	protected static final Logger logger = Logger.getInstance(GuiSendMessage.class,Logger.TRACE);
		
	public GuiSendMessage(Trace tr) {
		super(Locale.get("guisendmessage.SendSMS")/*Send SMS (map pos)*/);
		this.parent = tr;
		try {
			tfMessage = new TextField(Locale.get("guisendmessage.MessageText")/*Message text*/,
						        Locale.get("guisendmessage.Lat")/*Lat: */ + tr.center.radlat * MoreMath.FAC_RADTODEC +
						        Locale.get("guisendmessage.Lon")/* Lon: */ + tr.center.radlon * MoreMath.FAC_RADTODEC +
							" ",
							160,
							TextField.ANY
			);
			append(tfMessage);

			tfRecipientPhoneNumber = new TextField(Locale.get("guisendmessage.Recipient")/*Recipient*/, 
													Configuration.getSmsRecipient(),
													50,
													TextField.PHONENUMBER
			); 
			append(tfRecipientPhoneNumber);

			String [] rememberOpts = new String[1];
			rememberOpts[0] = Locale.get("guisendmessage.SetDefaultRecipient")/*Set as default recipient*/;
			cgRemember = new ChoiceGroup(Locale.get("guisendmessage.Options")/*Options:*/, Choice.MULTIPLE, rememberOpts, null);
			append(cgRemember);			
			
			addCommand(CMD_SEND);
			addCommand(CMD_BACK);
			
			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == CMD_SEND || c == CMD_BACK
			&& cgRemember.isSelected(0)) {
			Configuration.setSmsRecipient(tfRecipientPhoneNumber.getString());
		}
		
		if (c == CMD_SEND) {				
			String recipientAddr = "sms://" + tfRecipientPhoneNumber.getString();
	        MessageConnection conn = null;
	        try {
	        	conn = (MessageConnection) Connector.open(recipientAddr);
	        	TextMessage msg = (TextMessage) conn.newMessage(MessageConnection.TEXT_MESSAGE);
	        	msg.setPayloadText(tfMessage.getString());
	        	conn.send(msg);
	        	//#debug
	        	logger.info("SMS sent to " + recipientAddr);
	        } catch (Exception e) {
			logger.exception(Locale.get("guisendmessage.FailedToSend")/*Failed to send to */ + recipientAddr, e);
	        }
	        finally {
	        	if( conn != null ){
	        		try {
	        			conn.close();
	        		}
	        		catch( Exception e )
	        		{		       
	        		}
	        	}
	        }
		    parent.show();
			return;
		}
		if (c == CMD_BACK) {			
			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}
}
//#endif
