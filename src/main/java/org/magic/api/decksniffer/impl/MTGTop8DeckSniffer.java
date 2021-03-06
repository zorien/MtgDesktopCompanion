package org.magic.api.decksniffer.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.RetrievableDeck;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.abstracts.AbstractDeckSniffer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.tools.URLTools;
import org.magic.tools.URLToolsClient;

import com.google.common.collect.ImmutableMap.Builder;

public class MTGTop8DeckSniffer extends AbstractDeckSniffer {

	private static final String COMPETITION_FILTER = "COMPETITION_FILTER";
	Map<String, String> formats;

	public MTGTop8DeckSniffer() {
		super();
		initFormats();
	}

	private void initFormats() {
		formats = new HashMap<>();
		formats.put("Standard", "ST");
		formats.put("Modern", "MO");
		formats.put("Legacy", "LE");
		formats.put("Vintage", "VI");
		formats.put("Duel Commander", "EDH");
		formats.put("MTGO Commander", "EDHM");
		formats.put("Block", "BL");
		formats.put("Extended", "EX");
		formats.put("Pauper", "PAU");
		formats.put("Highlander", "HIGH");
		formats.put("Canadian Highlander", "CHL");
		formats.put("Limited", "LI");

	}

	@Override
	public STATUT getStatut() {
		return STATUT.BETA;
	}

	@Override
	public String[] listFilter() {
		return formats.keySet().toArray(new String[formats.keySet().size()]);
	}

	@Override
	public MagicDeck getDeck(RetrievableDeck info) throws IOException {
		Document root = URLTools.extractHtml(info.getUrl().toString());
		MagicDeck d = new MagicDeck();
		d.setDescription(info.getUrl().toString());
		d.setName(info.getName());
	
		Elements doc = root.select("table.Stable").get(1).select("td table").select(MTGConstants.HTML_TAG_TD);

		boolean side = false;
		for (Element e : doc.select("td table td")) {

			if (e.hasClass("O13")) {
				if (e.text().equalsIgnoreCase("SIDEBOARD"))
					side = true;
			} else {

				int qte = Integer.parseInt(e.text().substring(0, e.text().indexOf(' ')));
				String name = e.select("span.L14").text();
				if (!name.equals("")) {
					MagicCard mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class)
							.searchCardByName( name, null, true).get(0);
					if (!side)
						d.getMap().put(mc, qte);
					else
						d.getMapSideBoard().put(mc, qte);
					
					notify(mc);
				}
			}

		}

		return d;
	}

	@Override
	public List<RetrievableDeck> getDeckList() throws IOException {
		URLToolsClient httpClient = URLTools.newClient();
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < getInt("MAX_PAGE"); i++) {
			
			Builder<String,String> nvps = httpClient.buildMap();
			
			nvps.put("current_page", String.valueOf(i + 1));
			nvps.put("event_titre", getString("EVENT_FILTER"));
			nvps.put("deck_titre", "");
			nvps.put("player", "");
			nvps.put("format", formats.get(getString("FORMAT")));
			nvps.put("MD_check", "1");
			nvps.put("cards", getString("CARD_FILTER"));
			nvps.put("date_start", getString("DATE_START_FILTER"));
			nvps.put("date_end", "");

			if (getString(COMPETITION_FILTER) != null) {
				for (String c : getArray(COMPETITION_FILTER))
					nvps.put(" compet_check[" + c.toUpperCase() + "]", "1");
			}

			logger.debug("snif decks : " + getString("URL") + "/search");

			res.append(httpClient.doPost(getString("URL") + "/search", nvps.build(), null));
		}

		Document d = URLTools.toHtml(res.toString());
		Elements els = d.select("tr.hover_tr");

		List<RetrievableDeck> ret = new ArrayList<>();
		for (int i = 0; i < els.size(); i++) {
			Element e = els.get(i);
			RetrievableDeck dk = new RetrievableDeck();
			dk.setName(e.select("td.s11 a").text());
			try {
				dk.setUrl(new URI(getString("URL") + e.select("td.s11 a").attr("href")));
			} catch (URISyntaxException e1) {
				dk.setUrl(null);
			}
			dk.setAuthor(e.select("td.g11 a").text());
			dk.setDescription(e.select("td.S10 a").text());
			ret.add(dk);
		}

		return ret;
	}


	@Override
	public String getName() {
		return "MTGTop8";
	}

	@Override
	public void initDefault() {
		
		setProperty("URL", "http://mtgtop8.com/");
		setProperty("EVENT_FILTER", "");
		setProperty("FORMAT", "Standard");
		setProperty("MAX_PAGE", "2");
		setProperty("TIMEOUT", "0");
		setProperty("CARD_FILTER", "");
		setProperty(COMPETITION_FILTER, "P,M,C,R");
		setProperty("DATE_START_FILTER", "");

	}


}
