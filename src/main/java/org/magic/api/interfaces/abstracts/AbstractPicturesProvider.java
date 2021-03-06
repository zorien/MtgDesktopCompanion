package org.magic.api.interfaces.abstracts;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardNames;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGPictureProvider;
import org.magic.api.interfaces.MTGPicturesCache;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.tools.ImageTools;

public abstract class AbstractPicturesProvider extends AbstractMTGPlugin implements MTGPictureProvider {

	protected int newW=MTGConstants.DEFAULT_PIC_HEIGHT;
	protected int newH=MTGConstants.DEFAULT_PIC_WIDTH;
	
	
	@Override
	public PLUGINS getType() {
		return PLUGINS.PICTURES;
	}


	public AbstractPicturesProvider() {
		super();
		confdir = new File(MTGConstants.CONF_DIR, "pictures");
		if (!confdir.exists())
			confdir.mkdir();
		load();

		if (!new File(confdir, getName() + ".conf").exists()) {
			initDefault();
			save();
		}
		
		try {
			setSize(MTGControler.getInstance().getPictureProviderDimension());
		}
		catch(Exception e)
		{
			logger.error("couldn't set size",e);
		}
	}
	
	public abstract BufferedImage getOnlinePicture(MagicCard mc, MagicEdition ed) throws IOException;
	
	@Override
	public BufferedImage getPicture(MagicCard mc, MagicEdition ed) throws IOException {
		if (MTGControler.getInstance().getEnabled(MTGPicturesCache.class).getPic(mc, ed) != null) {
			logger.trace("cached " + mc + "(" + ed + ") found");
			return resizeCard(MTGControler.getInstance().getEnabled(MTGPicturesCache.class).getPic(mc, ed), newW, newH);
		}

		BufferedImage bufferedImage = getOnlinePicture(mc, ed);
		if (bufferedImage != null)
		{
			MTGControler.getInstance().getEnabled(MTGPicturesCache.class).put(bufferedImage, mc, ed);
			return resizeCard(bufferedImage, newW, newH);
		}
		else
		{
			return getBackPicture();
		}
	}
	
	@Override
	public BufferedImage getForeignNamePicture(MagicCardNames fn, MagicCard mc) throws IOException {
		return getPicture(mc.toForeign(fn),mc.getCurrentSet());
	}

	@Override
	public void setSize(Dimension d) {
		newW=(int)d.getWidth();
		newH=(int)d.getHeight();
	}

	@Override
	public BufferedImage getBackPicture() {
		try {
			return ImageIO.read(MTGConstants.DEFAULT_BACK_CARD);
		} catch (IOException e) {
			logger.error(e);
			return null;
		}
	}

	@Override
	public void initDefault() {
	}

	public BufferedImage resizeCard(BufferedImage img, int newW, int newH) {
		if(img==null)
			return null;
		return ImageTools.resize(img, newH, newW);
	}

}
