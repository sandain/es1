/*
 *    Ecotype Simulation models the sequence diversity within a bacterial
 *    clade as the evolutionary result of net ecotype formation and periodic
 *    selection, yielding a certain number of ecotypes.
 *
 *    Copyright (C) 2009       Andrew Warner, Wesleyan University
 *    Copyright (C) 2009-2014  Jason M. Wood, Montana State University
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package ecosim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  Reads in a Newick tree from a file and provides options to traverse it.
 *
 *  @author Andrew Warner
 *  @author Jason M. Wood
 *  @copyright GNU General Public License
 */
public class NewickTree implements Comparable<NewickTree> {

    public NewickTree () {
        root = new NewickTreeNode ();
    }

    /**
     *  Constructor for objects of class NewickTree.
     *
     *  @param tree Newick formated tree to read.
     */
    public NewickTree (String tree) throws InvalidNewickException {
        // Parse through the provided newick formated tree string.
        root = loadTree (tree);
        // Make sure that we actually have a tree.
        if (root.getChildren ().size () <= 1) {
            throw new InvalidNewickException (
                "Malformed Newick tree, not enough leaves found."
            );
        }
    }

    /**
     *  Constructor for objects of class NewickTree.
     *
     *  @param tree Newick formated tree to read.
     */
    public NewickTree (File tree) throws InvalidNewickException {
        // Load the file containing the newick formated tree.
        root = loadTreeFile (tree);
        // Make sure that we actually have a tree.
        if (root.getChildren ().size () <= 1) {
            throw new InvalidNewickException (
                "Malformed Newick tree, not enough leaves found."
            );
        }
    }

    /**
     *  Constructor for objects of class NewickTree.
     *
     *  @param tree NewickTree to use.
     */
    public NewickTree (NewickTree tree) throws InvalidNewickException {
        root = loadTree (tree.toString ());
    }

    /**
     *  Compare Newick Trees.
     */
    public int compareTo (NewickTree other) {
        return root.compareTo (other.getRoot ());
    }

    /**
     *  Returns the root node of this tree.
     *
     *  @return NewickTreeNode containing the root node.
     */
    public NewickTreeNode getRoot () {
        return root;
    }

    /**
     *  Returns the descendant with the given name.
     *
     *  @param name The name of the descendant.
     *  @return NewickTreeNode of the descendant.
     */
    public NewickTreeNode getDescendant (String name) {
        NewickTreeNode namedDescendant = new NewickTreeNode ();
        for (NewickTreeNode descendant: root.getDescendants ()) {
            if (descendant.getName ().equals (name)) {
                namedDescendant = descendant;
                break;
            }
        }
        return namedDescendant;
    }

    /**
     *  Returns an array of nodes that are descendants of the root node.
     *
     *  @return The descendants of the root node.
     */
    public ArrayList<NewickTreeNode> getDescendants () {
        return root.getDescendants ();
    }

    /**
     *  Remove a leaf node descendant from the tree.
     *
     *  @param name The name of the descendant to remove.
     */
    public void removeDescendant (String name) {
        NewickTreeNode descendant = getDescendant (name);
        removeDescendant (descendant);
    }

    /**
     *  Remove a leaf node descendant from the tree.
     *
     * Since we assume that each internal node has exactly two children,
     * the parent node of the descendant being destroyed will also have to
     * be destroyed.  The other child will have its grandparent become its
     * new parent, and its distance will become the sum of its distance and
     * its parents distance.
     *
     *  @param descendant The descendant to remove.
     */
    public void removeDescendant (NewickTreeNode descendant) {
        if (! descendant.isLeafNode ()) return;
        // Grab the parent node of the descendant.
        NewickTreeNode parent = descendant.getParent ();
        // Remove the descendant as a child of the parent node.
        parent.removeChild (descendant);
        // Grab the other child node of the parent.
        NewickTreeNode otherChild = parent.getChildren ().get (0);
        // Calculate a new distance for the other child node.
        Double distance = otherChild.getDistance () + parent.getDistance ();
        otherChild.setDistance (distance);
        // Setup the relationships of the other child node.
        if (parent.isRootNode ()) {
            // The parent is the root node, the other child becomes the new 
            // root node.
            otherChild.setParent (null);
            root = otherChild;
        }
        else {
            // The grandparent of the child node is the new parent.
            NewickTreeNode grandparent = parent.getParent ();
            grandparent.removeChild (parent);
            grandparent.addChild (otherChild);
        }
    }

    /**
     *  Reroot the tree so that the outgroup descendant is next to the root.
     *
     *  @param name The name of the outgroup descendant.
     */
    public void reroot (String name) {
        NewickTreeNode outgroup = getDescendant (name);
        reroot (outgroup);
    }

    /**
     *  Reroot the tree so that the outgroup descendant is next to the root.
     *
     *  @param outgroup The outgroup descendant.
     */
    public void reroot (NewickTreeNode outgroup) {
        NewickTreeNode newRoot = new NewickTreeNode ();
        NewickTreeNode oldParent = outgroup.getParent ();
        // Add the outgroup to the new root, and remove it from its old
        // parents children.
        oldParent.removeChild (outgroup);
        newRoot.addChild (outgroup);
        double distance = outgroup.getDistance () * 0.5;
        double oldDistance = oldParent.getDistance ();
        outgroup.setDistance (distance);
        outgroup.setOutgroup (true);
        oldParent.setDistance (distance);
        NewickTreeNode newParent = newRoot;
        while (oldParent != root) {
            NewickTreeNode node = oldParent;
            oldParent = node.getParent ();
            oldParent.removeChild (node);
            newParent.addChild (node);
            newParent = node;
            node.setDistance (distance);
            distance = oldDistance;
            oldDistance = oldParent.getDistance ();
        }
        for (NewickTreeNode child: oldParent.getChildren ()) {
          newParent.addChild (child);
          distance = child.getDistance () + distance;
          child.setDistance (distance);
        }
        // Save the the new root.
        root = newRoot;
    }


    /**
     *  Return the number of living descendants of the root node.
     *
     *  @return The number of descendants.
     */
    public int size () {
        return root.numberOfDescendants ();
    }

    /**
     *  Returns this tree as a Newick formatted String.
     *
     *  @return Newick formatted String containing the tree.
     */
    public String toString () {
        return root.toString ();
    }

    /**
     *  Save the tree data in this object to a Newick formatted file.
     *
     *  @param fileName File name to write the Newick tree to.
     *  @return True if the save was a success, False otherwise.
     */
    public boolean save (String fileName) {
        return save (new File (fileName));
    }

    /**
     *  Save the tree data in this object to a Newick formatted file.
     *
     *  @param file File to write the Newick tree to.
     *  @return True if the save was a success, False otherwise.
     */
    public boolean save (File file) {
        boolean success = false;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter (new FileWriter (file));
            out.write (root.toString () + "\n");
        }
        catch (IOException e) {
            System.out.println ("Error writing to output file.");
        }
        finally {
            try {
                out.close ();
                success = true;
            }
            catch (IOException e) {
                System.out.println ("Error closing output file.");
            }
        }
        return success;
    }

    /**
     *  Check if this NewickTree object is valid.
     *
     *  @return True if this is a valid NewickTree object, False if not.
     */
    public boolean isValid () {
        boolean valid = false;
        if (root.getDescendants ().size () > 0) {
            valid = true;
        }
        return valid;
    }

    /**
     *  Reads the tree in from an input file.
     *
     *  @param treeFile The Newick formatted tree file to load.
     *  @return NewickTreeNode containing the root of the Newick tree.
     */
    private NewickTreeNode loadTreeFile (File treeFile)
        throws InvalidNewickException {
        NewickTreeNode node = new NewickTreeNode ();
        BufferedReader input = null;
        if (treeFile == null) {
            throw new InvalidNewickException (
                "Newick tree file not supplied."
            );
        }
        else if (! treeFile.exists ()) {
            throw new InvalidNewickException (
                "Newick tree file does not exist."
            );
        }
        try {
            // Read in the tree file.
            input = new BufferedReader (new FileReader (treeFile));
            StringBuffer treeBuffer = new StringBuffer ();
            String line = input.readLine ();
            while (line != null){
                treeBuffer.append (line);
                line = input.readLine ();
            }
            node = loadTree (treeBuffer.toString ());
            // Sort the children of the node.
            node.sortChildren ();
        }
        catch (java.io.FileNotFoundException e) {
            throw new InvalidNewickException ("File not found.");
        }
        catch (java.io.IOException e) {
            throw new InvalidNewickException ("IO error.");
        }
        finally {
            try {
                input.close ();
            }
            catch (IOException e) {
                throw new InvalidNewickException ("IO error.");
            }
        }
        return node;
    }

    /**
     *  Grabs the root node and its descendants from the input tree string.
     *
     *  @param inputTree The newick formated tree containing the root node
     *         and its descendants.
     *  @return The root node.
     */
    private NewickTreeNode loadTree (String inputTree)
        throws InvalidNewickException {
        NewickTreeNode node = null;
        String tree = inputTree;
        // Phylip can return tree files that contain multiple trees
        // separated by semicolons, handle only the first one.
        if (tree.contains (";")) {
            String[] trees = inputTree.split (";");
            tree = trees[0];
        }
        // Remove any spaces from the Newick tree before continuing.
        tree = tree.replace (" ", "");
        // Make sure that we have something to parse.
        if (tree.length () > 0) {
            node = parseTree (tree);
        }
        if (node == null) {
            throw new InvalidNewickException (
                "Malformed Newick tree."
            );
        }
        return node;
    }

    /**
     *  The recursive method used to parse through a Newick tree.
     *
     *  @param tree The subtree to parse.
     *  @return A NewickTreeNode containing the subtree.
     */
    private NewickTreeNode parseTree (String tree)
        throws InvalidNewickException {
        NewickTreeNode node = new NewickTreeNode ();
        String metaString;
        // Check if the current node has a subTree.
        if (tree.length () > 0 && tree.charAt (0) == '(') {
            // Split the sub tree from the meta data.
            int length = getSpaceInBetweenParens (0, tree);
            String subTree = tree.substring (1, length);
            metaString = tree.substring (length + 1, tree.length ());
            // Parse this node's sub tree.
            StringBuffer buffer = new StringBuffer ();
            int i = 0;
            while (i < subTree.length ()) {
                // Check to see if this child node has a subtree, append it to
                // the buffer.
                if (subTree.charAt (i) == '(') {
                    int j = getSpaceInBetweenParens (i, subTree);
                    buffer.append (subTree.substring (i, j));
                    i = j;
                }
                // Keep appending the child node's meta data to the buffer
                // until we find a new child node.  If we do find a new child
                // node, add it to the current node by recursion with the
                // buffer as the subTree, then clear the buffer.
                if (subTree.charAt (i) != ',') {
                    buffer.append (subTree.charAt (i));
                }
                else {
                    node.addChild (parseTree (buffer.toString ()));
                    buffer = new StringBuffer ();
                }
                i ++;
            }
            // Add a new child to the current node by recursion with the
            // buffer as the subTree.
            node.addChild (parseTree (buffer.toString ()));
        }
        else {
            metaString = tree;
        }
        // Parse this node's meta data.
        if (metaString.length () > 0) {
            String[] meta = metaString.split (":", 2);
            if (meta.length > 0 && meta[0].length () > 0) {
                node.setName (meta[0]);
            }
            if (meta.length > 1 && meta[1].length () > 0) {
                try {
                    Double distance = new Double (meta[1]);
                    node.setDistance (distance.doubleValue ());
                }
                catch (NumberFormatException e) {
                    throw new InvalidNewickException (
                        "Malformed Newick tree, expected a number."
                    );
                }
            }
        }
        return node;
    }

    /**
     *  Get the space in between a set of parentheses, ie (   ), which could
     *  contain other open and close parentheses within it.
     *
     *  @param index The index of the first open parenthesis.
     *  @param tree The tree, or the tree segment.
     *  @return The index of the close parenthesis corresponding to the given
     *  index of the open parenthesis.
     */
    private int getSpaceInBetweenParens (int index, String tree)
        throws InvalidNewickException {
        int numOpenParensSeen = 0;
        for (int i = index; i < tree.length (); i ++) {
            if (tree.charAt (i) == '(') {
                numOpenParensSeen ++;
            }
            else if (tree.charAt (i) == ')') {
                numOpenParensSeen --;
                if (numOpenParensSeen == 0) {
                    return i;
                }
            }
        }
        // Throw an exception if there is there are unmatched parentheses.
        if (numOpenParensSeen != 0) {
            throw new InvalidNewickException (
                "Malformed Newick tree, unmatched parentheses."
            );
        }
        return 0;
    }

    private NewickTreeNode root;

}
