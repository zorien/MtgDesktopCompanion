package org.magic.api.notifiers.impl;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.FORMAT_NOTIFICATION;
import org.magic.api.interfaces.MTGCardsProvider.STATUT;
import org.magic.api.interfaces.abstracts.AbstractMTGNotifier;
import org.magic.services.MTGConstants;

public class OSTrayNotifier extends AbstractMTGNotifier {

	private TrayIcon trayNotifier;
	private SystemTray tray;

	public SystemTray getTray() {
		return tray;
	}
	
	public TrayIcon getTrayNotifier() {
		return trayNotifier;
	}
	
	@Override
	public FORMAT_NOTIFICATION getFormat() {
		return FORMAT_NOTIFICATION.TEXT;
	}
	
	public OSTrayNotifier() {
		try {
			trayNotifier = new TrayIcon(MTGConstants.IMAGE_LOGO.getScaledInstance(16, 16, BufferedImage.SCALE_SMOOTH));
			tray = SystemTray.getSystemTray();
			
			if (SystemTray.isSupported()) {
				tray.add(trayNotifier);
			}
			
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	@Override
	public void send(MTGNotification notification) throws IOException {
		trayNotifier.displayMessage(notification.getTitle(), notification.getMessage(), notification.getType());

	}
	
	@Override
	public boolean isEnable() {
		return SystemTray.isSupported();
	}

	@Override
	public String getName() {
		return "Tray";
	}

	@Override
	public STATUT getStatut() {
		return STATUT.STABLE;
	}

	@Override
	public void initDefault() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}