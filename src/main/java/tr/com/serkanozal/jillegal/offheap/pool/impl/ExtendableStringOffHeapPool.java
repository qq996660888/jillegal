/**
 * @author SERKAN OZAL
 *         
 *         E-Mail: <a href="mailto:serkanozal86@hotmail.com">serkanozal86@hotmail.com</a>
 *         GitHub: <a>https://github.com/serkan-ozal</a>
 */

package tr.com.serkanozal.jillegal.offheap.pool.impl;

import java.util.ArrayList;
import java.util.List;

import tr.com.serkanozal.jillegal.offheap.domain.model.pool.ExtendableStringOffHeapPoolCreateParameter;
import tr.com.serkanozal.jillegal.offheap.memory.DirectMemoryService;
import tr.com.serkanozal.jillegal.offheap.memory.DirectMemoryServiceFactory;
import tr.com.serkanozal.jillegal.offheap.pool.ContentAwareOffHeapPool;
import tr.com.serkanozal.jillegal.offheap.pool.DeeplyForkableStringOffHeapPool;
import tr.com.serkanozal.jillegal.offheap.pool.ExplicitStringOffHeapPool;
import tr.com.serkanozal.jillegal.util.JvmUtil;

public class ExtendableStringOffHeapPool
		implements 
			ExplicitStringOffHeapPool<ExtendableStringOffHeapPoolCreateParameter>,
			ContentAwareOffHeapPool<String, ExtendableStringOffHeapPoolCreateParameter> {

	protected DeeplyForkableStringOffHeapPool rootForkableOffHeapPool;
	protected List<DeeplyForkableStringOffHeapPool> forkableOffHeapPoolList = 
					new ArrayList<DeeplyForkableStringOffHeapPool>();
	protected DeeplyForkableStringOffHeapPool currentForkableOffHeapPool;
	protected DeeplyForkableStringOffHeapPool lastUsedForkableOffHeapPoolToFree;
	protected DirectMemoryService directMemoryService = 
				DirectMemoryServiceFactory.getDirectMemoryService();
	protected volatile boolean available = false;
	
	public ExtendableStringOffHeapPool(ExtendableStringOffHeapPoolCreateParameter parameter) {
		this(parameter.getForkableStringOffHeapPool());
	}
	
	public ExtendableStringOffHeapPool(DeeplyForkableStringOffHeapPool forkableOffHeapPool) {
		init(forkableOffHeapPool);
	}
	
	protected void init() {
		currentForkableOffHeapPool = rootForkableOffHeapPool;
		lastUsedForkableOffHeapPoolToFree = null;
	}
	
	@Override
	public Class<String> getElementType() {
		return String.class;
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	protected void makeAvaiable() {
		available = true;
	}
	
	protected void makeUnavaiable() {
		available = false;
	}
	
	protected void checkAvailability() {
		if (!available) {
			throw new IllegalStateException(getClass() + " is not available !");
		}
	}
	
	@Override
	public synchronized String get(String str) {
		checkAvailability();
		String obj = currentForkableOffHeapPool.get(str);
		if (obj == null) {
			for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
				DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
				if (!forkableOffHeapPool.isFull()) {
					obj = forkableOffHeapPool.get(str);
					if (obj != null) {
						return obj;
					}
				}
			}
			extend();
			return currentForkableOffHeapPool.get(str);
		}
		else {
			return obj;
		}
	}
	
	@Override
	public synchronized long getAsAddress(String str) {
		checkAvailability();
		long address = currentForkableOffHeapPool.getAsAddress(str);
		if (address == JvmUtil.NULL) {
			for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
				DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
				if (!forkableOffHeapPool.isFull()) {
					address = forkableOffHeapPool.getAsAddress(str);
					if (address != JvmUtil.NULL) {
						return address;
					}
				}
			}
			extend();
			return currentForkableOffHeapPool.getAsAddress(str);
		}
		else {
			return address;
		}
	}
	
	@Override
	public boolean isMine(String element) {
		checkAvailability();
		if (element == null) {
			return false;
		}
		else {
			return isMine(directMemoryService.addressOf(element));
		}	
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean isMine(long address) {
		checkAvailability();
		if (currentForkableOffHeapPool instanceof ContentAwareOffHeapPool) {
			if (((ContentAwareOffHeapPool)currentForkableOffHeapPool).isMine(address)) {
				return true;
			}
		}
		for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
			DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
			if (forkableOffHeapPool != currentForkableOffHeapPool && forkableOffHeapPool instanceof ContentAwareOffHeapPool) {
				if (((ContentAwareOffHeapPool)forkableOffHeapPool).isMine(address)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public synchronized boolean free(String str) {
		checkAvailability();
		if (str == null) {
			return false;
		}
		if (currentForkableOffHeapPool.free(str)) {
			return true;
		}
		if (lastUsedForkableOffHeapPoolToFree != null) {
			if (lastUsedForkableOffHeapPoolToFree.free(str)) {
				return true;
			}
		}
		for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
			DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
			if (forkableOffHeapPool != currentForkableOffHeapPool) {
				if (forkableOffHeapPool.free(str)) {
					lastUsedForkableOffHeapPoolToFree = forkableOffHeapPool;
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public synchronized boolean freeFromAddress(long strAddress) {
		checkAvailability();
		if (currentForkableOffHeapPool.freeFromAddress(strAddress)) {
			return true;
		}
		if (lastUsedForkableOffHeapPoolToFree != null) {
			if (lastUsedForkableOffHeapPoolToFree.freeFromAddress(strAddress)) {
				return true;
			}
		}
		for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
			DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
			if (forkableOffHeapPool != currentForkableOffHeapPool) {
				if (forkableOffHeapPool.freeFromAddress(strAddress)) {
					lastUsedForkableOffHeapPoolToFree = forkableOffHeapPool;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public synchronized void reset() {
		for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
			DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
			if (forkableOffHeapPool != rootForkableOffHeapPool) {
				forkableOffHeapPool.reset();
			}	
		}
		rootForkableOffHeapPool.reset();
		init();
		makeAvaiable();
	}
	
	@Override
	public synchronized void free() {
		checkAvailability();
		for (int i = 0; i < forkableOffHeapPoolList.size(); i++) {
			DeeplyForkableStringOffHeapPool forkableOffHeapPool = forkableOffHeapPoolList.get(i);
			if (forkableOffHeapPool != rootForkableOffHeapPool) {
				forkableOffHeapPool.free();
			}	
		}
		currentForkableOffHeapPool.free();
		makeUnavaiable();
	}

	@Override
	public synchronized void init(ExtendableStringOffHeapPoolCreateParameter parameter) {
		init(parameter.getForkableStringOffHeapPool());
	}
	
	protected void init(DeeplyForkableStringOffHeapPool forkableOffHeapPool) {
		rootForkableOffHeapPool = forkableOffHeapPool;
		init();
		makeAvaiable();
	}
	
	protected void extend() {
		DeeplyForkableStringOffHeapPool newForkableOffHeapPool = 
				currentForkableOffHeapPool.fork();
		forkableOffHeapPoolList.add(currentForkableOffHeapPool);
		currentForkableOffHeapPool = 
				(DeeplyForkableStringOffHeapPool) newForkableOffHeapPool;
	}

}
