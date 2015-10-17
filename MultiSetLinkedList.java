import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.io.*;

class LinkNode implements Serializable {

    private int data;
    private LinkNode next;

    // private constructor
    private LinkNode(int data) {
        this.data = data;
        this.next = null;
    }

    // public static factory method
    public static LinkNode createLinkNode (int data) {
        return new LinkNode(data);
    }

    // set the next node
    public void setNext(LinkNode nextOne) {
        next = nextOne;
    }

    // get the next node
    public LinkNode getNext() {
        return next;
    }

    // get the data
    public int getData() {
        return data;
    }

} // end of class LinkNode

/**
 *  A class for iterating through the permutations of a multiset without 
 *  repetition.  The idea is that it requires a lot of memory to list all 
 *  permutations of a big multiset at the same time, so this class stores 
 *  the current permutation in memory and has the ability to 'step' to the 
 *  permutation that is 'next' in some ordering on all permutations of 
 *  this multiset.  The ordering is the cool-lex ordering, which, along 
 *  with this iteration algorithm, is described in the paper by Aaron 
 *  Williams: <a href="http://epubs.siam.org/doi/abs/10.1137/1.9781611973068.107">Loopless Generation of Multiset Permutations using a Constant Number of Variables by Prefix Shifts</a>.  
 */
public class MultiSetLinkedList implements Serializable {

    private LinkNode head;
    private LinkNode i;
    private LinkNode afteri;
    private int size;

    // private constructor
    private MultiSetLinkedList(int data) {
        head = LinkNode.createLinkNode(data);
        afteri = head;
        size = 1;
    }

    // private constructor
    private MultiSetLinkedList(List<Integer> d) {
        Collections.sort(d);
        int l = d.size();
        size = l;
        head = LinkNode.createLinkNode(d.get(l-1));
        LinkNode currentNode = head;
        for (int j = l - 1; j > 0; j--) {
            currentNode.setNext(LinkNode.createLinkNode(d.get(j-1)));
            if (j == 1) i = currentNode;
            currentNode = currentNode.getNext();
            if (j == 1) afteri = currentNode;
        }
    }

    /**
     *  Public static factory method.  
     *  Create a MultiSetLinkedList where the underlying multiset is 
     *  input as a List of Integers.  
     *  @param d The underlying multiset, giving as a List of Integers.  
     *  @return The MultiSetLinkedList that iterates through permutations 
     *  of d.  
     */
    public static MultiSetLinkedList createMultiSetLinkedList(List<Integer> d) {
        return new MultiSetLinkedList(d);
    }

    /**
     *  Public static factory method.  
     *  Create a MultiSetLinkedList where the underlying multiset is 
     *  input as an array of Integers.  
     *  @param d The underlying multiset, giving as an array of Integers.  
     *  @return The MultiSetLinkedList that iterates through permutations 
     *  of d.  
     */
    public static MultiSetLinkedList createMultiSetLinkedList(Integer[] d) {
        return new MultiSetLinkedList(Arrays.asList(d));
    }

    /**
     *  Public static factory method.  
     *  Create a MultiSetLinkedList where the underlying multiset is 
     *  input as an array of int.  
     *  @param d The underlying multiset, giving as an array of int.  
     *  @return The MultiSetLinkedList that iterates through permutations 
     *  of d.  
     */
    public static MultiSetLinkedList createMultiSetLinkedList(int[] d) {
        List<Integer> dd = new ArrayList<>();
        for (int i = 0; i < d.length; i++) dd.add(d[i]);
        return new MultiSetLinkedList(dd);
    }

    /**
     *  Make a deep copy of this.  
     *  @return Another MultiSetLinkedList with the same underlying 
     *  multiset as this and in the same state, but with the internal 
     *  fields pointing to different memory addresses.  
     */
    public MultiSetLinkedList deepCopy() {
        MultiSetLinkedList output = new MultiSetLinkedList(this.head.getData());
        output.size = this.size;
        output.i = output.head;
        output.afteri = output.head;
        LinkNode currentNode = head.getNext();
        LinkNode newCurrent = output.head;
        while (currentNode != null) {
            newCurrent.setNext(LinkNode.createLinkNode(currentNode.getData()));
            newCurrent = newCurrent.getNext();
            if (this.afteri.equals(currentNode)) output.afteri = newCurrent;
            if (this.i.equals(currentNode)) output.i = newCurrent;
            currentNode = currentNode.getNext();
        }
        return output;
    }

    /**
     *  Output a String representation of the current state of this.  
     *  @return A String containing the current permutation of the 
     *  underlying multiset, with entries separated by spaces and 
     *  enclosed by parentheses.  
     */
    public String toString() {
        String output = "( ";
        LinkNode currentNode = head;
        while (currentNode != null) {
            output += currentNode.getData() + " ";
            currentNode = currentNode.getNext();
        }
        output += ")";
        return output;
    }

    /**
     *  Change to the next permutation in cool-lex order.  
     */
    public void iterate() {
        if (size > 1) {
            LinkNode beforek;
            if (afteri.getNext() != null && i.getData() >= afteri.getNext().getData()) {
                beforek = afteri;
            } else {
                beforek = i;
            }
            LinkNode k = beforek.getNext();
            beforek.setNext(k.getNext());
            k.setNext(head);
            if (k.getData() < head.getData()) i = k;
            if (i.getNext() == null) {
                i = k;
                for (int j = 0; j < size - 2; j++) i = i.getNext();
            }
            afteri = i.getNext();
            head = k;
        }
    }

    /**
     *  Produce an array version of the current permutation.  
     *  @return An array representing the current permutation of the 
     *  underlying multiset.  
     */
    public int[] getArray() {
        int[] preList = new int[size];
        LinkNode currentNode = head;
        for (int j = 0; j < size; j++) {
            preList[j] = currentNode.getData();
            currentNode = currentNode.getNext();
        }
        return preList;
    }

    // equals method.
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        MultiSetLinkedList l = (MultiSetLinkedList) obj;
        if (l.size != this.size)
            return false;
        LinkNode n1 = this.head;
        LinkNode n2 = l.head;
        while (n1 != null) {
            if (n1.getData() != n2.getData()) return false;
            n1 = n1.getNext();
            n2 = n2.getNext();
        }
        return true;
    }

    // hashCode override.
    public int hashCode() {
        int prime = 59;
        int result = 19;
        LinkNode currentNode = head;
        while (currentNode != null)
            result = prime*result + currentNode.getData();
        return result;
    }

} // end of class MultiSetLinkedList
