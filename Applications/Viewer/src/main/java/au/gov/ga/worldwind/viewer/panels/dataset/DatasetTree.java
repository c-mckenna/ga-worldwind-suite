package au.gov.ga.worldwind.viewer.panels.dataset;

import au.gov.ga.worldwind.common.ui.ClearableBasicTreeUI;
import au.gov.ga.worldwind.common.ui.lazytree.LazyTree;
import au.gov.ga.worldwind.common.ui.lazytree.LazyTreeModel;

/**
 * {@link LazyTree} subclass with a custom cell renderer that renders dataset
 * nodes.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class DatasetTree extends LazyTree
{
	public DatasetTree(LazyTreeModel treeModel)
	{
		super(treeModel);

		setUI(new ClearableBasicTreeUI());
		setCellRenderer(new DatasetCellRenderer());

		setRootVisible(false);
		setShowsRootHandles(true);
		setRowHeight(0);
	}

	@Override
	public LazyTreeModel getModel()
	{
		return (LazyTreeModel) super.getModel();
	}

	@Override
	public ClearableBasicTreeUI getUI()
	{
		return (ClearableBasicTreeUI) super.getUI();
	}

	public DatasetCellRenderer getDatasetCellRenderer()
	{
		return (DatasetCellRenderer) getCellRenderer();
	}
}
