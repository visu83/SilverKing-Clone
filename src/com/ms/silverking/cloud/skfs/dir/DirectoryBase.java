package com.ms.silverking.cloud.skfs.dir;

import com.ms.silverking.cloud.dht.daemon.PeerHealthMonitor;
import com.ms.silverking.numeric.NumConversion;

public abstract class DirectoryBase implements Directory {
    
    protected static final int    magicLength = NumConversion.BYTES_PER_INT;
    protected static final int    lengthLength = NumConversion.BYTES_PER_INT;
    protected static final int    indexOffsetLength = NumConversion.BYTES_PER_INT;
    protected static final int    numEntriesLength = NumConversion.BYTES_PER_INT;
    
    protected static final int    magicOffset = 0;
    protected static final int    lengthOffset = magicOffset + magicLength;
    protected static final int    indexOffsetOffset = lengthOffset + lengthLength;
    protected static final int    numEntriesOffset = indexOffsetOffset  + indexOffsetLength;
    protected static final int    dataOffset = numEntriesOffset + numEntriesLength;
    
    protected static final int    indexNumEntriesOffset = NumConversion.BYTES_PER_INT;
    protected static final int    indexFirstEntryOffset = indexNumEntriesOffset + NumConversion.BYTES_PER_INT;
    
    protected static final int    OD_MAGIC = 0x00abacad;
    protected static final int    DEI_MAGIC = 0xaaddaabb;
    protected static final int    DEI_ENTRY_SIZE = NumConversion.BYTES_PER_INT;
    protected static final int    headerSize = dataOffset;
    
    protected static PeerHealthMonitor    peerHealthMonitor;
    
    public static void setPeerHealthMonitor(PeerHealthMonitor _peerHealthMonitor) {
        peerHealthMonitor = _peerHealthMonitor; 
    }
    
    protected int writeHeader(byte[] buf, int offset, int length, int indexOffset, int numEntries) {
        NumConversion.intToBytesLittleEndian(OD_MAGIC, buf, offset + magicOffset);
        NumConversion.intToBytesLittleEndian(length, buf, offset + lengthOffset);
        NumConversion.intToBytesLittleEndian(indexOffset, buf, offset + indexOffsetOffset);
        NumConversion.intToBytesLittleEndian(numEntries, buf, offset + numEntriesOffset);
        return headerSize;
    }
    
    protected int computeIndexSizeBytes(int numEntries) {
        return indexFirstEntryOffset + numEntries * DEI_ENTRY_SIZE;
    }
    
    protected int writeIndexHeader(byte[] buf, int offset, int numEntries) {
        NumConversion.intToBytesLittleEndian(DEI_MAGIC, buf, offset);
        NumConversion.intToBytesLittleEndian(numEntries, buf, offset + NumConversion.BYTES_PER_INT);
        return NumConversion.BYTES_PER_INT * 2;
    }
    
    protected int writeIndex(byte[] buf, int offset, int[] indexOffsets) {
        int    bytesWritten;
        
        bytesWritten = writeIndexHeader(buf, offset + dataOffset, indexOffsets.length);
        for (int i = 0; i < indexOffsets.length; i++) {
            NumConversion.intToBytesLittleEndian(indexOffsets[i], buf, dataOffset + bytesWritten + offset + i * DEI_ENTRY_SIZE);
        }
        bytesWritten += indexOffsets.length * DEI_ENTRY_SIZE;
        return bytesWritten;
    }
    
    public void display() {
        int    numEntries;
        
        numEntries = getNumEntries();
        System.out.printf("numEntries %s\n", numEntries);
        for (int i = 0; i < numEntries; i++) {
            DirectoryEntry    entry;
            
            entry = getEntry(i);
            System.out.printf("%d\t%s\n", i, entry.toString());
        }
    }
}
