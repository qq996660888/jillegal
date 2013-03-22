/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jillegal.offheap.pool;

import tr.com.serkanozal.jillegal.offheap.domain.model.pool.SequentialObjectPoolCreateParameter;
import tr.com.serkanozal.jillegal.offheap.memory.OffHeapMemoryService;

public class SequentialObjectPool<T> extends BaseOffHeapPool<T, SequentialObjectPoolCreateParameter<T>> {

	private long objectCount;
	private long objectSize;
	private long currentMemoryIndex;
	private long allocatedAddress;
	private T sampleObject;
	private long sampleObjectAddress;
	private long addressLimit;
	
	public SequentialObjectPool(SequentialObjectPoolCreateParameter<T> parameter) {
		this(parameter.getElementType(), parameter.getObjectCount(), parameter.getOffHeapMemoryService());
	}
	
	public SequentialObjectPool(Class<T> clazz, long objectCount, OffHeapMemoryService offHeapMemoryService) {
		super(clazz, offHeapMemoryService);
		init(clazz, objectCount, offHeapMemoryService);
	}
	
	protected synchronized void init() {
		this.currentMemoryIndex	= allocatedAddress - objectSize;
		for (long l = 0; l < objectCount; l++) {
			offHeapMemoryService.copyMemory(sampleObjectAddress, allocatedAddress + (l * objectSize), objectSize);
		}
	}
	
	public long getObjectCount() {
		return objectCount;
	}
	
	@Override
	public synchronized T newObject() {
		if (currentMemoryIndex >= addressLimit) {
			return null;
		}
		return offHeapMemoryService.getObject((currentMemoryIndex += objectSize));
	}
	
	@Override
	public synchronized T getObject(long objectIndex) {
		if (objectIndex < 0 || objectIndex > objectCount) {
			return null;
		}	
		return offHeapMemoryService.getObject((allocatedAddress + (objectIndex * objectSize)));
	}
	
	@Override
	public void reset() {
		init();
	}
	
	@Override
	public void free() {
		offHeapMemoryService.freeMemory(allocatedAddress);
	}

	@Override
	public void init(SequentialObjectPoolCreateParameter<T> parameter) {
		init(parameter.getElementType(), parameter.getObjectCount(), parameter.getOffHeapMemoryService());
	}
	
	@SuppressWarnings("unchecked")
	protected void init(Class<T> clazz, long objectCount, OffHeapMemoryService offHeapMemoryService) {
		this.elementType = clazz;
		this.objectCount = objectCount;
		this.offHeapMemoryService = offHeapMemoryService;
		this.objectSize = offHeapMemoryService.sizeOf(clazz);
		this.allocatedAddress = offHeapMemoryService.allocateMemory(objectSize * objectCount);
		this.addressLimit = allocatedAddress + (objectCount * objectSize) - objectSize;
		this.sampleObject = (T) offHeapMemoryService.allocateInstance(clazz);
		this.sampleObjectAddress = offHeapMemoryService.addressOf(sampleObject);
		
		init();
	}
	
}