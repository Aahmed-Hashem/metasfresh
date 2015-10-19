/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                        *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.grid.tree;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.adempiere.misc.service.IPOService;
import org.adempiere.plaf.AdempierePLAF;
import org.adempiere.plaf.VTreePanelUI;
import org.adempiere.process.event.IProcessEventListener;
import org.adempiere.process.event.ProcessEvent;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.api.IMsgBL;
import org.compiere.apps.ADialog;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.model.PO;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.CLabel;
import org.compiere.swing.CMenuItem;
import org.compiere.swing.CPanel;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import de.schaeffer.compiere.tools.DocumentSearch;

/**
 * Tree Panel displays trees. <br>
 * When a node is selected by Left Click, a propertyChange (NODE_SELECTION) event is fired
 * 
 * <pre>
 * 	PropertyChangeListener -
 * 		treePanel.addPropertyChangeListener(VTreePanel.NODE_SELECTION, this);
 * 		calls: public void propertyChange(PropertyChangeEvent e)
 * </pre>
 * 
 * To select a specific node call setSelectedNode(NodeID);
 *
 * @author Jorg Janke
 * @version $Id: VTreePanel.java,v 1.3 2006/07/30 00:51:28 jjanke Exp $
 *
 * @author kthiemann / Carlos Ruiz <li>2761420 - Advanced Search
 * 
 * @author Teo Sarca <li>BF [ 2866493 ] VTreePanel is not saving who did the node move https://sourceforge.net/tracker/?func=detail&atid=879332&aid=2866493&group_id=176962
 *
 * @author Paul Bowden <li>FR [ 2032092 ] Java 6 improvements to tree drag and drop https://sourceforge.net/tracker/index.php?func=detail&aid=2032092&group_id=176962&atid=879335
 */
public final class VTreePanel extends CPanel
		// metas: adding process event support to handle record creation, change
		// and deletion by processes.
		implements IProcessEventListener
{
	// services
	private final transient IMsgBL msgBL = Services.get(IMsgBL.class);

	/******************************************************************************
	 * Mouse Clicked
	 */
	private static class VTreePanel_mouseAdapter extends java.awt.event.MouseAdapter
	{
		VTreePanel m_adaptee;

		/**
		 * VTreePanel_mouseAdapter
		 *
		 * @param adaptee
		 */
		VTreePanel_mouseAdapter(final VTreePanel adaptee)
		{
			m_adaptee = adaptee;
		}

		/**
		 * Mouse Clicked
		 *
		 * @param e
		 */
		@Override
		public void mouseClicked(MouseEvent e)
		{
			m_adaptee.mouseClicked(e);
		}
	}   // VTreePanel_mouseAdapter

	/**
	 * Key Pressed
	 */
	private static class VTreePanel_keyAdapter extends java.awt.event.KeyAdapter
	{
		VTreePanel m_adaptee;

		/**
		 * VTreePanel_keyAdapter
		 *
		 * @param adaptee
		 */
		VTreePanel_keyAdapter(VTreePanel adaptee)
		{
			m_adaptee = adaptee;
		}

		/**
		 * Key Pressed
		 *
		 * @param e
		 */
		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_ENTER)
				m_adaptee.keyPressed(e);
		}
	}   // VTreePanel_keyAdapter

	/**
	 *
	 */
	private static final long serialVersionUID = -6798614427038652192L;

	private static final String PREFIX_DOCUMENT_SEARCH = "/";

	private VTreeTransferHandler handler = new VTreeTransferHandler();

	/**
	 * Tree Panel for browsing and editing of a tree. Need to call initTree
	 * 
	 * @param WindowNo WindowNo
	 * @param editable if true you can edit it
	 * @param hasBar has OutlookBar
	 */
	public VTreePanel(int WindowNo_IGNORED, final boolean hasBar, final boolean editable)
	{
		super();
		tree.setModel(new AdempiereTreeModel()); // set an empty tree model just to avoid displaying the sample tree nodes
		log.config("Bar=" + hasBar + ", Editable=" + editable);

		m_hasBar = hasBar;
		m_editable = editable;

		// static init
		jbInit();
		if (!m_hasBar)
		{
			treePopupMenu.remove(mFavoritesBarAdd);
		}

		// base settings
		if (editable)
		{
			tree.setDragEnabled(true);
			tree.setTransferHandler(handler);
			tree.setDropMode(DropMode.INSERT);
			setMappings(tree);
		}
		else
		{
			treePopupMenu.remove(mFrom);
			treePopupMenu.remove(mTo);
		}
	}   // VTreePanel

	/**
	 * Tree initialization. May be called several times
	 *
	 * @param AD_Tree_ID tree to load
	 * @return true if loaded ok
	 */
	public boolean initTree(final int AD_Tree_ID)
	{
		log.config("AD_Tree_ID=" + AD_Tree_ID);
		//
		m_AD_Tree_ID = AD_Tree_ID;

		// Get Tree
		final MTree vTree = new MTree(Env.getCtx(), AD_Tree_ID, m_editable, true, null);
		m_root = vTree.getRoot();
		m_root.setName(Services.get(IMsgBL.class).getMsg(Env.getCtx(), vTree.getName())); // translate name of menu.
		log.log(Level.CONFIG, "root={0}", m_root);

		treeModel = new AdempiereTreeModel(m_root);
		treeModel.setMTree(vTree);
		tree.setModel(treeModel);

		// Shortcut Bar
		if (m_hasBar)
		{
			favoritesBar.loadAllFromRoot(m_root);
		}
		return true;
	}   // initTree

	/** Logger */
	private static final transient CLogger log = CLogger.getCLogger(VTreePanel.class);

	private final JTree tree = new JTree();
	private AdempiereTreeModel treeModel;
	private CCheckBox treeExpand = new CCheckBox();
	private final CTextField treeSearch = new CTextField(10);
	private JPopupMenu treePopupMenu = new JPopupMenu();
	private CMenuItem mFrom = new CMenuItem();
	private CMenuItem mTo = new CMenuItem();
	private final FavoritesGroupContainer favoritesBar = new FavoritesGroupContainer();
	private final CMenuItem mFavoritesBarAdd = new CMenuItem();
	private JSplitPane centerSplitPane;
	private final MouseListener mouseListener = new VTreePanel_mouseAdapter(this);
	private final KeyListener keyListener = new VTreePanel_keyAdapter(this);

	/** Tree ID */
	private int m_AD_Tree_ID = 0;

	/** Tree is editable (can move nodes) - also not active shown */
	private final boolean m_editable;
	/** Tree has a shortcut Bar */
	private final boolean m_hasBar;
	/** The root node */
	private MTreeNode m_root = null;

	private String m_search = "";
	private Enumeration<?> m_nodeEn;
	private MTreeNode m_selectedNode;	// the selected model node

	/** Property Listener NodeSelected by Left Click */
	public static final String NODE_SELECTION = "NodeSelected";

	/**
	 * Static Component initialization.
	 * 
	 * <pre>
	 * -centerSplitPane
	 * 		- treePane
	 * 		- tree
	 * 		- bar
	 * 		- southPanel
	 * </pre>
	 */
	private void jbInit()
	{
		//
		// Setup the Tree component
		{
			//
			// only one node to be selected
			final DefaultTreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
			treeSelectionModel.setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
			tree.setSelectionModel(treeSelectionModel);
			//
			tree.setEditable(false); // don't allow to change the node text
			// TODO task 7393: add "real" tree listeners: http://docs.oracle.com/javase/tutorial/uiswing/components/tree.html#select
			tree.addMouseListener(mouseListener);
			tree.addKeyListener(keyListener);
			tree.setCellRenderer(new VTreeCellRenderer());
			tree.setBorder(BorderFactory.createEmptyBorder());

			// metas: implementing a tree expansion listener to expand leaf nodes (thus making it obvious to the user that they are leafs).
			final TreeExpansionListener treeExpansionListener = new TreeExpansionListener()
			{
				@Override
				public void treeCollapsed(final TreeExpansionEvent event)
				{
				}

				@Override
				public void treeExpanded(final TreeExpansionEvent event)
				{
					final TreeNode expandedNode = (TreeNode)event.getPath().getLastPathComponent();

					@SuppressWarnings("unchecked")
					final Enumeration<TreeNode> children = expandedNode.children();

					while (children.hasMoreElements())
					{
						final TreeNode child = children.nextElement();
						if (child.getChildCount() == 0)
						{
							tree.expandPath(event.getPath().pathByAddingChild(child));
						}
					}
				}
			};
			tree.addTreeExpansionListener(treeExpansionListener);
			// metas end
		}

		//
		// Center: Tree panel (the tree with it's toolbar)
		final CPanel treePart = new CPanel();
		{
			final BorderLayout treePartLayout = new BorderLayout();
			treePartLayout.setVgap(2); // i.e. the space between the tree and the bottom search panel
			treePart.setLayout(treePartLayout);
			treePart.setBorder(BorderFactory.createEmptyBorder());

			//
			// Center panel: the actual tree
			{
				final JScrollPane treePane = new JScrollPane();
				treePane.setViewportView(tree);
				treePane.setBorder(BorderFactory.createEmptyBorder());
				treePart.add(treePane, BorderLayout.CENTER);
			}

			//
			// North/South panel: the tree toolbar (search field, expand checkbox)
			{
				treeExpand.setText(msgBL.getMsg(Env.getCtx(), "ExpandTree"));
				treeExpand.setActionCommand("Expand");
				treeExpand.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(final ActionEvent e)
					{
						expandTree();
					}
				});
				//
				final CLabel treeSearchLabel = new CLabel();
				treeSearchLabel.setText(msgBL.getMsg(Env.getCtx(), "TreeSearch") + " ");
				treeSearchLabel.setLabelFor(treeSearch);
				treeSearchLabel.setToolTipText(msgBL.getMsg(Env.getCtx(), "TreeSearchText"));
				//
				treeSearch.setBackground(AdempierePLAF.getInfoBackground());
				treeSearch.addKeyListener(keyListener);

				final CPanel treeToolbar = new CPanel();
				treeToolbar.setLayout(new FlowLayout(FlowLayout.LEADING));
				treeToolbar.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
				treeToolbar.add(treeSearchLabel);
				treeToolbar.add(treeSearch);
				treeToolbar.add(treeExpand);

				final boolean showOnTop = AdempierePLAF.getBoolean(VTreePanelUI.KEY_SearchPanelAnchorOnTop, VTreePanelUI.DEFAULT_SearchPanelAnchorOnTop);
				treePart.add(treeToolbar, showOnTop ? BorderLayout.NORTH : BorderLayout.SOUTH);
			}
		}

		//
		// Main panel layout
		{
			final BorderLayout mainLayout = new BorderLayout();
			mainLayout.setVgap(5);
			this.setLayout(mainLayout);

			if (m_hasBar)
			{
				favoritesBar.setListener(new FavoritesListener()
				{
					@Override
					public void onNodeClicked(final MTreeNode node)
					{
						final int nodeId = node.getNode_ID();
						setTreeSelectionPath(nodeId, false);
						// Select it
						MTreeNode tn = (MTreeNode)tree.getSelectionPath().getLastPathComponent();
						setSelectedNode(tn);
					}
				});
				centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
				centerSplitPane.setOpaque(false);
				centerSplitPane.setLeftComponent(favoritesBar.getComponent());
				centerSplitPane.setRightComponent(treePart);
				centerSplitPane.setBorder(BorderFactory.createEmptyBorder());
				removeSplitPaneBorder(centerSplitPane);
				
				this.add(centerSplitPane, BorderLayout.CENTER);
			}
			else
			{
				this.add(treePart, BorderLayout.CENTER);
			}
		}

		//
		// Tree right click popup menu
		{
			mFrom.setText(msgBL.getMsg(Env.getCtx(), "ItemMove"));
			mFrom.setActionCommand((String)TransferHandler.getCutAction().getValue(Action.NAME));
			mFrom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
			mFrom.setMnemonic(KeyEvent.VK_X);
			mFrom.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					onTreeAction(e.getActionCommand());
				}
			});
			//
			mTo.setText(msgBL.getMsg(Env.getCtx(), "ItemInsert"));
			mTo.setActionCommand((String)TransferHandler.getPasteAction().getValue(Action.NAME));
			mTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
			mTo.setMnemonic(KeyEvent.VK_V);
			mTo.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					onTreeAction(e.getActionCommand());
				}
			});

			mFavoritesBarAdd.setText(msgBL.getMsg(Env.getCtx(), "BarAdd"));
			mFavoritesBarAdd.setActionCommand("BarAdd");
			mFavoritesBarAdd.addActionListener(new ActionListener()
			{
				
				@Override
				public void actionPerformed(ActionEvent e)
				{
					final MTreeNode node = (MTreeNode)tree.getSelectionPath().getLastPathComponent();
					favoritesBar.addItem(node);
				}
			});
			//
			treePopupMenu.setLightWeightPopupEnabled(false);
			treePopupMenu.add(mFavoritesBarAdd);
			treePopupMenu.add(mFrom);
			if (!m_hasBar)
			{
				treePopupMenu.addSeparator();
			}
			treePopupMenu.add(mTo);
		}
	}   // jbInit

	private static void removeSplitPaneBorder(final JSplitPane splitPane)
	{
		if (splitPane == null)
		{
			return;
		}

		final SplitPaneUI splitPaneUI = splitPane.getUI();
		if (splitPaneUI instanceof BasicSplitPaneUI)
		{
			final BasicSplitPaneUI basicUI = (BasicSplitPaneUI)splitPaneUI;
			basicUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
		}
	}

	/**
	 * Get Divider Location
	 *
	 * @return divider location
	 */
	public int getDividerLocation()
	{
		if (centerSplitPane == null)
		{
			return 0;
		}
		return centerSplitPane.getDividerLocation();
	}	// getDividerLocation

	/**
	 * Enter Key
	 * 
	 * @param e event
	 */
	protected void keyPressed(final KeyEvent e)
	{
		// CHANGED - document search
		if (e.getSource() == treeSearch && treeSearch.getText() != null
				&& treeSearch.getText().length() > 0
				&& treeSearch.getText().substring(0, 1).equals(PREFIX_DOCUMENT_SEARCH))
		{
			setBusy(true);
			try
			{
				DocumentSearch search = new DocumentSearch();
				if (search.openDocumentsByDocumentNo(treeSearch.getText().substring(1)))
					treeSearch.setText(null);
			}
			finally
			{
				setBusy(false);
			}
			return;
		}

		// *** Tree ***
		if (e.getSource() instanceof JTree
				|| (e.getSource() == treeSearch && e.getModifiers() != 0))	// InputEvent.CTRL_MASK
		{
			TreePath tp = tree.getSelectionPath();
			if (tp == null)
				ADialog.beep();
			else
			{
				MTreeNode tn = (MTreeNode)tp.getLastPathComponent();
				setSelectedNode(tn);
			}
		}

		// *** treeSearch ***
		else if (e.getSource() == treeSearch)
		{
			final String search = treeSearch.getText();
			boolean found = false;

			// at the end - try from top
			if (m_nodeEn != null && !m_nodeEn.hasMoreElements())
			{
				m_search = "";
			}

			// this is the first time
			if (!search.equals(m_search))
			{
				// get enumeration of all nodes
				m_nodeEn = m_root.preorderEnumeration();
				m_search = search;
			}

			// search the nodes
			while (!found && m_nodeEn != null && m_nodeEn.hasMoreElements())
			{
				final MTreeNode nd = (MTreeNode)m_nodeEn.nextElement();
				// compare in upper case
				if (nd.toString().toUpperCase().indexOf(search.toUpperCase()) != -1)
				{
					found = true;
					TreePath treePath = new TreePath(nd.getPath());
					tree.setSelectionPath(treePath);
					tree.makeVisible(treePath);			// expand it
					tree.scrollPathToVisible(treePath);
				}
			}
			if (!found)
				ADialog.beep();
		}   // treeSearch

	}   // keyPressed

	/*************************************************************************/

	/**
	 * Mouse clicked
	 * 
	 * @param e event
	 */
	protected void mouseClicked(MouseEvent e)
	{
		// *** JTree ***
		if (e.getSource() instanceof JTree)
		{
			// Left Double Click
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 0)
			{
				final int selRow = tree.getRowForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					final MTreeNode tn = (MTreeNode)tree.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
					setSelectedNode(tn);
				}
			}

			// Right Click for PopUp
			else if ((m_editable || m_hasBar)
					&& SwingUtilities.isRightMouseButton(e))
			{
				final int selRow = tree.getRowForLocation(e.getX(), e.getY());
				if (selRow != -1)
				{
					tree.setSelectionRow(selRow);
				}
				if (tree.getSelectionPath() != null)         // need select first
				{
					final MTreeNode nd = (MTreeNode)tree.getSelectionPath().getLastPathComponent();
					if (mFavoritesBarAdd != null)
					{
						// only leaves can be added to bar, which were not already added
						mFavoritesBarAdd.setEnabled(nd.isLeaf() && !nd.isOnBar());
					}

					final Rectangle r = tree.getPathBounds(tree.getSelectionPath());
					treePopupMenu.show(tree, (int)r.getMaxX(), (int)r.getY());
				}
			}
		}   // JTree
	}   // mouseClicked

	/**
	 * Get currently selected node
	 * 
	 * @return MTreeNode
	 */
	public MTreeNode getSelectedNode()
	{
		return m_selectedNode;
	}   // getSelectedNode

	/**
	 * Search Field
	 * 
	 * @return Search Field
	 */
	public JComponent getSearchField()
	{
		return treeSearch;
	}   // getSearchField

	/**
	 * Set Selection to Node in Event
	 * 
	 * @param nodeID Node ID
	 * @return true if selected
	 */
	public boolean setSelectedNode(final int nodeID)
	{
		log.config("ID=" + nodeID);
		if (nodeID != -1)				// new is -1
			return setTreeSelectionPath(nodeID, true);     // show selection
		return false;
	}   // setSelectedNode

	/**
	 * Select ID in Tree
	 * 
	 * @param nodeID Node ID
	 * @param makeNodeVisible scroll to node
	 * @return true if selected
	 */
	private boolean setTreeSelectionPath(final int nodeID, final boolean makeNodeVisible)
	{
		if (m_root == null)
			return false;
		log.config("NodeID=" + nodeID + ", Show=" + makeNodeVisible + ", root=" + m_root);
		// try to find the node
		MTreeNode node = m_root.findNode(nodeID);
		if (node != null)
		{
			TreePath treePath = new TreePath(node.getPath());
			log.config("Node=" + node + ", Path=" + treePath.toString());
			tree.setSelectionPath(treePath);
			if (makeNodeVisible)
			{
				tree.makeVisible(treePath);       	// expand it
				tree.scrollPathToVisible(treePath);
			}
			return true;
		}
		log.info("Node not found; ID=" + nodeID);
		return false;
	}   // selectID

	/**
	 * Set the selected node & initiate all listeners
	 * 
	 * @param nd node
	 */
	private void setSelectedNode(final MTreeNode nd)
	{
		log.config("Node = " + nd);
		m_selectedNode = nd;
		//
		firePropertyChange(NODE_SELECTION, null, nd);
	}   // setSelectedNode

	/**************************************************************************
	 * Node Changed - synchronize Node
	 *
	 * @param save true the node was saved (changed/added), false if the row was deleted
	 * @param info
	 */
	public void nodeChanged(final boolean save, final MTreeNode info)
	{
		log.config("Save=" + save + ", Info=" + info);
		// if ID==0=root - don't update it
		if (info.getNode_ID() == 0)
		{
			return;
		}
		// try to find the node ()
		MTreeNode node = m_root.findNode(info.getNode_ID());

		// Node not found and saved -> new
		if (node == null && save)
		{
			node = info;
			m_root.add(node); // add the node to the root first, but a few lines below see if we can move it to a better place

			// metas: tsa: Search on first selected node where we can insert our node:
			MTreeNode parent = getSelectedNode();
			if (parent != null && !parent.getAllowsChildren())
			{
				parent = (MTreeNode)parent.getParent();
			}
			if (parent != null && parent != m_root)
			{
				node.removeFromParent();
				parent.add(node);
				treeModel.saveChildren(parent);
			}
		}

		// Node found and saved -> change
		else if (node != null && save)
		{
			node.copyInfoFrom(info);
		}

		// Node found and not saved -> delete
		else if (node != null && !save)
		{
			final MTreeNode parent = (MTreeNode)node.getParent();
			node.removeFromParent();
			node = parent;  // select Parent
		}

		// Error
		else
		{
			log.log(Level.SEVERE, "Save=" + save + ", Info=" + info + ", Node=" + node);
			node = null;
		}

		// Nothing to display
		if (node == null)
		{
			return;
		}
		// (Re) Display Node
		tree.updateUI();
		final TreePath treePath = new TreePath(node.getPath());
		tree.setSelectionPath(treePath);
		tree.makeVisible(treePath);       	// expand it
		tree.scrollPathToVisible(treePath);
	}   // nodeChanged

	private final void onTreeAction(final String action)
	{
		final Action a = tree.getActionMap().get(action);
		if (a != null)
		{
			a.actionPerformed(new ActionEvent(tree, ActionEvent.ACTION_PERFORMED, null));
		}
	}
	
	/**
	 * Clicked on Expand All
	 */
	private void expandTree()
	{
		if (treeExpand.isSelected())
		{
			for (int row = 0; row < tree.getRowCount(); row++)
				tree.expandRow(row);
		}
		else
		{
			// patch: 1654055 +jgubo Changed direction of collapsing the tree nodes
			for (int row = tree.getRowCount(); row > 0; row--)
				tree.collapseRow(row);
		}
	}   // expandTree

	private static void setMappings(final JTree tree)
	{
		final ActionMap map = tree.getActionMap();
		map.put(TransferHandler.getCutAction().getValue(Action.NAME), TransferHandler.getCutAction());
		map.put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());
	}

	/**
	 * Indicate Busy
	 * 
	 * @param busy busy
	 */
	private void setBusy(final boolean busy)
	{
		final JFrame frame = Env.getFrame(this);
		if (frame == null)  // during init
			return;

		if (busy)
		{
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			treeSearch.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		else
		{
			this.setCursor(Cursor.getDefaultCursor());
			frame.setCursor(Cursor.getDefaultCursor());
			treeSearch.setCursor(Cursor.getDefaultCursor());
		}
	}	// set Busy

	// metas: begin:
	// adding process event support to handle record creation, change
	// and deletion by processes.
	@Override
	public void processEvent(final ProcessEvent event)
	{
		final Object objSource = event.getSource();
		//
		// first we figure out if we are interested in the changed object
		if (!(objSource instanceof PO))
		{
			return;
		}
		final PO poSource = (PO)objSource;
		final int poTreeId = MTree.getDefaultAD_Tree_ID(Env.getAD_Client_ID(Env.getCtx()), poSource.get_KeyColumns()[0]);
		if (poTreeId != m_AD_Tree_ID)
		{
			// the source has no tree at all or has a different tree than this
			// panel.
			return;
		}

		final boolean paramSave;
		if (ProcessEvent.EventType.recordDeleted.equals(event.getType()))
		{
			paramSave = false;
		}
		else
		{
			paramSave = true;
		}

		final IPOService poService = Services.get(IPOService.class);
		final String name = (String)poService.getValue(poSource, "name");
		final String description = (String)poService.getValue(poSource, "description");
		final MTreeNode info = new MTreeNode(poSource.get_ID(), 0, name, description, -1, false, null, false, null);
		nodeChanged(paramSave, info);
	}

	public AdempiereTreeModel getTreeModel()
	{
		return treeModel;
	}

	public void filterIds(final List<Integer> ids)
	{
		if (treeModel == null)
		{
			return; // nothing to do
		}

		Check.assumeNotNull(ids, "Param 'ids' is not null");
		treeModel.filterIds(ids);

		if (treeExpand.isSelected())
		{
			expandTree();
		}
		if (ids != null && ids.size() > 0)
		{
			setTreeSelectionPath(ids.get(0), true);
		}
	}

	// metas end

}   // VTreePanel
