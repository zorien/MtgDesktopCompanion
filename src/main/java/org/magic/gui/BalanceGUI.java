package org.magic.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Currency;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SortOrder;

import org.jdesktop.swingx.JXTable;
import org.magic.api.beans.CardPriceVariations;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicEdition;
import org.magic.api.beans.OrderEntry;
import org.magic.api.beans.OrderEntry.TYPE_TRANSACTION;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.api.interfaces.MTGDashBoard;
import org.magic.gui.abstracts.MTGUIComponent;
import org.magic.gui.components.OrderEntryPanel;
import org.magic.gui.components.charts.HistoryPricesPanel;
import org.magic.gui.components.dialog.OrderImporterDialog;
import org.magic.gui.models.ShoppingEntryTableModel;
import org.magic.gui.renderer.MagicEditionJLabelRenderer;
import org.magic.gui.renderer.OrderEntryRenderer;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;
import org.magic.tools.UITools;
import java.awt.Font;
import javax.swing.SwingConstants;
import java.awt.Component;
import org.magic.gui.components.charts.EditionFinancialChartPanel;
import java.awt.GridLayout;

public class BalanceGUI extends MTGUIComponent {
	
	private static final long serialVersionUID = 1L;
	private ShoppingEntryTableModel model;
	private JLabel totalBuy;
	private JLabel totalSell;
	private JLabel total;
	private JLabel selectionBuy;
	private JLabel selectionSell;
	private JLabel totalSelection;
	private JXTable table;
	private OrderEntryPanel orderEntryPanel;
	private HistoryPricesPanel pricesPanel;
	private JLabel lblComparator;
	private JPanel editorPanel;
	private JPanel panelComparator;
	private JPanel chartesContainerPanel;
	private EditionFinancialChartPanel editionFinancialChartPanel;

	
	
	private void loadFinancialBook()
	{
		 	MTGControler.getInstance().getFinancialService().loadFinancialBook();
			List<OrderEntry> l =MTGControler.getInstance().getFinancialService().getEntries();
			model.addItems(l);
			calulate(l);
			table.packAll();
	}
	
	
	public BalanceGUI() {
		
		JPanel panneauBas = new JPanel();
		JPanel panneauHaut = new JPanel();
		JPanel panneauRight = new JPanel();
		table = new JXTable();
		model = new ShoppingEntryTableModel();
		JButton btnImportTransaction = new JButton(MTGConstants.ICON_IMPORT);
		JButton btnSave = new JButton(MTGConstants.ICON_SAVE);
		totalBuy = new JLabel(MTGConstants.ICON_DOWN);
		totalSell = new JLabel(MTGConstants.ICON_UP);
		total = new JLabel();
		totalSelection = new JLabel();
		selectionSell = new JLabel(MTGConstants.ICON_UP);
		selectionBuy=new JLabel(MTGConstants.ICON_DOWN);
		OrderEntryRenderer render = new OrderEntryRenderer();
		editorPanel = new JPanel();
		orderEntryPanel = new OrderEntryPanel();
		JButton btnSaveOrder = new JButton(MTGConstants.ICON_SAVE);
		JPanel panelButton = new JPanel();
		JButton btnDeleteOrder = new JButton(MTGConstants.ICON_DELETE);
		JButton btnNewEntry = new JButton(MTGConstants.ICON_NEW);
		panelComparator = new JPanel();
		lblComparator = new JLabel("Values");
		editionFinancialChartPanel = new EditionFinancialChartPanel();
		
		btnSave.setEnabled(false);
		UITools.initTableFilter(table);
		setLayout(new BorderLayout(0, 0));
		table.setModel(model);
	
		table.setDefaultRenderer(MagicEdition.class, new MagicEditionJLabelRenderer());
		table.setDefaultRenderer(Double.class, render);
		panneauRight.setPreferredSize(new Dimension(500, 1));
		editorPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		editorPanel.setLayout(new BorderLayout(0, 0));
		
		btnDeleteOrder.setEnabled(false);
		panelComparator.setLayout(new BorderLayout(0, 0));
		lblComparator.setHorizontalAlignment(SwingConstants.CENTER);
		lblComparator.setFont(MTGConstants.FONT.deriveFont(Font.BOLD, 16));
	
		
		
		panneauBas.add(totalBuy);
		panneauBas.add(totalSell);
		panneauBas.add(total);
		panneauBas.add(new JLabel(" ("));
		panneauBas.add(selectionBuy);
		panneauBas.add(selectionSell);
		panneauBas.add(totalSelection);
		panneauHaut.add(btnImportTransaction);
		panneauHaut.add(btnSave);
		add(panneauHaut, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(panneauRight,BorderLayout.EAST);
		panneauRight.setLayout(new BorderLayout(0, 0));
		editorPanel.add(orderEntryPanel, BorderLayout.CENTER);
		editorPanel.add(panelButton, BorderLayout.SOUTH);
		
		panelButton.add(btnSaveOrder);
		panelButton.add(btnNewEntry);
		panelButton.add(btnDeleteOrder);

		panneauRight.add(editorPanel, BorderLayout.NORTH);
		panneauRight.add(panelComparator, BorderLayout.SOUTH);
			
		panelComparator.add(lblComparator);
		
		chartesContainerPanel = new JPanel();
		panneauRight.add(chartesContainerPanel, BorderLayout.CENTER);
		chartesContainerPanel.setLayout(new GridLayout(2, 1, 0, 0));
		pricesPanel = new HistoryPricesPanel(false);
		chartesContainerPanel.add(pricesPanel);
		chartesContainerPanel.add(editionFinancialChartPanel);
		
		
		
		btnSaveOrder.addActionListener(ae->{
			orderEntryPanel.save();
			model.fireTableDataChanged();
		});
		
		btnNewEntry.addActionListener(ae->{
			model.addItem(orderEntryPanel.newOrderEntry());
			calulate(model.getItems());
		});
		
		
		btnDeleteOrder.addActionListener(ae->{
			model.removeItem(UITools.getTableSelection(table, 0));
			calulate(model.getItems());
		});
		add(panneauBas,BorderLayout.SOUTH);
		
		
		loadFinancialBook();
		table.setSortOrder(2, SortOrder.DESCENDING);
		
		
		
		table.getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting()) {
				try {
				OrderEntry o = (OrderEntry) UITools.getTableSelection(table, 0).get(0);
				orderEntryPanel.setOrderEntry(o);
				
				calulate(UITools.getTableSelection(table, 0));
			
				ThreadManager.getInstance().execute(()->{
						MagicCard mc=null;
						try {
							mc = MTGControler.getInstance().getEnabled(MTGCardsProvider.class).searchCardByName(o.getDescription(), o.getEdition(), false).get(0);
						}
						catch(Exception e)
						{
							//do nothing
						}	
						Currency source = MTGControler.getInstance().getEnabled(MTGDashBoard.class).getCurrency();
						CardPriceVariations e;
						try {
							e = MTGControler.getInstance().getEnabled(MTGDashBoard.class).getPriceVariation(mc, o.getEdition());
						
						Double actualValue = MTGControler.getInstance().getCurrencyService().convert(source, o.getCurrency(), e.get(e.getLastDay()));
						
						editionFinancialChartPanel.init(o.getEdition());
						
						lblComparator.setText(o.getCurrency() + " VALUE="+UITools.formatDouble(actualValue) + " " +o.getTypeTransaction() + " =" + UITools.formatDouble(o.getItemPrice()));
						if(actualValue<o.getItemPrice())
							lblComparator.setIcon((o.getTypeTransaction()==TYPE_TRANSACTION.BUY)?MTGConstants.ICON_DOWN:MTGConstants.ICON_UP);
						else if(actualValue>o.getItemPrice())
							lblComparator.setIcon((o.getTypeTransaction()==TYPE_TRANSACTION.BUY)?MTGConstants.ICON_UP:MTGConstants.ICON_DOWN);
						else
							lblComparator.setIcon(null);
						} catch (IOException e1) {
							//do nothing
						}
							
						
						
						pricesPanel.init(mc, o.getEdition(), o.getDescription());
						pricesPanel.revalidate();
						
				}, "loading prices for "+o.getDescription());
				
				
				
				btnDeleteOrder.setEnabled(true);
				btnSaveOrder.setEnabled(true);
				btnSave.setEnabled(true);
				}
				catch(Exception e)
				{
					btnDeleteOrder.setEnabled(false);
					btnSave.setEnabled(false);

				}
			}
		});
		
		btnSave.addActionListener(ae->{
			try {
				MTGControler.getInstance().getFinancialService().saveBook(model.getItems());
				MTGControler.getInstance().notify(new MTGNotification("Confirmation", "Financial book saved",MESSAGE_TYPE.INFO));
			} catch (IOException e) {
				MTGControler.getInstance().notify(new MTGNotification("ERROR", e));
			}
		});
		
		btnImportTransaction.addActionListener(ae->{
			OrderImporterDialog diag = new OrderImporterDialog();
			diag.setVisible(true);
			model.addItems(diag.getSelectedEntries());
			calulate(model.getItems());
		});
		
	}

	public static void main(String[] args) {
		MTGControler.getInstance().getEnabled(MTGCardsProvider.class).init();
		MTGUIComponent.createJDialog(new BalanceGUI(),true,false).setVisible(true);
	}

	@Override
	public String getTitle() {
		return MTGControler.getInstance().getLangService().getCapitalize("FINANCIAL_MODULE");
	}
	
	@Override
	public ImageIcon getIcon() {
		return MTGConstants.ICON_SHOP;
	}
	
	private void calulate(List<OrderEntry> entries)
	{
		double totalS=0;
		double totalB=0;

		for(OrderEntry e : entries)
		{
			if(e.getTypeTransaction().equals(TYPE_TRANSACTION.BUY))
				totalB=totalB+e.getItemPrice();
			else
				totalS=totalS+e.getItemPrice();
		}
	
		if(entries.size()<model.getRowCount())
		{
			selectionBuy.setText(UITools.formatDouble(totalB));
			selectionSell.setText(UITools.formatDouble(totalS));
			totalSelection.setText(": "+UITools.formatDouble(totalS-totalB)+")");
			if((totalS-totalB)>0)
				totalSelection.setIcon(MTGConstants.ICON_UP);
			else
				totalSelection.setIcon(MTGConstants.ICON_DOWN);
			
		}
		else
		{
			totalBuy.setText(UITools.formatDouble(totalB));
			totalSell.setText(UITools.formatDouble(totalS));
			total.setText(UITools.formatDouble(totalS-totalB));
			
			if((totalS-totalB)>0)
				total.setIcon(MTGConstants.ICON_UP);
			else
				total.setIcon(MTGConstants.ICON_DOWN);
		}
	}
	

}