package com.jagex.game.runetek5.cache.node;

import java.util.Iterator;

/**
 * @author Major
 * @author Brainpower - 29/05/2020 
 * 
 * Ported from revision 839
 *
 * @param <V> value type
 */
public final class IterableNodeMap<V extends Node> implements Iterable<V> {

	public Node<?>[] nodes;
	public int currentPosition = 0;
	public Node<?> iterationNode;
	public long retrievalKey;
	public Node<?> retrievalNode;
	public int size;

	public IterableNodeMap(int capacity) {
		this.size = capacity;
		this.nodes = new Node[capacity];

		for (int index = 0; index < capacity; index++) {
			Node linkable = this.nodes[index] = new Node();
			linkable.next = linkable;
			linkable.previous = linkable;
		}
	}

	public void clear() {
		for (int index = 0; index < this.size; index++) {
			Node bucket = this.nodes[index];
			for (;;) {
				Node next = bucket.next;
				if (bucket == next) {
					break;
				}
				next.unlink();
			}
		}
		this.retrievalNode = null;
		this.iterationNode = null;
	}

	public V first() {
		this.currentPosition = 0;
		return next();
	}

	@SuppressWarnings("unchecked")
	public V get(long key) {
		this.retrievalKey = key;
		Node head = this.nodes[(int) (key & this.size - 1)];
		for (this.retrievalNode = head.next; this.retrievalNode != head; this.retrievalNode = this.retrievalNode.next) {
			if (key == this.retrievalNode.pointer) {
				Node value = this.retrievalNode;
				this.retrievalNode = this.retrievalNode.next;
				return (V) value;
			}
		}
		this.retrievalNode = null;
		return null;
	}

	@Override
	public Iterator<V> iterator() {
		return new HashTableIterator<V>(this);
	}

	@SuppressWarnings("unchecked")
	public V next() {
		if (this.currentPosition > 0 && this.nodes[this.currentPosition - 1] != this.iterationNode) {
			Node value = this.iterationNode;
			this.iterationNode = value.next;
			return (V) value;
		}
		while (this.currentPosition < this.size) {
			Node value = this.nodes[++this.currentPosition - 1].next;
			if (value != this.nodes[this.currentPosition - 1]) {
				this.iterationNode = value.next;
				return (V) value;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public V nextInBucket() {
		if (this.retrievalNode == null) {
			return null;
		}
		for (Node linkable = this.nodes[(int) (this.retrievalKey & this.size - 1)]; linkable != this.retrievalNode; this.retrievalNode = this.retrievalNode.next) {
			if (this.retrievalNode.pointer == this.retrievalKey) {
				Node value = this.retrievalNode;

				this.retrievalNode = this.retrievalNode.next;
				return (V) value;
			}
		}
		this.retrievalNode = null;
		return null;
	}

	public void put(V value, long key) {
		if (null != value.previous) {
			value.unlink();
		}
		Node linkable = this.nodes[(int) (key & this.size - 1)];
		value.previous = linkable.previous;
		value.next = linkable;
		value.previous.next = value;
		value.next.previous = value;
		value.pointer = key;
	}

	public int size() {
		int count = 0;
		for (int index = 0; index < this.size; index++) {
			Node linkable = this.nodes[index];
			for (Node next = linkable.next; linkable != next; next = next.next) {
				count++;
			}
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	public int values(V[] values) {
		int count = 0;
		for (int index = 0; index < this.size; index++) {
			Node head = this.nodes[index];
			for (Node next = head.next; head != next; next = next.next) {
				values[count++] = (V) next;
			}
		}
		return count;
	}
}
