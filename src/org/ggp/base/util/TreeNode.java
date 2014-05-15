package org.ggp.base.util;

import java.util.ArrayList;
import java.util.List;

public class TreeNode <T> {
	public T value = null;
	public TreeNode<T> parent = null;
	public List<TreeNode<T>> children = new ArrayList<TreeNode<T>>();
}
