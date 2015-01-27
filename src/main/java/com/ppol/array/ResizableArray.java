package com.ppol.array;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class ResizableArray<E> extends AbstractCollection<E> {

    private static final int FIRST_BUCKET_SIZE = 2;
    
    private final AtomicReferenceArray<AtomicReferenceArray<E>> memory = new AtomicReferenceArray<>(16);
    private AtomicReference<ResizableArray.Descriptor> currentDescriptor;

    public ResizableArray() {
        super();
        final WriteOperation operation = new WriteOperation(0, null, null);
        operation.setPendingFlag(false);
        this.currentDescriptor = new AtomicReference<>(new ResizableArray.Descriptor(0, operation));
        this.memory.set(0, new AtomicReferenceArray<E>(ResizableArray.FIRST_BUCKET_SIZE));
    }

    @Override
    public boolean add(final E obj) {
        ResizableArray.Descriptor current;
        ResizableArray.Descriptor newD;
        do {
            current = currentDescriptor.get();
            if (current.getWo().isPending()) {
                this.write(current.getWo());
            }
            this.allocateBucketIfNeeded(current.getSize());
            WriteOperation wo = new WriteOperation(current.getSize(), null, obj);
            wo.setPendingFlag(true);
            newD = new ResizableArray.Descriptor(current.getSize() + 1, wo);
        } while (!this.currentDescriptor.compareAndSet(current, newD));
        this.write(currentDescriptor.get().getWo());
        return true;
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
        final int hibit = Integer.highestOneBit(size + ResizableArray.FIRST_BUCKET_SIZE);
        if (hibit == 0) {
            return 0;
        }
        return 31 - Integer.numberOfLeadingZeros(hibit) - 1 ;
    }

    private int indexInsideBucket(final int globalIndex, final int bucketId) {
        final int pos = globalIndex + ResizableArray.FIRST_BUCKET_SIZE;
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
        private int size = ResizableArray.this.size();

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        @Override
        public E next() {
            final int bucketId = ResizableArray.this.bucketId(this.index);
            return (E) ResizableArray.this.memory.get(bucketId(index))
                .get(indexInsideBucket(index++, bucketId));
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("");
        }
    }

    private static class Descriptor {
        private final ResizableArray.WriteOperation wo;
        private final int size;

        private Descriptor(final int size, final ResizableArray.WriteOperation wo) {
            this.wo = wo;
            this.size = size;
        }
        public ResizableArray.WriteOperation getWo() {
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
