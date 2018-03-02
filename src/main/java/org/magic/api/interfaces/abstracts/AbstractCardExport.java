package org.magic.api.interfaces.abstracts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicDeck;
import org.magic.api.interfaces.MTGCardsExport;
import org.magic.services.MTGConstants;
import org.magic.services.MTGLogger;
import org.utils.patterns.observer.Observable;

public abstract class AbstractCardExport extends Observable implements MTGCardsExport {
	protected Logger logger = MTGLogger.getLogger(this.getClass());

	private boolean enable;
	protected Properties props;

	protected File confdir = new File(MTGConstants.CONF_DIR, "exports");
	
	@Override
	public PLUGINS getType() {
		return PLUGINS.EXPORT;
	}
	
	
	public void load()
	{
		File f =null;
		try {
			f = new File(confdir,getName()+".conf");
			if(f.exists())
			{	
				FileInputStream fis = new FileInputStream(f);
				props.load(fis);
				fis.close();
			}
		} catch (Exception e) {
			logger.error("couln't load properties " + f,e);
		} 
	}
	
	public void save()
	{
		File f = null;
		try {
			f = new File(confdir, getName()+".conf");
		
			FileOutputStream fos = new FileOutputStream(f);
			props.store(fos,"");
			fos.close();
		} catch (Exception e) {
			logger.error("error writing file " + f,e);
		} 
	}
	
	
	public AbstractCardExport() {
		props=new Properties();

		if(!confdir.exists())
			confdir.mkdir();
		load();
	}
	
	@Override
	public Properties getProperties() {
		return props;
	}

	@Override
	public void setProperties(String k, Object value) {
		props.put(k,value);
	}

	@Override
	public String getProperty(String k) {
		return String.valueOf(props.get(k));
	}

	@Override
	public boolean isEnable() {
		return enable;
	}

	@Override
	public void enable(boolean t) {
		this.enable=t;
		
	}
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==null)
			return false;
		
		return this.hashCode()==obj.hashCode();
	}
	
	@Override
	public String toString() {
		return getName();
	}


	@Override
	public void export(List<MagicCard> cards, File f) throws IOException {

		MagicDeck d = new MagicDeck();
				d.setName("export " + getName() + " cards");
				d.setDescription(getName() +" export to " + f.getName());
				d.setDateCreation(new Date());
		int i=0;
		for(MagicCard mc : cards)
		{
			d.add(mc);
			setChanged();
			notifyObservers(i++);
		}
		export(d,f);
	}
	
	
	protected List<MagicCardStock> importFromDeck(MagicDeck deck)
	{
		List<MagicCardStock> mcs = new ArrayList<>();
		
		for(MagicCard mc : deck.getMap().keySet())
		{
			MagicCardStock stock = new MagicCardStock();
				stock.setMagicCard(mc);
				stock.setQte(deck.getMap().get(mc));
				stock.setComment("import from " + deck.getName());
				stock.setIdstock(-1);
				stock.setUpdate(true);
				mcs.add(stock);
		}
		return mcs;
	}
	

}
