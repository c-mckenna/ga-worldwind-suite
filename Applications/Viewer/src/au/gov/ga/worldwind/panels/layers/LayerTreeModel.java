package au.gov.ga.worldwind.panels.layers;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import au.gov.ga.worldwind.panels.dataset.ILayerDefinition;

public class LayerTreeModel implements TreeModel, TreeExpansionListener
{
	private INode root;
	private JTree tree;
	private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
	private Map<URL, Set<LayerNode>> layerURLs = new HashMap<URL, Set<LayerNode>>();

	public LayerTreeModel(JTree tree, INode root)
	{
		this.tree = tree;
		this.root = root;
		addLayerChildrenURLsToMap(root);
	}

	private void addLayerChildrenURLsToMap(INode node)
	{
		for (int i = 0; i < node.getChildCount(); i++)
		{
			INode child = node.getChild(i);
			if (child instanceof LayerNode)
			{
				addedLayer((LayerNode) child);
			}
			addLayerChildrenURLsToMap(child);
		}
	}

	public void addToRoot(INode node)
	{
		insertNodeInto(node, root, root.getChildCount());
	}

	public void addLayer(ILayerDefinition layer)
	{
		INode node = LayerNode.createFromLayerDefinition(layer);
		TreePath selected = tree.getSelectionPath();
		if (selected == null)
			addToRoot(node);
		else
		{
			INode parent = (INode) selected.getLastPathComponent();
			insertNodeInto(node, parent, parent.getChildCount());
		}
		((ClearableBasicTreeUI) tree.getUI()).relayout();
	}

	public void removeLayer(ILayerDefinition layer)
	{
		URL url = layer.getLayerURL();
		if (layerURLs.containsKey(url))
		{
			Set<LayerNode> set = layerURLs.get(url);
			while (!set.isEmpty())
			{
				//this will call removedLayer() which will remove the node from the set
				removeNodeFromParent(set.iterator().next());
			}
		}
	}

	public boolean containsLayer(ILayerDefinition layer)
	{
		return layerURLs.containsKey(layer.getLayerURL());
	}

	public void addedLayer(LayerNode node)
	{
		URL url = node.getLayerURL();
		Set<LayerNode> set;
		if (layerURLs.containsKey(url))
		{
			set = layerURLs.get(url);
		}
		else
		{
			set = new HashSet<LayerNode>();
			layerURLs.put(url, set);
		}
		set.add(node);
	}

	public void removedLayer(LayerNode node)
	{
		URL url = node.getLayerURL();
		if (layerURLs.containsKey(url))
		{
			Set<LayerNode> set = layerURLs.get(url);
			set.remove(node);
			if (set.isEmpty())
			{
				layerURLs.remove(url);
			}
		}
	}

	public Object getChild(Object parent, int index)
	{
		INode node = (INode) parent;
		if (index < 0 || index >= node.getChildCount())
			return null;
		return node.getChild(index);
	}

	public int getChildCount(Object parent)
	{
		INode node = (INode) parent;
		return node.getChildCount();
	}

	public int getIndexOfChild(Object parent, Object child)
	{
		INode node = (INode) parent;
		return node.getChildIndex(child);
	}

	public INode getRoot()
	{
		return root;
	}

	public boolean isLeaf(Object node)
	{
		return getChildCount(node) == 0;
	}

	public void valueForPathChanged(TreePath path, Object newValue)
	{
		if (newValue instanceof String)
		{
			String s = (String) newValue;
			INode item = (INode) path.getLastPathComponent();
			if (!item.getName().equals(s))
			{
				item.setName(s);
				nodeChanged(item);
			}
		}
	}

	public void addTreeModelListener(TreeModelListener l)
	{
		listeners.add(l);
	}

	public void removeTreeModelListener(TreeModelListener l)
	{
		listeners.remove(l);
	}

	public void insertNodeInto(INode newNode, INode parentNode, int index)
	{
		parentNode.insertChild(index, newNode);

		int[] newIndexs = new int[1];
		newIndexs[0] = index;
		nodesWereInserted(parentNode, newIndexs);

		if (newNode instanceof LayerNode)
		{
			addedLayer((LayerNode) newNode);
		}
	}

	public void removeNodeFromParent(INode node)
	{
		INode parent = node.getParent();
		if (parent != null)
		{
			int index = getIndexOfChild(parent, node);
			parent.removeChild(node);

			int[] childIndex = new int[1];
			Object[] removedArray = new Object[1];

			childIndex[0] = index;
			removedArray[0] = node;
			nodesWereRemoved(parent, childIndex, removedArray);
		}
		if (node instanceof LayerNode)
		{
			removedLayer((LayerNode) node);
		}
	}

	public INode getParent(INode targetNode)
	{
		return targetNode.getParent();
	}

	public void treeCollapsed(TreeExpansionEvent event)
	{
		Object o = event.getPath().getLastPathComponent();
		INode node = (INode) o;
		node.setExpanded(false);
	}

	public void treeExpanded(TreeExpansionEvent event)
	{
		Object o = event.getPath().getLastPathComponent();
		INode node = (INode) o;
		node.setExpanded(true);
	}

	public void expandNodes()
	{
		List<INode> path = new ArrayList<INode>();
		path.add(root);
		expandIfRequired(tree, path);
	}

	private void expandIfRequired(JTree tree, List<INode> path)
	{
		INode last = path.get(path.size() - 1);
		if (last.isExpanded() && last.getChildCount() > 0)
		{
			TreePath tp = new TreePath(path.toArray());
			tree.expandPath(tp);

			for (int i = 0; i < last.getChildCount(); i++)
			{
				INode child = last.getChild(i);
				path.add(child);
				expandIfRequired(tree, path); //call recursively
				path.remove(path.size() - 1);
			}
		}
	}

	//----------------------------------------------------------------------------------//
	// METHODS BELOW ARE FROM DefaultTreeModel, adapted for the LayerTreeModel's INodes //
	//----------------------------------------------------------------------------------//

	private void nodeChanged(INode node)
	{
		if (node != null)
		{
			INode parent = node.getParent();

			if (parent != null)
			{
				int anIndex = getIndexOfChild(parent, node);
				if (anIndex != -1)
				{
					int[] cIndexs = new int[1];

					cIndexs[0] = anIndex;
					nodesChanged(parent, cIndexs);
				}
			}
			else if (node == getRoot())
			{
				nodesChanged(node, null);
			}
		}
	}

	private void nodesChanged(INode node, int[] childIndices)
	{
		if (node != null)
		{
			if (childIndices != null)
			{
				int cCount = childIndices.length;

				if (cCount > 0)
				{
					Object[] cChildren = new Object[cCount];

					for (int counter = 0; counter < cCount; counter++)
						cChildren[counter] = getChild(node, childIndices[counter]);
					fireTreeNodesChanged(this, getPathToRoot(node), childIndices, cChildren);
				}
			}
			else if (node == getRoot())
			{
				fireTreeNodesChanged(this, getPathToRoot(node), null, null);
			}
		}
	}

	private void nodesWereInserted(INode node, int[] childIndices)
	{
		if (node != null && childIndices != null && childIndices.length > 0)
		{
			int cCount = childIndices.length;
			Object[] newChildren = new Object[cCount];

			for (int counter = 0; counter < cCount; counter++)
				newChildren[counter] = getChild(node, childIndices[counter]);
			fireTreeNodesInserted(this, getPathToRoot(node), childIndices, newChildren);
		}
	}

	private void nodesWereRemoved(INode node, int[] childIndices, Object[] removedChildren)
	{
		if (node != null && childIndices != null)
		{
			fireTreeNodesRemoved(this, getPathToRoot(node), childIndices, removedChildren);
		}
	}

	private INode[] getPathToRoot(INode aNode)
	{
		return getPathToRoot(aNode, 0);
	}

	private INode[] getPathToRoot(INode aNode, int depth)
	{
		INode[] nodes;
		// This method recurses, traversing towards the root in order
		// size the array. On the way back, it fills in the nodes,
		// starting from the root and working back to the original node.

		/* Check for null, in case someone passed in a null node, or
		   they passed in an element that isn't rooted at root. */
		if (aNode == null)
		{
			if (depth == 0)
				return null;
			else
				nodes = new INode[depth];
		}
		else
		{
			depth++;
			if (aNode == root)
				nodes = new INode[depth];
			else
				nodes = getPathToRoot(aNode.getParent(), depth);
			nodes[nodes.length - depth] = aNode;
		}
		return nodes;
	}

	protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices,
			Object[] children)
	{
		TreeModelEvent e = new TreeModelEvent(source, path, childIndices, children);
		for (TreeModelListener l : listeners)
			l.treeNodesChanged(e);
	}

	private void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices,
			Object[] children)
	{
		TreeModelEvent e = new TreeModelEvent(source, path, childIndices, children);
		for (TreeModelListener l : listeners)
			l.treeNodesInserted(e);
	}

	private void fireTreeNodesRemoved(Object source, Object[] path, int[] childIndices,
			Object[] children)
	{
		TreeModelEvent e = new TreeModelEvent(source, path, childIndices, children);
		for (TreeModelListener l : listeners)
			l.treeNodesRemoved(e);
	}
}
