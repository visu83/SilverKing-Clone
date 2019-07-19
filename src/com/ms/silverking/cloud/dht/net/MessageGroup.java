package com.ms.silverking.cloud.dht.net;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import com.ms.silverking.cloud.dht.ValueCreator;
import com.ms.silverking.cloud.dht.client.ChecksumType;
import com.ms.silverking.cloud.dht.common.MessageType;
import com.ms.silverking.cloud.dht.common.SimpleValueCreator;
import com.ms.silverking.id.UUIDBase;
import com.ms.silverking.io.util.BufferUtil;
import com.ms.silverking.log.Log;
import com.ms.silverking.net.async.OutgoingData;
import com.ms.silverking.numeric.NumConversion;
import com.ms.silverking.text.StringUtil;
import com.ms.silverking.time.AbsMillisTimeSource;

/**
 * Message which groups single type of sub-messages.
 * These sub-messages are stored in serialized ByteBuffer
 * form.
 * 
 * MessageGroups are created using specific subclasses of ProtoMessageGroup.
 * The components of a MessageGroup are available via iterators
 * that create instances of MessageGroupKeyEntry and descendants.
 */
public final class MessageGroup {
    private final MessageType     messageType;
    private int                   options; // internal options used in processing
    private final long            context;
    private final ByteBuffer[]    buffers;
    private final int             bytesPerKeyEntry;
    private final byte[]          originator;
    private final UUIDBase        uuid;
    private final int             deadlineRelativeMillis;
    private final ForwardingMode  forward;
        
    // preamble buffer not visible at this layer
    private static final int    keyBufferIndex = 0;
    private static final int    keyBufferMetaDataLength = NumConversion.BYTES_PER_SHORT;
    
    private static final int    putResponseKeyBufferIndex = 1;    
    
    private static final boolean    debug = false;
    private static final boolean    debugShortTimeout = false;
    private static final int        shortTimeoutLimit = 1000;
    
    public static final int    minDeadlineRelativeMillis = OutgoingData.minRelativeDeadline;
    
    private static final int    MG_O_peer = 1;
    
    public MessageGroup(MessageType messageType, int options, UUIDBase uuid, long context, ByteBuffer[] buffers, 
                        byte[] originator, int deadlineRelativeMillis, ForwardingMode forward) {
        this.messageType = messageType;
        this.options = options;
        this.uuid = uuid;
        this.context = context;
        this.buffers = buffers;
        assert originator.length == ValueCreator.BYTES;
        this.originator = originator;
        if (deadlineRelativeMillis < minDeadlineRelativeMillis) {
            Log.warningf("deadlineRelativeMillis < minDeadlineRelativeMillis; %d < %d", deadlineRelativeMillis, minDeadlineRelativeMillis);
            Thread.dumpStack();
            deadlineRelativeMillis = minDeadlineRelativeMillis;
        }
        this.deadlineRelativeMillis = deadlineRelativeMillis;
        this.forward = forward;
        //for (ByteBuffer buffer : buffers) {
            //System.out.println("\t\t"+ buffer);
            //buffer.flip();
            //System.out.println("\t\t\t"+ buffer);
        //}
        switch (messageType) {
        case PUT:
        case RETRIEVE:
        case RETRIEVE_RESPONSE:
        case PUT_UPDATE:
            //System.out.printf("buffers[keyBufferIndex] %s\n", StringUtil.byteBufferToHexString(buffers[keyBufferIndex])); System.out.flush();
            bytesPerKeyEntry = buffers[keyBufferIndex].getShort(0);
            //System.out.printf("messageType %s bytesPerKeyEntry %d\n", messageType, bytesPerKeyEntry); System.out.flush();
            assert bytesPerKeyEntry > 0;
            break;
        case PUT_RESPONSE:
            bytesPerKeyEntry = buffers[keyBufferIndex].getShort(0);
            //assert bytesPerKeyEntry == 0;
            break;
        case SNAPSHOT:
        case SYNC_REQUEST:
        case CHECKSUM_TREE:
        case CHECKSUM_TREE_REQUEST:
        case OP_RESPONSE:
        case NAMESPACE_REQUEST:
        case NAMESPACE_RESPONSE:
        case OP_NOP:
        case OP_PING:
        case OP_PING_ACK:
        case SET_CONVERGENCE_STATE:            
            bytesPerKeyEntry = 0;
            break;
        default:
            throw new RuntimeException("Unsupported message type: "+ messageType);
        }
        if (debugShortTimeout) {
            if (deadlineRelativeMillis < shortTimeoutLimit) {
                Log.warning("short deadlineRelativeMillis: "+ deadlineRelativeMillis);
                Log.warning(toString());
                Thread.dumpStack();
            }
        }
    }
    
    public MessageGroup(MessageType messageType, int options, UUIDBase uuid, long context, List<ByteBuffer> buffers, 
                        byte[] originator, int deadlineRelativeMillis, ForwardingMode forward) {
        this(messageType, options, uuid, context, buffers.toArray(new ByteBuffer[0]), originator, 
             deadlineRelativeMillis, forward);
        /*
         // For debugging only
        if (this.buffers.length >= 3 && this.buffers[2].isDirect() && this.buffers[2].remaining() == 0) {
            displayForDebug(true);
            Thread.dumpStack();
            throw new RuntimeException("panic");
        }
        */
    }
    
    public void setPeer(boolean peer) {
        if (peer) {
            options = options | MG_O_peer;
        } else {
            options = options & (~MG_O_peer);
        }
    }
    
    public boolean getPeer() {
        return (options & MG_O_peer) != 0;
    }
    
    public MessageGroup duplicate() {
        ByteBuffer[]    _buffers;
        
        _buffers = new ByteBuffer[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            _buffers[i] = buffers[i].duplicate();
        }
        return new MessageGroup(messageType, options, uuid, context, _buffers, originator, 
                                deadlineRelativeMillis, forward);
    }
    
    public MessageGroup ensureArrayBacked() {
        boolean    arrayBacked;
        
        arrayBacked = true;
        for (int i = 0; i < buffers.length; i++) {
            if (!buffers[i].hasArray()) {
                arrayBacked = false;
                break;
            }
        }
        if (arrayBacked) {
            return this;
        } else {
            ByteBuffer[]    _buffers;
            
            _buffers = new ByteBuffer[buffers.length];
            for (int i = 0; i < buffers.length; i++) {
                _buffers[i] = BufferUtil.ensureArrayBacked(buffers[i]);
            }
            return new MessageGroup(messageType, options, uuid, context, _buffers, originator, 
                                    deadlineRelativeMillis, forward);
        }
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public int getOptions() {
        return options;
    }
    
    public long getContext() {
        return context;
    }
    
    public byte[] getOriginator() {
        return originator;
    }
    
    public int getDeadlineRelativeMillis() {
        return deadlineRelativeMillis;
    }
    
    public long getDeadlineAbsMillis(AbsMillisTimeSource absMillisTimeSource) {
        return absMillisTimeSource.absTimeMillis() + deadlineRelativeMillis;
    }
    
    public ForwardingMode getForwardingMode() {
        return forward;
    }
    
    public UUIDBase getUUID() {
        return uuid;
    }
    
    public ByteBuffer[] getBuffers() {
        return buffers;
    }
    
    public long getTotalBytes() {
        long    totalBytes;
        
        totalBytes = 0;
        for (ByteBuffer buffer : buffers) {
            totalBytes += buffer.remaining();
        }
        return totalBytes;
    }
    
    /**
     * A crude estimate on the number of keys. Used for sizing maps that need to know this. 
     * @return
     */
    public int estimatedKeys() {
        /*
        System.out.printf("buffers[keyBufferIndex].limit() - keyBufferMetaDataLength) %d\tbytesPerKeyEntry %d\t%s\n", 
                buffers[keyBufferIndex].limit() - keyBufferMetaDataLength, bytesPerKeyEntry,
                messageType);
                */
        return (buffers[keyBufferIndex].limit() - keyBufferMetaDataLength) / bytesPerKeyEntry;
    }
    
    public void displayForDebug() {
        displayForDebug(false);
    }
       
    public void displayForDebug(boolean displayContent) {
        System.out.println(messageType);
        System.out.println("buffers.size()\t"+ buffers.length);
        for (ByteBuffer buffer : buffers) {
            System.out.println("\t"+ buffer);
            if (displayContent) {
                System.out.println(StringUtil.byteBufferToHexString(buffer));
            }
            //Log.warning("buffer.remaining()\t", buffer.remaining());
            //Log.fine(StringUtil.byteArrayToHexString(buffer.array()));
        }
    }
    
    ////////////////
    
    public Iterable<MessageGroupKeyEntry> getKeyIterator() {
        return new KeyIterator();
    }

    class KeyIterator implements Iterator<MessageGroupKeyEntry>, Iterable<MessageGroupKeyEntry> {
        private final ByteBuffer  keyBuffer;
        private int         curKey;
        
        KeyIterator() {
            keyBuffer = buffers[keyBufferIndex].duplicate();
        }
        
        @Override
        public Iterator<MessageGroupKeyEntry> iterator() {
            return this;
        }
        
        @Override
        public boolean hasNext() {
            if (debug) {
                Log.warning("a: curKey "+ curKey +"\tkeyBuffer.limit() "+ keyBuffer.limit());
            }
            return keyBufferMetaDataLength + curKey * MessageGroupKeyEntry.bytesPerEntry < keyBuffer.limit();
        }

        @Override
        public MessageGroupKeyEntry next() {
            if (debug) {
                Log.warning("b: curKey "+ curKey +"\tkeyBuffer.limit() "+ keyBuffer.limit());
            }
            if (hasNext()) {
                return new MessageGroupKeyEntry(keyBuffer, keyBufferMetaDataLength + curKey++ * MessageGroupKeyEntry.bytesPerEntry);
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }        
    }

    ////////////////
    
    public ByteBuffer wrapEntry(MessageGroupKVEntry entry) {
        return ByteBuffer.wrap(buffers[entry.getBufferIndex()].array(),
                        entry.getBufferOffset(),
                        entry.getStoredLength());
    }
    
    public Iterable<MessageGroupRetrievalResponseEntry> getRetrievalResponseValueKeyIterator() {
        return new RetrievalResponseKeyValueIterator();
    }
    
    public Iterable<MessageGroupPutEntry> getPutValueKeyIterator(ChecksumType checksumType) {
        return new PutKeyValueIterator(checksumType);
    }
    
    class PutKeyValueIterator extends KeyValueIterator<MessageGroupPutEntry> {
        private final ChecksumType  checksumType;
        
        PutKeyValueIterator(ChecksumType checksumType) {
            super();
            this.checksumType = checksumType;
        }
        
        @Override
        MessageGroupPutEntry createEntry(int curOffset, ByteBuffer[] buffers) {
            return new MessageGroupPutEntry(keyBuffer, curOffset, buffers, checksumType);
        }
    }
    
    class RetrievalResponseKeyValueIterator extends KeyValueIterator<MessageGroupRetrievalResponseEntry> {
        RetrievalResponseKeyValueIterator() {
            super();
        }
        
        @Override
        MessageGroupRetrievalResponseEntry createEntry(int curOffset, ByteBuffer[] buffers) {
            return new MessageGroupRetrievalResponseEntry(keyBuffer, curOffset, buffers);
        }
    }
    
    abstract class KeyValueIterator<T extends MessageGroupKVEntry> implements Iterator<T>, Iterable<T> {
        protected final ByteBuffer  keyBuffer;
        private int curOffset;
        
        KeyValueIterator() {
            keyBuffer = buffers[keyBufferIndex].duplicate();
            curOffset = keyBufferMetaDataLength;
        }
        
        @Override
        public Iterator<T> iterator() {
            return this;
        }
        
        @Override
        public boolean hasNext() {
            return curOffset < keyBuffer.limit();
        }
        
        abstract T createEntry(int offset, ByteBuffer[] buffers);
        
        private int oldOffset;

        @Override
        public T next() {
            try {
            if (hasNext()) {
                T next;
                
                //System.out.println(keyBufferIndex +" "+ curOffset);
                //next = new MessageGroupKVEntry(keyBuffer, curOffset, buffers);
                next = createEntry(curOffset, buffers);
                oldOffset = curOffset;
                curOffset += next.entryLength();
                return next;
            } else {
                return null;
            }
            } catch (RuntimeException re) {
                System.out.printf("%d %d %s\n", curOffset, oldOffset, keyBuffer);
                re.printStackTrace();
                System.exit(-1);
                throw re;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }        
    }
    
    public void copyEntry(MessageGroupKVEntry entry, byte[] dest, int destOffset) {
        //System.out.println(entry);
        //System.out.println(buffers[entry.getBufferIndex()].array().length);
        //System.out.println(entry.getBufferOffset());
        System.arraycopy(buffers[entry.getBufferIndex()].array(), entry.getBufferOffset(), 
                         dest, destOffset, entry.getStoredLength());
    }
    
    ///////////////    

    public Iterable<MessageGroupKeyOrdinalEntry> getKeyOrdinalIterator() {
        return new KeyOrdinalIterator();
    }
    
    class KeyOrdinalIterator implements Iterator<MessageGroupKeyOrdinalEntry>, Iterable<MessageGroupKeyOrdinalEntry> {
        private final ByteBuffer    putResponseKeyBuffer; 
        private int curKey;
        
        KeyOrdinalIterator() {
            putResponseKeyBuffer = buffers[putResponseKeyBufferIndex].duplicate();
        }
        
        @Override
        public Iterator<MessageGroupKeyOrdinalEntry> iterator() {
            return this;
        }
        
        @Override
        public boolean hasNext() {
            //Log.warning("a: curKey "+ curKey +"\tputResponseKeyBuffer.limit() "+ putResponseKeyBuffer.limit());
            return keyBufferMetaDataLength + curKey * MessageGroupKeyOrdinalEntry.bytesPerEntry < putResponseKeyBuffer.limit();
        }

        @Override
        public MessageGroupKeyOrdinalEntry next() {
            //Log.warning("b: curKey "+ curKey +"\tputResponseKeyBuffer.limit() "+ putResponseKeyBuffer.limit());
            if (hasNext()) {
                return new MessageGroupKeyOrdinalEntry(putResponseKeyBuffer, keyBufferMetaDataLength + curKey++ * MessageGroupKeyOrdinalEntry.bytesPerEntry);
            } else {
                return null;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }        
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%x:%s:%s", messageType, uuid, context, 
                new SimpleValueCreator(originator), getBufferLengthsString());
    }
    
    private String getBufferLengthsString() {
        StringBuilder    sb;
        
        sb = new StringBuilder();
        if (buffers != null) {
            for (ByteBuffer buf : buffers) {
                if (buf != null) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(buf.limit());
                }
            }
        }
        return sb.toString();
    }
    
    public static MessageGroup clone(MessageGroup mg) {
        return new MessageGroup(mg.messageType, mg.options, mg.uuid, mg.context, cloneBuffers(mg), 
                mg.originator, mg.deadlineRelativeMillis, mg.forward);
    }

    private static ByteBuffer[] cloneBuffers(MessageGroup mg) {
        ByteBuffer[]    _buffers;
        
        _buffers = new ByteBuffer[mg.buffers.length];
        for (int i = 0; i < _buffers.length; i++) {
            if (mg.buffers[i].hasArray()) {
                _buffers[i] = mg.buffers[i].duplicate();
            } else {
                byte[]  a;
                
                a = new byte[mg.buffers[i].remaining()];
                mg.buffers[i].duplicate().get(a);
                _buffers[i] = ByteBuffer.wrap(a);
            }
        }
        return _buffers;
    }
}
