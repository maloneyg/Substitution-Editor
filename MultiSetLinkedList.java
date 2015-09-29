
/*************************************************************************
 *  Compilation:  javac MultiSetLinkedList.java
 *  Execution:    java MultiSetLinkedList
 *
 *
 *************************************************************************/

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

    // public static factory method
    public static MultiSetLinkedList createMultiSetLinkedList(List<Integer> d) {
        return new MultiSetLinkedList(d);
    }

    public static MultiSetLinkedList createMultiSetLinkedList(Integer[] d) {
        return new MultiSetLinkedList(Arrays.asList(d));
    }

    public static MultiSetLinkedList createMultiSetLinkedList(int[] d) {
        List<Integer> dd = new ArrayList<>();
        for (int i = 0; i < d.length; i++) dd.add(d[i]);
        return new MultiSetLinkedList(dd);
    }

    // make a deep copy
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

    // output a String
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

    // change into the next multiset in cool-lex order
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

    // produce an array of the data in this list
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


    // test client
    public static void main(String[] args) {

        ArrayList<Integer> test = new ArrayList<>(5);
        test.add(1);
        test.add(2);
        test.add(0);
        test.add(0);
        test.add(2);
        MultiSetLinkedList l = new MultiSetLinkedList(test);
        for (int i = 0; i < 32; i++) {
            System.out.println(l);
            l.iterate();
        }

    }

} // end of class MultiSetLinkedList
