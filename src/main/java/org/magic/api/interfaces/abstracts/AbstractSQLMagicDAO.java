package org.magic.api.interfaces.abstracts;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.magic.api.beans.EnumCondition;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCardAlert;
import org.magic.api.beans.MagicCardStock;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.MagicNews;
import org.magic.api.beans.OrderEntry;
import org.magic.api.beans.OrderEntry.TYPE_ITEM;
import org.magic.api.beans.OrderEntry.TYPE_TRANSACTION;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGNewsProvider;
import org.magic.services.MTGControler;
import org.magic.tools.Chrono;
import org.magic.tools.IDGenerator;

public abstract class AbstractSQLMagicDAO extends AbstractMagicDAO {
	
	protected static final String LOGIN = "LOGIN";
	protected static final String PASS = "PASS";
	protected static final String DB_NAME = "DB_NAME";
	protected static final String PARAMS = "PARAMS";
	protected static final String SERVERPORT = "SERVERPORT";
	protected static final String SERVERNAME = "SERVERNAME";
	
	protected Connection con;
	public abstract String getAutoIncrementKeyWord();
	public abstract String getjdbcnamedb();
	public abstract String cardStorage();
	public abstract void storeCard(PreparedStatement pst, int position,MagicCard mc) throws SQLException;
	public abstract MagicCard readCard(ResultSet rs) throws SQLException;
	public abstract void createIndex(Statement stat) throws SQLException;
	public abstract String createListStockSQL(MagicCard mc);
	
	
	@Override
	public void initDefault() {
		setProperty(SERVERNAME, "localhost");
		setProperty(SERVERPORT, "1234");
		setProperty(DB_NAME, "mtgdesktopclient");
		setProperty(LOGIN, "login");
		setProperty(PASS, "pass");
		setProperty(PARAMS, "?autoDeserialize=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true");
	}
	
	public String getDBLocation() {
		return getString(SERVERNAME) + "/" + getString(DB_NAME);
	}
	
	@Override
	public String getVersion() {
		try {
			Driver d = DriverManager.getDriver(getjdbcUrl());
			return d.getMajorVersion()+"."+d.getMinorVersion();
		} catch (SQLException e) {
			return "1.0";
		}
	}

	
	protected String getjdbcUrl()
	{
		String url = "jdbc:"+getjdbcnamedb()+"://" + getString(SERVERNAME);
		if(!getString(SERVERPORT).isEmpty())
			url+=":" + getString(SERVERPORT);
		
		url+="/" + getString(DB_NAME) + getString(PARAMS);
		
		return url;
	}
	 
	public void init() throws SQLException, ClassNotFoundException {
		logger.info("init " + getName());
		
		logger.trace("Connexion to " + getjdbcUrl() + "/" + getString(DB_NAME) + getString(PARAMS));
		con = DriverManager.getConnection(getjdbcUrl(),getString(LOGIN), getString(PASS));
		createDB();
	}

	public boolean createDB() {
		try (Statement stat = con.createStatement()) {
			logger.debug("Create table Orders");
			
			stat.executeUpdate("CREATE TABLE orders (id "+getAutoIncrementKeyWord()+" PRIMARY KEY, idTransaction VARCHAR(250), description VARCHAR(250),edition VARCHAR(10),itemPrice DECIMAL(10,3),shippingPrice  DECIMAL(10,3), currency VARCHAR(4), transactionDate DATE,typeItem VARCHAR(50),typeTransaction VARCHAR(50),sources VARCHAR(250),seller VARCHAR(250))");
			logger.debug("Create table Cards");
			stat.executeUpdate("create table cards (ID varchar(250),mcard "+cardStorage()+", edition varchar(20), cardprovider varchar(50),collection varchar(250))");
			logger.debug("Create table collections");
			stat.executeUpdate("CREATE TABLE collections ( name VARCHAR(250) PRIMARY KEY)");
			logger.debug("Create table stocks");
			stat.executeUpdate("create table stocks (idstock "+getAutoIncrementKeyWord()+" PRIMARY KEY , idmc varchar(250), mcard "+cardStorage()+", collection varchar(250),comments varchar(250), conditions varchar(50),foil boolean, signedcard boolean, langage varchar(50), qte integer,altered boolean,price DECIMAL(10,3))");
			logger.debug("Create table Alerts");
			stat.executeUpdate("create table alerts (id varchar(250) PRIMARY KEY, mcard "+cardStorage()+", amount DECIMAL)");
			logger.debug("Create table News");
			stat.executeUpdate("CREATE TABLE news (id "+getAutoIncrementKeyWord()+" PRIMARY KEY, name VARCHAR(100), url VARCHAR(256), categorie VARCHAR(100),typeNews varchar(150))");

			logger.debug("populate collections");
			
			saveCollection(new MagicCollection("Library"));
			saveCollection(new MagicCollection("Needed"));
			saveCollection(new MagicCollection("For sell"));
			saveCollection(new MagicCollection("Favorites"));
			
			createIndex(stat);
			
			return true;
		} catch (SQLException e) {
			logger.error(e);
			return false;
		}

	}

	@Override
	public void saveCard(MagicCard mc, MagicCollection collection) throws SQLException {
		logger.debug("saving " + mc + " in " + collection);

		try (PreparedStatement pst = con.prepareStatement("insert into cards values (?,?,?,?,?)")) {
			pst.setString(1, IDGenerator.generate(mc));
			storeCard(pst, 2, mc);
			pst.setString(3, mc.getCurrentSet().getId());
			pst.setString(4, MTGControler.getInstance().getEnabled(MTGCardsProvider.class).toString());
			pst.setString(5, collection.getName());
			pst.executeUpdate();
		}
	}

	@Override
	public void removeCard(MagicCard mc, MagicCollection collection) throws SQLException {
		logger.debug("delete " + mc + " in " + collection);
		try (PreparedStatement pst = con.prepareStatement("delete from cards where id=? and edition=? and collection=?")) {
			pst.setString(1, IDGenerator.generate(mc));
			pst.setString(2, mc.getCurrentSet().getId());
			pst.setString(3, collection.getName());
			pst.executeUpdate();
		}
	}
	
	@Override
	public void moveCard(MagicCard mc, MagicCollection from, MagicCollection to) throws SQLException {
		logger.debug("move " + mc+ " s=" + from + "d=" + to);
		
		try (PreparedStatement pst = con.prepareStatement("update cards set collection= ? where id=? and collection=?")) 
		{
			pst.setString(1, to.getName());
			pst.setString(2, IDGenerator.generate(mc));
			pst.setString(3, from.getName());
			pst.executeUpdate();
		}
		
		listStocks(mc, from,true).forEach(cs->{
			
			try {
				cs.setMagicCollection(to);
				saveOrUpdateStock(cs);
			} catch (SQLException e) {
				logger.error("Error saving stock for" + mc + " from " + from + " to " + to);
			}
		});
		
	}
	

	@Override
	public List<MagicCard> listCards() throws SQLException {
		logger.debug("list all cards");

		String sql = "select mcard from cards";

		try (PreparedStatement pst = con.prepareStatement(sql); ResultSet rs = pst.executeQuery();) {
			List<MagicCard> listCards = new ArrayList<>();
			while (rs.next()) {
				listCards.add(readCard(rs));
			}
			return listCards;
		}
	}

	@Override
	public Map<String, Integer> getCardsCountGlobal(MagicCollection c) throws SQLException {
		String sql = "select edition, count(ID) from cards where collection=? group by edition";
		try (PreparedStatement pst = con.prepareStatement(sql);) {
			pst.setString(1, c.getName());
			try (ResultSet rs = pst.executeQuery()) {
				Map<String, Integer> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				while (rs.next())
					map.put(rs.getString(1), rs.getInt(2));
				return map;
			}
		}
	}

	@Override
	public int getCardsCount(MagicCollection cols, MagicEdition me) throws SQLException {

		String sql = "select count(ID) from cards ";

		if (cols != null)
			sql += " where collection = '" + cols.getName() + "'";

		if (me != null)
			sql += " and LOWER('edition') = '" + me.getId().toLowerCase() + "'";

		logger.trace(sql);

		try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql);) {
			rs.next();
			return rs.getInt(1);
		}
	}

	@Override
	public List<MagicCard> listCardsFromCollection(MagicCollection collection) throws SQLException {
		return listCardsFromCollection(collection, null);
	}

	@Override
	public List<MagicCard> listCardsFromCollection(MagicCollection collection, MagicEdition me) throws SQLException {
		String sql = "select mcard from cards where collection= ?";

		if (me != null)
			sql = "select mcard from cards where collection= ? and edition = ?";

		logger.trace(sql +" " + collection +" " + me);

		try (PreparedStatement pst = con.prepareStatement(sql)) {
			pst.setString(1, collection.getName());
			if (me != null)
				pst.setString(2, me.getId());
			logger.trace(sql +" begin query");
			try (ResultSet rs = pst.executeQuery()) {
				logger.trace(sql +" resultSet done");
				List<MagicCard> ret = new ArrayList<>();
				while (rs.next()) {
					MagicCard mc = readCard(rs);
					ret.add(mc);
				}
				
				return ret;
			}
		}
	}

	@Override
	public List<String> listEditionsIDFromCollection(MagicCollection collection) throws SQLException {
		String sql = "select distinct(edition) from cards where collection=?";
		Chrono c = new Chrono();
		c.start();
		logger.trace(sql + " begin query " + collection);
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			pst.setString(1, collection.getName());
			try (ResultSet rs = pst.executeQuery()) {
				List<String> retour = new ArrayList<>();
				while (rs.next()) {
					retour.add(rs.getString("edition"));
				}
				logger.trace(sql +" query done in " + c.stop() + " sec");
				return retour;
			}
		}
	}

	@Override
	public MagicCollection getCollection(String name) throws SQLException {

		try (PreparedStatement pst = con.prepareStatement("select * from collections where name= ?")) {
			pst.setString(1, name);
			try (ResultSet rs = pst.executeQuery()) {
				if (rs.next()) {
					return new MagicCollection(rs.getString("name"));
				}
				return null;
			}
		}
	}

	@Override
	public void saveCollection(MagicCollection c) throws SQLException {
		try (PreparedStatement pst = con.prepareStatement("insert into collections values (?)")) {
			pst.setString(1, c.getName().replaceAll("'", "\'"));
			pst.executeUpdate();
		}
	}

	@Override
	public void removeCollection(MagicCollection c) throws SQLException {

		if (c.getName().equals(MTGControler.getInstance().get("default-library")))
			throw new SQLException(c.getName() + " can not be deleted");

		try (PreparedStatement pst = con.prepareStatement("delete from collections where name = ?")) {
			pst.setString(1, c.getName());
			pst.executeUpdate();

		}

		try (PreparedStatement pst2 = con.prepareStatement("delete from cards where collection = ?")) {
			pst2.setString(1, c.getName());
			pst2.executeUpdate();
		}
	}

	@Override
	public List<MagicCollection> listCollections() throws SQLException {
		try (PreparedStatement pst = con.prepareStatement("select * from collections")) {
			try (ResultSet rs = pst.executeQuery()) {
				List<MagicCollection> colls = new ArrayList<>();
				while (rs.next()) {
					MagicCollection mc = new MagicCollection(rs.getString(1));
					colls.add(mc);
				}
				return colls;
			}
		}
	}

	@Override
	public void removeEdition(MagicEdition me, MagicCollection col) throws SQLException {
		logger.debug("remove " + me + " from " + col);
		try (PreparedStatement pst = con.prepareStatement("delete from cards where edition=? and collection=?")) {
			pst.setString(1, me.getId());
			pst.setString(2, col.getName());
			pst.executeUpdate();
		}
	}

	@Override
	public List<MagicCollection> listCollectionFromCards(MagicCard mc) throws SQLException {

		if (mc.getEditions().isEmpty())
			throw new SQLException("No edition defined");
		
		try (PreparedStatement pst = con.prepareStatement("SELECT collection FROM cards WHERE id=? and edition=?")) {
			String id = IDGenerator.generate(mc);
			
			logger.trace("SELECT collection FROM cards WHERE id="+id+" and edition='"+mc.getCurrentSet().getId()+"'");
			
			
			pst.setString(1, id);
			pst.setString(2, mc.getCurrentSet().getId());
			try (ResultSet rs = pst.executeQuery()) {
				List<MagicCollection> cols = new ArrayList<>();
				while (rs.next()) {
					MagicCollection col = new MagicCollection();
					col.setName(rs.getString("collection"));
					cols.add(col);
				}
				return cols;
			}
		}
	}

	@Override
	public void deleteStock(List<MagicCardStock> state) throws SQLException {
		logger.debug("remove " + state.size() + " items in stock");
		StringBuilder st = new StringBuilder();
		st.append("delete from stocks where idstock IN (");
		for (MagicCardStock sto : state) {
			st.append(sto.getIdstock()).append(",");
		}
		st.append(")");
		String sql = st.toString().replace(",)", ")");
		try (Statement pst = con.createStatement()) {
			pst.executeUpdate(sql);
		}
	}

	@Override
	public List<MagicCardStock> listStocks(MagicCard mc, MagicCollection col,boolean editionStrict) throws SQLException {
		
		String sql = createListStockSQL(mc);
		
		if(editionStrict)
			sql ="select * from stocks where collection=? and idmc=?";
		
		logger.trace("sql="+sql);
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			pst.setString(1, col.getName());
			
			if(editionStrict)
				pst.setString(2, IDGenerator.generate(mc));
			else
				pst.setString(2, mc.getName());
			
		
			try (ResultSet rs = pst.executeQuery()) {
				List<MagicCardStock> colls = new ArrayList<>();
				while (rs.next()) {
					MagicCardStock state = new MagicCardStock();

					state.setComment(rs.getString("comments"));
					state.setIdstock(rs.getInt("idstock"));
					state.setMagicCard(mc);
					state.setMagicCollection(col);
					state.setCondition(EnumCondition.valueOf(rs.getString("conditions")));
					state.setFoil(rs.getBoolean("foil"));
					state.setSigned(rs.getBoolean("signedcard"));
					state.setLanguage(rs.getString("langage"));
					state.setAltered(rs.getBoolean("altered"));
					state.setQte(rs.getInt("qte"));
					state.setPrice(rs.getDouble("price"));
					colls.add(state);
				}
				logger.trace("load " + colls.size() + " item from stock for " + mc);
				return colls;
			}

		}

	}

	public List<MagicCardStock> listStocks() throws SQLException {
		try (PreparedStatement pst = con.prepareStatement("select * from stocks"); ResultSet rs = pst.executeQuery();) {
			List<MagicCardStock> colls = new ArrayList<>();
			while (rs.next()) {
				MagicCardStock state = new MagicCardStock();

				state.setComment(rs.getString("comments"));
				state.setIdstock(rs.getInt("idstock"));
				state.setMagicCard(readCard(rs));
				state.setMagicCollection(new MagicCollection(rs.getString("collection")));
				try {
					state.setCondition(EnumCondition.valueOf(rs.getString("conditions")));
				} catch (Exception e) {
					state.setCondition(null);
				}
				state.setFoil(rs.getBoolean("foil"));
				state.setSigned(rs.getBoolean("signedcard"));
				state.setLanguage(rs.getString("langage"));
				state.setQte(rs.getInt("qte"));
				state.setAltered(rs.getBoolean("altered"));
				state.setPrice(rs.getDouble("price"));
				colls.add(state);
			}
			logger.debug("load " + colls.size() + " item(s) from stock");
			return colls;
		}
	}

	private int getGeneratedKey(PreparedStatement pst) {

		try (ResultSet rs = pst.getGeneratedKeys()) {
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			logger.error("couldn't retrieve id", e);
		}
		return -1;

	}

	@Override
	public void saveOrUpdateStock(MagicCardStock state) throws SQLException {

		if (state.getIdstock() < 0) {

			logger.debug("save " + state);
			try (PreparedStatement pst = con.prepareStatement(
					"insert into stocks  ( conditions,foil,signedcard,langage,qte,comments,idmc,collection,mcard,altered,price) values (?,?,?,?,?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS)) {
				pst.setString(1, String.valueOf(state.getCondition()));
				pst.setBoolean(2, state.isFoil());
				pst.setBoolean(3, state.isSigned());
				pst.setString(4, state.getLanguage());
				pst.setInt(5, state.getQte());
				pst.setString(6, state.getComment());
				pst.setString(7, IDGenerator.generate(state.getMagicCard()));
				pst.setString(8, String.valueOf(state.getMagicCollection()));
				storeCard(pst, 9, state.getMagicCard());
				pst.setBoolean(10, state.isAltered());
				pst.setDouble(11, state.getPrice());
				pst.executeUpdate();
				state.setIdstock(getGeneratedKey(pst));
			} catch (Exception e) {
				logger.error(e);
			}
		} else {
			logger.debug("update Stock " + state);
			try (PreparedStatement pst = con.prepareStatement(
					"update stocks set comments=?, conditions=?, foil=?,signedcard=?,langage=?, qte=? ,altered=?,price=?,idmc=?,collection=? where idstock=?")) {
				pst.setString(1, state.getComment());
				pst.setString(2, state.getCondition().toString());
				pst.setBoolean(3, state.isFoil());
				pst.setBoolean(4, state.isSigned());
				pst.setString(5, state.getLanguage());
				pst.setInt(6, state.getQte());
				pst.setBoolean(7, state.isAltered());
				pst.setDouble(8, state.getPrice());
				pst.setString(9, IDGenerator.generate(state.getMagicCard()));
				pst.setString(10, state.getMagicCollection().getName());
				pst.setInt(11, state.getIdstock());
				pst.executeUpdate();
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}


	@Override
	public void initAlerts() {

		try (PreparedStatement pst = con.prepareStatement("select * from alerts")) {
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					MagicCardAlert alert = new MagicCardAlert();
					alert.setCard(readCard(rs));
					alert.setId(rs.getString("id"));
					alert.setPrice(rs.getDouble("amount"));

					listAlerts.add(alert);
				}
			}
		} catch (Exception e) {
			logger.error("error get alert",e);
		}
	}


	@Override
	public void saveAlert(MagicCardAlert alert) throws SQLException {

		try (PreparedStatement pst = con.prepareStatement("insert into alerts  ( id,mcard,amount) values (?,?,?)")) {
			
			alert.setId(IDGenerator.generate(alert.getCard()));
			
			pst.setString(1, alert.getId());
			storeCard(pst, 2, alert.getCard());
			pst.setDouble(3, alert.getPrice());
			pst.executeUpdate();
			logger.debug("save alert for " + alert.getCard()+ " ("+alert.getCard().getCurrentSet()+")");
			listAlerts.add(alert);
		}
	}

	@Override
	public void updateAlert(MagicCardAlert alert) throws SQLException {
		logger.debug("update " + alert);
		try (PreparedStatement pst = con.prepareStatement("update alerts set amount=?,mcard=? where id=?")) {
			pst.setDouble(1, alert.getPrice());
			storeCard(pst, 2, alert.getCard());
			pst.setString(3, alert.getId());
			pst.executeUpdate();
		}

	}

	@Override
	public void deleteAlert(MagicCardAlert alert) throws SQLException 
	{
		try (PreparedStatement pst = con.prepareStatement("delete from alerts where id=?")) {
			logger.debug("delete from alerts where id="+alert.getId());
			pst.setString(1, alert.getId());
			int res = pst.executeUpdate();
			logger.debug("delete alert " + alert + " ("+alert.getCard().getCurrentSet()+")="+res);
		}

		if (listAlerts != null)
		{
			boolean res = listAlerts.remove(alert);
			logger.debug("delete alert from list " + res);
		}

	}

	@Override
	public List<MagicNews> listNews() {
		try (PreparedStatement pst = con.prepareStatement("select * from news")) {
			List<MagicNews> news = new ArrayList<>();
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					MagicNews n = new MagicNews();
					n.setCategorie(rs.getString("categorie"));
					n.setName(rs.getString("name"));
					n.setUrl(rs.getString("url"));
					n.setId(rs.getInt("id"));
					n.setProvider(MTGControler.getInstance().getPlugin(rs.getString("typeNews"),MTGNewsProvider.class));
					news.add(n);
				}
				return news;
			}
		} catch (Exception e) {
			logger.error(e);
			return new ArrayList<>();
		}
	}

	@Override
	public void deleteNews(MagicNews n) throws SQLException {
		logger.debug("delete news " + n);
		try (PreparedStatement pst = con.prepareStatement("delete from news where id=?")) {
			pst.setInt(1, n.getId());
			pst.executeUpdate();
		}
	}

	@Override
	public void saveOrUpdateNews(MagicNews n) throws SQLException {
		if (n.getId() < 0) {

			logger.debug("save " + n);
			try (PreparedStatement pst = con.prepareStatement(
					"insert into news  ( name,categorie,url,typeNews) values (?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS)) {
				pst.setString(1, n.getName());
				pst.setString(2, n.getCategorie());
				pst.setString(3, n.getUrl());
				pst.setString(4, n.getProvider().getName());
				pst.executeUpdate();
				n.setId(getGeneratedKey(pst));
			}

		} else {
			logger.debug("update " + n);
			try (PreparedStatement pst = con
					.prepareStatement("update news set name=?, categorie=?, url=?,typeNews=? where id=?")) {
				pst.setString(1, n.getName());
				pst.setString(2, n.getCategorie());
				pst.setString(3, n.getUrl());
				pst.setString(4, n.getProvider().getName());
				pst.setInt(5, n.getId());
				pst.executeUpdate();
			} catch (Exception e) {
				logger.error(e);
			}
		}

	}

	
	@Override
	public void initOrders() {
		try (PreparedStatement pst = con.prepareStatement("select * from orders"); ResultSet rs = pst.executeQuery();) {
			while (rs.next()) {
				OrderEntry state = new OrderEntry();
				
				state.setId(rs.getInt("id"));
				state.setIdTransation(rs.getString("idTransaction"));
				state.setDescription(rs.getString("description"));
				try {
					state.setEdition(MTGControler.getInstance().getEnabled(MTGCardsProvider.class).getSetById(rs.getString("edition")));
				} catch (Exception e) {
					state.setEdition(null);
				}
				state.setCurrency(Currency.getInstance(rs.getString("currency")));
				state.setTransationDate(rs.getDate("transactionDate"));
				state.setItemPrice(rs.getDouble("itemPrice"));
				state.setShippingPrice(rs.getDouble("shippingPrice"));
				state.setType(TYPE_ITEM.valueOf(rs.getString("typeItem")));
				state.setTypeTransaction(TYPE_TRANSACTION.valueOf(rs.getString("typeTransaction")));
				state.setSource(rs.getString("sources"));
				state.setSeller(rs.getString("seller"));
				state.setUpdated(false);
				listOrders.add(state);
			}
			logger.debug("load " + listOrders.size() + " item(s) from orders");
		}
		catch(Exception e)
		{
			logger.error(e);
		}
	}


	@Override
	public void deleteOrderEntry(List<OrderEntry> state) throws SQLException {
		logger.debug("remove " + state.size() + " items in orders");
		StringBuilder st = new StringBuilder();
		st.append("delete from orders where id IN (");
		for (OrderEntry sto : state) {
			st.append(sto.getId()).append(",");
		}
		st.append(")");
		String sql = st.toString().replace(",)", ")");
		try (Statement pst = con.createStatement()) {
			pst.executeUpdate(sql);
		}
		

		if (listOrders != null)
		{
			boolean res = listOrders.removeAll(state);
			logger.debug("delete orders from list " + res);
		}
		
	}
	
	@Override
	public void saveOrUpdateOrderEntry(OrderEntry state) throws SQLException {

		if (state.getId() < 0) {
			logger.debug("save " + state);
			try (PreparedStatement pst = con.prepareStatement(
					"INSERT INTO orders (idTransaction, description, edition, itemPrice, shippingPrice, currency, transactionDate, typeItem, typeTransaction, sources, seller)"
				  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS)) {
				
				pst.setString(1, state.getIdTransation());
				pst.setString(2, state.getDescription());
				
				if(state.getEdition()!=null)
					pst.setString(3, state.getEdition().getId());
				else
					pst.setString(3, null);
				
				pst.setDouble(4, state.getItemPrice());
				pst.setDouble(5,state.getShippingPrice());
				pst.setString(6,state.getCurrency().getCurrencyCode());
				pst.setDate(7, new Date(state.getTransationDate().getTime()));
				pst.setString(8,state.getType().name());
				pst.setString(9,state.getTypeTransaction().name());
				pst.setString(10, state.getSource());
				pst.setString(11, state.getSeller());
				pst.executeUpdate();
				state.setId(getGeneratedKey(pst));
				listOrders.add(state);
			} catch (Exception e) {
				logger.error("error insert " + state.getDescription() , e);
			}
		} else {
			logger.debug("update Order " + state);
			try (PreparedStatement pst = con.prepareStatement(
					"UPDATE orders SET "
					+ "idTransaction= ?, description=?, edition=?,itemPrice=?,shippingPrice=?,currency=?,transactionDate=?,typeItem=?,typeTransaction=?,sources=?,seller=? "
					+ "WHERE id = ?")) {
				
				pst.setString(1, state.getIdTransation());
				pst.setString(2, state.getDescription());
				
				if(state.getEdition()!=null)
					pst.setString(3, state.getEdition().getId());
				else
					pst.setString(3, null);
				
				pst.setDouble(4, state.getItemPrice());
				pst.setDouble(5,state.getShippingPrice());
				pst.setString(6,state.getCurrency().getCurrencyCode());
				pst.setDate(7, new Date(state.getTransationDate().getTime()));
				pst.setString(8,state.getType().name());
				pst.setString(9,state.getTypeTransaction().name());
				pst.setString(10, state.getSource());
				pst.setString(11, state.getSeller());
				pst.setInt(12, state.getId());
				pst.executeUpdate();
			}
		}
	}




}