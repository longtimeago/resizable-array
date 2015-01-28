package com.ppol.array;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class LockFreeList<E> extends AbstractList<E> {

    private static final int FIRST_BUCKET_SIZE = 2;

    private final AtomicReferenceArray<AtomicReferenceArray<E>> memory = new AtomicReferenceArray<>(16);
    private AtomicReference<LockFreeList.Descriptor> currentDescriptor;

    public LockFreeList() {
        super();
        final WriteOperation operation = new WriteOperation(0, null, null);
        operation.setPendingFlag(false);
        this.currentDescriptor = new AtomicReference<>(new LockFreeList.Descriptor(0, operation));
        this.memory.set(0, new AtomicReferenceArray<E>(LockFreeList.FIRST_BUCKET_SIZE));
    }

    @Override
    public boolean add(final E obj) {
        LockFreeList.Descriptor current;
        LockFreeList.Descriptor newD;
        do {
            current = currentDescriptor.get();
            if (current.getWo().isPending()) {
                this.write(current.getWo());
            }
            this.allocateBucketIfNeeded(current.getSize());
            WriteOperation wo = new WriteOperation(current.getSize(), null, obj);
            wo.setPendingFlag(true);
            newD = new LockFreeList.Descriptor(current.getSize() + 1, wo);
        } while (!this.currentDescriptor.compareAndSet(current, newD));
        this.write(currentDescriptor.get().getWo());
        return true;
    }

    @Override
    public E get(final int index) {
        final int bucketId = bucketId(index);
        final int indexInsideBucket = indexInsideBucket(index, bucketId);
        final AtomicReferenceArray<E> bucket = this.memory.get(bucketId);
        if (bucket == null || indexInsideBucket >= bucket.length()) {
            throw new IndexOutOfBoundsException();
        }
        return bucket.get(indexInsideBucket);
    }

    @Override
    public E set(final int index, final E element) {
        final int bucketId = bucketId(index);
        final int indexInsideBucket = indexInsideBucket(index, bucketId);
        final AtomicReferenceArray<E> bucket = this.memory.get(bucketId);
        if (bucket == null || indexInsideBucket >= bucket.length()) {
            throw new IndexOutOfBoundsException();
        }
        return this.memory.get(bucketId)
            .getAndSet(indexInsideBucket, element);
    }

    private void write(final WriteOperation wo) {
        if (wo.isPending()) {
            final int bucketId = bucketId(wo.getIndexToInsert());
            final int idx = indexInsideBucket(wo.getIndexToInsert(), bucketId);

            final AtomicReferenceArray<E> bucket = this.memory.get(bucketId);
            if (bucket.compareAndSet(idx, (E)wo.getOldValue(), (E)wo.getNewValue())) {
                wo.setPendingFlag(false);
            }
        }
    }

    private void allocateBucketIfNeeded(final int size) {
        final int bucket = bucketId(size);
        if (this.memory.get(bucket) == null) {
            final int bucketSize = Integer.highestOneBit(size << 1);
            final AtomicReferenceArray<E> mem = new AtomicReferenceArray<>(bucketSize);
            this.memory.compareAndSet(bucket, null, mem);
        }
    }

    private int bucketId(final int size) {
        final int hibit = Integer.highestOneBit(size + LockFreeList.FIRST_BUCKET_SIZE);
        if (hibit == 0) {
            return 0;
        }
        return 31 - Integer.numberOfLeadingZeros(hibit) - 1 ;
    }

    private int indexInsideBucket(final int globalIndex, final int bucketId) {
        final int pos = globalIndex + LockFreeList.FIRST_BUCKET_SIZE;
        return pos ^ 1 << bucketId + 1;
    }

    @Override
    public Iterator<E> iterator() {
        return new ArrayIterator<>();
    }

    @Override
    public int size() {
        int size = currentDescriptor.get().getSize();
        if (currentDescriptor.get().getWo().isPending()) {
            size--;
        }
        return size;
    }

    private class ArrayIterator<E> implements Iterator<E> {
        private int index;
        private int size = LockFreeList.this.size();

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public E next() {
            final int bucketId = LockFreeList.this.bucketId(this.index);
            return (E) LockFreeList.this.memory.get(bucketId(index))
                .get(indexInsideBucket(index++, bucketId));
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("");
        }
    }

    private static class Descriptor {
        private final LockFreeList.WriteOperation wo;
        private final int size;

        private Descriptor(final int size, final LockFreeList.WriteOperation wo) {
            this.wo = wo;
            this.size = size;
        }
        public LockFreeList.WriteOperation getWo() {
            return this.wo;
        }
        public int getSize() {
            return this.size;
        }

        @Override
        public String toString() {
            return "Descriptor{" +
                "wo=" + wo +
                ", size=" + size +
                '}';
        }
    }

    private static class WriteOperation<E> {
        private final E oldValue;
        private final E newValue;
        private final int indexToInsert;
        private volatile boolean pendingFlag = false;

        WriteOperation(final int indexToInsert, final E oldValue, final E newValue) {
            this.indexToInsert = indexToInsert;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        public E getOldValue() {
            return this.oldValue;
        }

        public E getNewValue() {
            return this.newValue;
        }

        public int getIndexToInsert() {
            return this.indexToInsert;
        }

        public boolean isPending() {
            return this.pendingFlag;
        }

        public void setPendingFlag(final boolean pendingFlag) {
            this.pendingFlag = pendingFlag;
        }

        @Override
        public String toString() {
            return "WriteOperation{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", indexToInsert=" + indexToInsert +
                ", pendingFlag=" + pendingFlag +
                '}';
        }
    }
}
