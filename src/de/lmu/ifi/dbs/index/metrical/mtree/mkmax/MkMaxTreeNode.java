package de.lmu.ifi.dbs.index.metrical.mtree.mkmax;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Represents a node in a MkNN-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkMaxTreeNode<O extends DatabaseObject, D extends Distance<D>> extends MTreeNode<O, D> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkMaxTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkMaxTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkMaxTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkMaxTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkMaxTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkMaxTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum
   * knn distance of all entries.
   *
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected D kNNDistance(DistanceFunction<O, D> distanceFunction) {
    D knnDist = distanceFunction.nullDistance();
    for (int i = 0; i < numEntries; i++) {
      MkMaxEntry<D> entry = (MkMaxEntry<D>) entries[i];
      knnDist = Util.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }
}
