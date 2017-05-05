package org.magic.game.model.counters;

import org.magic.game.gui.components.DisplayableCard;

public class BonusCounter extends AbstractCounter {

	int powerModifier;
	int toughnessModifier;
	
	public BonusCounter(int powerModifier,int toughnessModifier)
	{
		this.powerModifier=powerModifier;
		this.toughnessModifier=toughnessModifier;
	}

	@Override
	public void apply(DisplayableCard displayableCard) {
		int power=0;
		int toughness=0;
		
		try{
			power = Integer.parseInt(displayableCard.getMagicCard().getPower());
		}
		catch(Exception e)
		{	}
		
		
		try{
			toughness = Integer.parseInt(displayableCard.getMagicCard().getToughness());
		}
		catch(Exception e)
		{	}
		
		power = power + powerModifier;
		toughness = toughness + toughnessModifier;
		
		displayableCard.getMagicCard().setPower(String.valueOf(power));
		displayableCard.getMagicCard().setToughness(String.valueOf(toughness));
		
	}

	@Override
	public void remove(DisplayableCard displayableCard) {
		int power=0;
		int toughness=0;
		
		try{
			power = Integer.parseInt(displayableCard.getMagicCard().getPower());
		}
		catch(Exception e)
		{	}
		
		
		try{
			toughness = Integer.parseInt(displayableCard.getMagicCard().getToughness());
		}
		catch(Exception e)
		{	}
		
		power = power - powerModifier;
		toughness = toughness - toughnessModifier;
		
		displayableCard.getMagicCard().setPower(String.valueOf(power));
		displayableCard.getMagicCard().setToughness(String.valueOf(toughness));
		
	}

	@Override
	public String describe() {
		if(powerModifier>0 && toughnessModifier==0)
			return "+"+powerModifier+"/+0 counter";
		else if(powerModifier<0 && toughnessModifier==0)
			return powerModifier+"/+0 counter";
		else if(powerModifier==0 && toughnessModifier<0)
			return "+0/"+toughnessModifier+" counter";
		else if(powerModifier==0 && toughnessModifier>0)
			return "+0/+"+toughnessModifier+" counter";
		else
			return powerModifier+"/"+toughnessModifier+ " counter";
		
	}
	
	
	
}
